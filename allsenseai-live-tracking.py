import json
import boto3
import uuid
import os
from datetime import datetime, timezone, timedelta
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Lazy-loaded AWS clients
_sms_client = None
_dynamodb = None

# AWS END USER MESSAGING CONFIGURATION
ORIGINATOR_NUMBER = "+12173933490"  # Our registered 10DLC number
CONFIGURATION_SET = "AllSensesAI-SafetyAlerts"  # Our campaign
MESSAGE_TYPE = "TRANSACTIONAL"  # For emergency alerts
REGION = "us-east-1"

# DYNAMODB CONFIGURATION
LOCATION_TABLE = "AllSenses-LocationTracking"
INCIDENT_TABLE = "AllSenses-Incidents"

# TRACKING PAGE URL
# Update after deploying CloudFormation stack:
# - With custom domain: https://track.allsensesai.com
# - Without custom domain: https://YOUR-CLOUDFRONT-DOMAIN.cloudfront.net
TRACKING_BASE_URL = os.environ.get('TRACKING_BASE_URL', "https://track.allsensesai.com")

# JURY DEMO CONFIGURATION
JURY_PHONE_NUMBER = "+13053033060"

def get_sms_client():
    """
    Lazy-load AWS End User Messaging (Pinpoint SMS Voice v2) client
    This is the CORRECT API for 10DLC compliance
    """
    global _sms_client
    if _sms_client is None:
        try:
            _sms_client = boto3.client('pinpoint-sms-voice-v2', region_name=REGION)
            logger.info("AWS End User Messaging client initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize EUM client: {str(e)}")
            raise
    return _sms_client

def get_dynamodb():
    """Lazy-load DynamoDB resource"""
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.resource('dynamodb', region_name=REGION)
    return _dynamodb

def handler(event, context):
    """
    AllSensesAI Emergency SMS Handler with Live Location Tracking
    AWS End User Messaging Compliant
    """
    logger.info(f"AllSenseAI received: {json.dumps(event, default=str)}")
    
    try:
        # Handle CORS preflight
        if event.get('httpMethod') == 'OPTIONS':
            return cors_response({})
        
        # Parse request body
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        action = body.get('action', 'TEST_SMS')
        
        logger.info(f"Processing action: {action}")
        
        # Route to appropriate handler
        if action == 'JURY_EMERGENCY_ALERT':
            return handle_jury_emergency_alert(body)
        elif action == 'JURY_TEST':
            return handle_jury_test(body)
        elif action == 'TEST_SMS':
            return test_sms_direct(body)
        elif action == 'UPDATE_LOCATION':
            return handle_update_location(body)
        elif action == 'GET_LOCATION':
            return handle_get_location(body)
        elif action == 'GET_LOCATION_HISTORY':
            return handle_get_location_history(body)
        elif action == 'CHECK_EUM_CONFIG':
            return check_eum_configuration()
        else:
            return cors_response({
                'status': 'success',
                'message': 'AllSensesAI Lambda is operational',
                'action': action,
                'eumCompliant': True,
                'liveTrackingEnabled': True,
                'originatorNumber': ORIGINATOR_NUMBER,
                'campaign': CONFIGURATION_SET,
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}")
        logger.error(f"Error type: {type(e).__name__}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def handle_jury_emergency_alert(body):
    """
    Handle jury emergency alert with AWS End User Messaging and Live Tracking
    """
    try:
        # Extract configuration
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber', JURY_PHONE_NUMBER)
        detection_type = body.get('detectionType', 'emergency')
        detection_data = body.get('detectionData', {})
        location = body.get('location', {})
        
        # Generate incident ID
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        
        logger.info(f"Processing emergency alert for {victim_name} to {emergency_phone}")
        logger.info(f"Incident ID: {incident_id}")
        
        # Store incident in DynamoDB
        store_incident(incident_id, victim_name, emergency_phone, detection_type, location)
        
        # Store initial location
        if location.get('latitude') and location.get('longitude'):
            store_location_update(incident_id, victim_name, location)
        
        # Generate live tracking URL
        tracking_url = f"{TRACKING_BASE_URL}?incident={incident_id}"
        
        # Compose emergency SMS message with live tracking
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"EMERGENCY ALERT: {victim_name} is in DANGER!\nEmergency words detected: {', '.join(detected_words)}"
        elif detection_type == 'abrupt_noise':
            danger_message = f"EMERGENCY ALERT: {victim_name} is in DANGER!\nSudden loud noise detected"
        else:
            danger_message = f"EMERGENCY ALERT: {victim_name} is in DANGER!\nEmergency situation detected"
        
        # Add location information
        place_name = location.get('placeName', 'Unknown Location')
        
        # Complete SMS message with live tracking
        sms_message = f"🚨 {danger_message}\n\n"
        sms_message += f"LIVE TRACKING:\n{tracking_url}\n\n"
        sms_message += f"Initial Location: {place_name}\n\n"
        sms_message += f"Incident: {incident_id}\n"
        sms_message += f"Time: {datetime.now().strftime('%H:%M:%S')}\n\n"
        sms_message += f"From: AllSensesAI Guardian ({ORIGINATOR_NUMBER})"
        
        logger.info(f"Sending EUM SMS with live tracking to {emergency_phone}")
        logger.info(f"Tracking URL: {tracking_url}")
        
        # Send via AWS End User Messaging
        sms_result = send_eum_sms(emergency_phone, sms_message)
        
        logger.info(f"EUM SMS result: {json.dumps(sms_result, default=str)}")
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Emergency alert processed with live tracking',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
            'trackingUrl': tracking_url,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': True,
            'liveTrackingEnabled': True,
            'originatorNumber': ORIGINATOR_NUMBER,
            'campaign': CONFIGURATION_SET,
            'smsError': sms_result.get('error'),
            'dangerMessage': danger_message,
            'location': location,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Emergency alert error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Emergency alert failed: {str(e)}',
            'errorType': type(e).__name__,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def handle_update_location(body):
    """
    Store location update in DynamoDB for live tracking
    """
    try:
        incident_id = body.get('incidentId')
        victim_name = body.get('victimName', 'Unknown')
        location = body.get('location', {})
        battery_level = body.get('batteryLevel')
        
        if not incident_id:
            return cors_response({
                'status': 'error',
                'message': 'incidentId is required'
            }, 400)
        
        # Store location update
        store_location_update(incident_id, victim_name, location, battery_level)
        
        logger.info(f"Location updated for incident {incident_id}")
        
        return cors_response({
            'status': 'success',
            'message': 'Location updated',
            'incidentId': incident_id,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Location update error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def handle_get_location(body):
    """
    Get latest location for incident (for tracking page)
    """
    try:
        incident_id = body.get('incidentId')
        
        if not incident_id:
            return cors_response({
                'status': 'error',
                'message': 'incidentId is required'
            }, 400)
        
        # Query DynamoDB for latest location
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        response = table.query(
            KeyConditionExpression='incidentId = :id',
            ExpressionAttributeValues={':id': incident_id},
            ScanIndexForward=False,  # Sort descending (latest first)
            Limit=1
        )
        
        if response['Items']:
            location = response['Items'][0]
            return cors_response({
                'status': 'success',
                'location': location,
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
        else:
            return cors_response({
                'status': 'error',
                'message': 'No location found for this incident'
            }, 404)
            
    except Exception as e:
        logger.error(f"Get location error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def handle_get_location_history(body):
    """
    Get location history for incident (movement trail)
    """
    try:
        incident_id = body.get('incidentId')
        limit = body.get('limit', 100)
        
        if not incident_id:
            return cors_response({
                'status': 'error',
                'message': 'incidentId is required'
            }, 400)
        
        # Query DynamoDB for location history
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        response = table.query(
            KeyConditionExpression='incidentId = :id',
            ExpressionAttributeValues={':id': incident_id},
            ScanIndexForward=True,  # Sort ascending (oldest first)
            Limit=limit
        )
        
        return cors_response({
            'status': 'success',
            'history': response['Items'],
            'count': len(response['Items']),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
            
    except Exception as e:
        logger.error(f"Get location history error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def store_incident(incident_id, victim_name, emergency_phone, detection_type, location):
    """Store incident details in DynamoDB"""
    try:
        dynamodb = get_dynamodb()
        table = dynamodb.Table(INCIDENT_TABLE)
        
        table.put_item(
            Item={
                'incidentId': incident_id,
                'victimName': victim_name,
                'emergencyPhone': emergency_phone,
                'detectionType': detection_type,
                'initialLocation': location,
                'createdAt': datetime.now(timezone.utc).isoformat(),
                'status': 'active',
                'ttl': int((datetime.now(timezone.utc) + timedelta(days=7)).timestamp())
            }
        )
        
        logger.info(f"Incident {incident_id} stored in DynamoDB")
        
    except Exception as e:
        logger.error(f"Failed to store incident: {str(e)}")
        # Don't fail the whole request if incident storage fails

def store_location_update(incident_id, victim_name, location, battery_level=None):
    """Store location update in DynamoDB"""
    try:
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        timestamp = int(datetime.now(timezone.utc).timestamp() * 1000)  # Milliseconds for precision
        
        item = {
            'incidentId': incident_id,
            'timestamp': timestamp,
            'victimName': victim_name,
            'latitude': float(location.get('latitude', 0)),
            'longitude': float(location.get('longitude', 0)),
            'accuracy': float(location.get('accuracy', 0)),
            'ttl': int((datetime.now(timezone.utc) + timedelta(hours=24)).timestamp())
        }
        
        # Add optional fields
        if location.get('speed') is not None:
            item['speed'] = float(location['speed'])
        if location.get('heading') is not None:
            item['heading'] = float(location['heading'])
        if battery_level is not None:
            item['batteryLevel'] = int(battery_level)
        
        table.put_item(Item=item)
        
        logger.info(f"Location updated for incident {incident_id}: {location.get('latitude')}, {location.get('longitude')}")
        
    except Exception as e:
        logger.error(f"Failed to store location: {str(e)}")
        # Don't fail the whole request if location storage fails

def handle_jury_test(body):
    """Handle jury test message with AWS End User Messaging"""
    try:
        test_phone = body.get('phoneNumber', JURY_PHONE_NUMBER)
        victim_name = body.get('victimName', 'Test User')
        
        test_message = f"AllSensesAI SYSTEM TEST\n\nSystem ready for {victim_name}!\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nEmergency detection operational.\nLive tracking enabled.\n\nFrom: AllSensesAI Guardian ({ORIGINATOR_NUMBER})"
        
        logger.info(f"Sending EUM test SMS to {test_phone}")
        
        sms_result = send_eum_sms(test_phone, test_message)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Test message processed via AWS End User Messaging',
            'victimName': victim_name,
            'testPhone': test_phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': True,
            'liveTrackingEnabled': True,
            'originatorNumber': ORIGINATOR_NUMBER,
            'campaign': CONFIGURATION_SET,
            'smsError': sms_result.get('error'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Test message error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Test failed: {str(e)}',
            'errorType': type(e).__name__
        }, 500)

def test_sms_direct(body):
    """Direct SMS test endpoint using AWS End User Messaging"""
    try:
        phone = body.get('phoneNumber', '+19543483664')
        message = body.get('message', f'AllSensesAI Test Message via AWS End User Messaging. Time: {datetime.now().strftime("%H:%M:%S")}')
        
        logger.info(f"Direct EUM SMS test to {phone}")
        
        sms_result = send_eum_sms(phone, message)
        
        is_success = sms_result['status'] == 'sent'
        
        return cors_response({
            'status': 'success' if is_success else 'failed',
            'phone': phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': is_success,
            'liveTrackingEnabled': True,
            'originatorNumber': ORIGINATOR_NUMBER,
            'campaign': CONFIGURATION_SET,
            'smsError': sms_result.get('error'),
            'fullResponse': sms_result.get('fullResponse'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Direct SMS test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__,
            'smsStatus': 'failed',
            'smsError': str(e),
            'eumCompliant': False,
            'originatorNumber': ORIGINATOR_NUMBER,
            'campaign': CONFIGURATION_SET,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def send_eum_sms(phone_number, message):
    """
    Send SMS via AWS End User Messaging (Pinpoint SMS Voice v2)
    This is the CORRECT way to use 10DLC compliance
    """
    try:
        logger.info(f"=== AWS END USER MESSAGING SMS SEND ===")
        logger.info(f"Destination: {phone_number}")
        logger.info(f"Originator: {ORIGINATOR_NUMBER}")
        logger.info(f"Campaign: {CONFIGURATION_SET}")
        logger.info(f"Message Type: {MESSAGE_TYPE}")
        logger.info(f"Message Length: {len(message)} characters")
        
        # Get EUM client
        sms_client = get_sms_client()
        
        # Send via AWS End User Messaging API
        response = sms_client.send_text_message(
            DestinationPhoneNumber=phone_number,
            OriginationIdentity=ORIGINATOR_NUMBER,
            MessageBody=message,
            MessageType=MESSAGE_TYPE,
            ConfigurationSetName=CONFIGURATION_SET
        )
        
        message_id = response.get('MessageId')
        
        logger.info(f"=== EUM SMS SENT SUCCESSFULLY ===")
        logger.info(f"MessageId: {message_id}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'phone': phone_number,
            'eumCompliant': True,
            'originatorNumber': ORIGINATOR_NUMBER,
            'campaign': CONFIGURATION_SET,
            'fullResponse': response,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        
    except Exception as e:
        logger.error(f"=== EUM SMS SENDING FAILED ===")
        logger.error(f"Error: {str(e)}")
        
        return {
            'status': 'failed',
            'error': str(e),
            'errorType': type(e).__name__,
            'phone': phone_number,
            'eumCompliant': False,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }

def check_eum_configuration():
    """Check AWS End User Messaging configuration"""
    try:
        sms_client = get_sms_client()
        
        # Try to describe phone numbers
        try:
            phone_response = sms_client.describe_phone_numbers()
            phone_numbers = phone_response.get('PhoneNumbers', [])
        except Exception as e:
            logger.warning(f"Could not describe phone numbers: {str(e)}")
            phone_numbers = []
        
        # Try to describe configuration sets
        try:
            config_response = sms_client.describe_configuration_sets()
            config_sets = config_response.get('ConfigurationSets', [])
        except Exception as e:
            logger.warning(f"Could not describe configuration sets: {str(e)}")
            config_sets = []
        
        return cors_response({
            'status': 'success',
            'message': 'EUM configuration check complete',
            'configuration': {
                'originatorNumber': ORIGINATOR_NUMBER,
                'campaign': CONFIGURATION_SET,
                'messageType': MESSAGE_TYPE,
                'region': REGION,
                'liveTrackingEnabled': True,
                'trackingBaseUrl': TRACKING_BASE_URL,
                'phoneNumbers': phone_numbers,
                'configurationSets': config_sets
            },
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"EUM configuration check failed: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Configuration check failed: {str(e)}',
            'errorType': type(e).__name__
        }, 500)

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
