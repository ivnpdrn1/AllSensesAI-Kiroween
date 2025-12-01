import json
import boto3
import uuid
import os
from datetime import datetime, timezone
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize AWS clients
sns = boto3.client('sns')
_eum_client = None

# AWS END USER MESSAGING CONFIGURATION (US only)
ORIGINATOR_NUMBER = "+12173933490"  # US 10DLC number
CONFIGURATION_SET = "AllSensesAI-SafetyAlerts"
MESSAGE_TYPE = "TRANSACTIONAL"
REGION = "us-east-1"

# JURY DEMO CONFIGURATION
JURY_PHONE_NUMBER = "+13053033060"

def get_eum_client():
    """Lazy-load EUM client for US SMS"""
    global _eum_client
    if _eum_client is None:
        _eum_client = boto3.client('pinpoint-sms-voice-v2', region_name=REGION)
        logger.info("EUM client initialized for US SMS")
    return _eum_client

def is_us_number(phone):
    """Check if phone number is US/Canada"""
    return phone.startswith('+1')

def handler(event, context):
    """AllSensesAI Emergency SMS Handler - Hybrid EUM/SNS"""
    logger.info(f"AllSenseAI received: {json.dumps(event, default=str)}")
    
    try:
        if event.get('httpMethod') == 'OPTIONS':
            return cors_response({})
        
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        action = body.get('action', 'TEST_SMS')
        logger.info(f"Processing action: {action}")
        
        if action == 'JURY_EMERGENCY_ALERT':
            return handle_jury_emergency_alert(body)
        elif action == 'JURY_TEST':
            return handle_jury_test(body)
        elif action == 'TEST_SMS':
            return test_sms_direct(body)
        elif action == 'CHECK_EUM_CONFIG':
            return check_eum_configuration()
        else:
            return cors_response({
                'status': 'success',
                'message': 'AllSensesAI Lambda operational',
                'action': action,
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}", exc_info=True)
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def handle_jury_emergency_alert(body):
    """Handle emergency alert with hybrid EUM/SNS"""
    try:
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber', JURY_PHONE_NUMBER)
        detection_type = body.get('detectionType', 'emergency')
        detection_data = body.get('detectionData', {})
        location = body.get('location', {})
        
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        
        logger.info(f"🚨 JURY_EMERGENCY_ALERT for {victim_name} to {emergency_phone}")
        
        # Compose message
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER! Words: {', '.join(detected_words)}"
        elif detection_type == 'abrupt_noise':
            volume = detection_data.get('volume', 'high')
            danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER! Loud noise: {volume} dB"
        else:
            danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER!"
        
        place_name = location.get('placeName', 'Unknown location')
        map_link = location.get('mapLink', 'https://maps.google.com/?q=25.7617,-80.1918')
        
        sms_message = f"{danger_message}\n\nLocation: {place_name}\nMap: {map_link}\n\nIncident: {incident_id}\nTime: {datetime.now().strftime('%H:%M:%S')}"
        
        # Send SMS (hybrid EUM/SNS)
        sms_result = send_sms_hybrid(emergency_phone, sms_message, incident_id)
        
        logger.info(f"📱 SMS Result: {json.dumps(sms_result, default=str)}")
        
        return cors_response({
            'status': 'success',
            'message': 'Emergency alert sent',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result.get('status'),
            'smsMethod': sms_result.get('method'),
            'eumCompliant': sms_result.get('method') == 'EUM',
            'smsResult': sms_result,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"❌ Emergency alert error: {str(e)}", exc_info=True)
        return cors_response({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def handle_jury_test(body):
    """Handle test message"""
    try:
        test_phone = body.get('phoneNumber', JURY_PHONE_NUMBER)
        victim_name = body.get('victimName', 'Test User')
        
        test_message = f"AllSensesAI TEST\n\nSystem ready for {victim_name}!\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nEmergency detection operational."
        
        logger.info(f"JURY_TEST to {test_phone}")
        
        sms_result = send_sms_hybrid(test_phone, test_message)
        
        return cors_response({
            'status': 'success',
            'message': 'Test message sent',
            'victimName': victim_name,
            'testPhone': test_phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsMethod': sms_result.get('method'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def test_sms_direct(body):
    """Direct SMS test"""
    try:
        phone = body.get('phoneNumber', '+573222063010')
        message = body.get('message', f'AllSensesAI Test. Time: {datetime.now().strftime("%H:%M:%S")}')
        
        logger.info(f"TEST_SMS to {phone}")
        
        sms_result = send_sms_hybrid(phone, message)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'phone': phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsMethod': sms_result.get('method'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Test SMS error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def send_sms_hybrid(phone_number, message, event_id=None):
    """
    Hybrid SMS: Use EUM for US, SNS for international
    """
    try:
        # Validate phone
        if not phone_number or not phone_number.startswith('+'):
            return {
                'status': 'failed',
                'error': 'Invalid phone number format',
                'phone': phone_number
            }
        
        # Choose method based on destination
        if is_us_number(phone_number):
            logger.info(f"📱 US number detected - using EUM")
            return send_via_eum(phone_number, message)
        else:
            logger.info(f"🌍 International number detected - using SNS")
            return send_via_sns(phone_number, message)
            
    except Exception as e:
        logger.error(f"❌ SMS error: {str(e)}", exc_info=True)
        return {
            'status': 'failed',
            'error': str(e),
            'phone': phone_number
        }

def send_via_eum(phone_number, message):
    """Send via AWS End User Messaging (US only)"""
    try:
        logger.info(f"=== EUM SMS SEND ===")
        logger.info(f"Destination: {phone_number}")
        logger.info(f"Originator: {ORIGINATOR_NUMBER}")
        
        eum_client = get_eum_client()
        
        response = eum_client.send_text_message(
            DestinationPhoneNumber=phone_number,
            OriginationIdentity=ORIGINATOR_NUMBER,
            MessageBody=message,
            MessageType=MESSAGE_TYPE,
            ConfigurationSetName=CONFIGURATION_SET
        )
        
        message_id = response.get('MessageId')
        
        logger.info(f"✅ EUM SMS SENT: {message_id}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'phone': phone_number,
            'method': 'EUM',
            'originator': ORIGINATOR_NUMBER,
            'campaign': CONFIGURATION_SET,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        
    except Exception as e:
        logger.error(f"❌ EUM SMS FAILED: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e),
            'phone': phone_number,
            'method': 'EUM'
        }

def send_via_sns(phone_number, message):
    """Send via SNS (international)"""
    try:
        logger.info(f"=== SNS SMS SEND ===")
        logger.info(f"Destination: {phone_number}")
        
        # SNS attributes for international SMS
        message_attributes = {
            'AWS.SNS.SMS.SMSType': {
                'DataType': 'String',
                'StringValue': 'Transactional'
            }
        }
        
        response = sns.publish(
            PhoneNumber=phone_number,
            Message=message,
            MessageAttributes=message_attributes
        )
        
        message_id = response['MessageId']
        
        logger.info(f"✅ SNS SMS SENT: {message_id}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'phone': phone_number,
            'method': 'SNS',
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        
    except Exception as e:
        logger.error(f"❌ SNS SMS FAILED: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e),
            'phone': phone_number,
            'method': 'SNS'
        }

def check_eum_configuration():
    """Check EUM configuration"""
    return cors_response({
        'status': 'success',
        'message': 'Hybrid EUM/SNS configuration',
        'configuration': {
            'eumOriginator': ORIGINATOR_NUMBER,
            'eumCampaign': CONFIGURATION_SET,
            'eumRegion': REGION,
            'snsEnabled': True,
            'hybridMode': True
        },
        'timestamp': datetime.now(timezone.utc).isoformat()
    })

def cors_response(data, status_code=200):
    """Return CORS-enabled response"""
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
