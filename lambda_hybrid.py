import json
import boto3
import uuid
from datetime import datetime, timezone, timedelta
import logging
from decimal import Decimal

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# AWS Configuration
ORIGINATOR_NUMBER_US = "+12173933490"  # 10DLC for US only
CONFIGURATION_SET = "AllSensesAI-SafetyAlerts"
MESSAGE_TYPE = "TRANSACTIONAL"
REGION = "us-east-1"
SNS_TOPIC_ARN = "arn:aws:sns:us-east-1:794289527784:AllSenses-Emergency-Alerts"

# DynamoDB Tables
LOCATION_TABLE = "AllSenses-LocationTracking"
INCIDENTS_TABLE = "AllSenses-Incidents"

# Lazy-loaded clients
_eum_client = None
_sns_client = None
_dynamodb = None

def get_eum_client():
    global _eum_client
    if _eum_client is None:
        _eum_client = boto3.client('pinpoint-sms-voice-v2', region_name=REGION)
    return _eum_client

def get_sns_client():
    global _sns_client
    if _sns_client is None:
        _sns_client = boto3.client('sns', region_name=REGION)
    return _sns_client

def get_dynamodb():
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.resource('dynamodb', region_name=REGION)
    return _dynamodb

def is_us_number(phone):
    """Check if phone number is US (+1)"""
    return phone.startswith('+1')

def send_sms_hybrid(phone_number, message):
    """
    Hybrid SMS sending:
    - US numbers (+1): Use EUM with 10DLC
    - International: Use SNS
    """
    try:
        if is_us_number(phone_number):
            # Use EUM for US numbers
            logger.info(f"Sending to US number via EUM: {phone_number}")
            eum_client = get_eum_client()
            
            response = eum_client.send_text_message(
                DestinationPhoneNumber=phone_number,
                OriginationIdentity=ORIGINATOR_NUMBER_US,
                MessageBody=message,
                MessageType=MESSAGE_TYPE,
                ConfigurationSetName=CONFIGURATION_SET
            )
            
            message_id = response.get('MessageId')
            logger.info(f"EUM SMS sent: {message_id}")
            
            return {
                'status': 'sent',
                'messageId': message_id,
                'method': 'EUM',
                'originator': ORIGINATOR_NUMBER_US
            }
        else:
            # Use SNS for international numbers
            logger.info(f"Sending to international number via SNS: {phone_number}")
            sns_client = get_sns_client()
            
            response = sns_client.publish(
                PhoneNumber=phone_number,
                Message=message,
                MessageAttributes={
                    'AWS.SNS.SMS.SMSType': {
                        'DataType': 'String',
                        'StringValue': 'Transactional'
                    }
                }
            )
            
            message_id = response.get('MessageId')
            logger.info(f"SNS SMS sent: {message_id}")
            
            return {
                'status': 'sent',
                'messageId': message_id,
                'method': 'SNS',
                'originator': 'AllSenses'
            }
            
    except Exception as e:
        logger.error(f"SMS sending failed: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e),
            'method': 'EUM' if is_us_number(phone_number) else 'SNS'
        }

def handler(event, context):
    logger.info("="*80)
    logger.info("LAMBDA INVOKED")
    logger.info(f"Event: {json.dumps(event, default=str)}")
    logger.info("="*80)
    
    try:
        if event.get('httpMethod') == 'OPTIONS':
            logger.info("CORS preflight request")
            return cors_response({})
        
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        action = body.get('action', 'TEST_SMS')
        logger.info(f"Action: {action}")
        logger.info(f"Body: {json.dumps(body, default=str)}")
        
        if action == 'JURY_EMERGENCY_ALERT':
            return handle_emergency_alert(body)
        elif action == 'TEST_SMS':
            return test_sms(body)
        elif action == 'UPDATE_LOCATION':
            return handle_update_location(body)
        else:
            return cors_response({
                'status': 'success',
                'message': 'Hybrid SMS Lambda operational',
                'supportedMethods': ['EUM (US)', 'SNS (International)']
            })
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def handle_emergency_alert(body):
    try:
        logger.info("="*80)
        logger.info("HANDLE_EMERGENCY_ALERT CALLED")
        logger.info("="*80)
        
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber')
        detection_type = body.get('detectionType', 'emergency')
        location = body.get('location', {})
        
        logger.info(f"Victim: {victim_name}")
        logger.info(f"Phone: {emergency_phone}")
        logger.info(f"Detection Type: {detection_type}")
        
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        logger.info(f"Incident ID: {incident_id}")
        
        # Compose message
        danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER!"
        place_name = location.get('placeName', 'Unknown location')
        
        sms_message = f"{danger_message}\n\nLocation: {place_name}\nIncident: {incident_id}\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nFrom: AllSenses AI Guardian"
        
        logger.info(f"SMS Message: {sms_message[:100]}...")
        logger.info(f"Calling send_sms_hybrid for {emergency_phone}")
        
        # Send SMS (hybrid)
        sms_result = send_sms_hybrid(emergency_phone, sms_message)
        
        logger.info(f"SMS Result: {json.dumps(sms_result, default=str)}")
        
        # Store incident
        try:
            store_incident(incident_id, victim_name, emergency_phone, detection_type, location)
        except:
            pass
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'message': f"Emergency alert sent via {sms_result.get('method')}",
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsMethod': sms_result.get('method'),
            'originator': sms_result.get('originator'),
            'smsError': sms_result.get('error'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Emergency alert error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def test_sms(body):
    phone = body.get('phoneNumber', '+573222063010')
    message = body.get('message', f'AllSenses Test - {datetime.now().strftime("%H:%M:%S")}')
    
    result = send_sms_hybrid(phone, message)
    
    return cors_response({
        'status': result['status'],
        'phone': phone,
        'messageId': result.get('messageId'),
        'method': result.get('method'),
        'originator': result.get('originator'),
        'error': result.get('error')
    })

def store_incident(incident_id, victim_name, emergency_phone, detection_type, location):
    dynamodb = get_dynamodb()
    table = dynamodb.Table(INCIDENTS_TABLE)
    ttl = int((datetime.now(timezone.utc) + timedelta(days=7)).timestamp())
    
    table.put_item(Item={
        'incidentId': incident_id,
        'victimName': victim_name,
        'emergencyPhone': emergency_phone,
        'detectionType': detection_type,
        'initialLocation': location,
        'createdAt': datetime.now(timezone.utc).isoformat(),
        'status': 'active',
        'ttl': ttl
    })

def handle_update_location(body):
    # Location tracking implementation
    return cors_response({'status': 'success', 'message': 'Location updated'})

def cors_response(data, status_code=200):
    return {
        'statusCode': status_code,
        'headers': {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type,Authorization'
        },
        'body': json.dumps(data, default=str)
    }
