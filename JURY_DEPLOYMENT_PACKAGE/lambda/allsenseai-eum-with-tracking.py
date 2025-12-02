import json
import boto3
import uuid
import os
from datetime import datetime, timezone, timedelta
import logging
from decimal import Decimal

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

# JURY DEMO CONFIGURATION
JURY_PHONE_NUMBER = "+13053033060"

# DYNAMODB TABLES
LOCATION_TABLE = "AllSenses-LocationTracking"
INCIDENTS_TABLE = "AllSenses-Incidents"

def get_sms_client():
    """
    Lazy-load AWS End User Messaging (Pinpoint SMS Voice v2) client
    This is the CORRECT API for 10DLC compliance
    """
    global _sms_client
    if _sms_client is None:
        try:
            # Use pinpoint-sms-voice-v2 for AWS End User Messaging
            _sms_client = boto3.client('pinpoint-sms-voice-v2', region_name=REGION)
            logger.info("AWS End User Messaging client initialized successfully")
            logger.info(f"Using Originator: {ORIGINATOR_NUMBER}")
            logger.info(f"Using Campaign: {CONFIGURATION_SET}")
        except Exception as e:
            logger.error(f"Failed to initialize EUM client: {str(e)}")
            raise
    return _sms_client

def get_dynamodb():
    """
    Lazy-load DynamoDB resource
    """
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.resource('dynamodb', region_name=REGION)
        logger.info("DynamoDB resource initialized successfully")
    return _dynamodb

def handler(event, context):
    """
    AllSensesAI Emergency SMS Handler - AWS End User Messaging Compliant + Live Tracking
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
        elif action == 'CHECK_EUM_CONFIG':
            return check_eum_configuration()
        # NEW: Location tracking handlers
        elif action == 'UPDATE_LOCATION':
            return handle_update_location(body)
        elif action == 'GET_LOCATION':
            return handle_get_location(body)
        elif action == 'GET_LOCATION_HISTORY':
            return handle_get_location_history(body)
        else:
            return cors_response({
                'status': 'success',
                'message': 'AllSensesAI Lambda is operational with live tracking',
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
    Handle jury emergency alert with AWS End User Messaging + Create incident for tracking
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
        logger.info(f"Using EUM Originator: {ORIGINATOR_NUMBER}")
        logger.info(f"Using EUM Campaign: {CONFIGURATION_SET}")
        
        # Store incident in DynamoDB
        try:
            store_incident(incident_id, victim_name, emergency_phone, detection_type, location)
            logger.info(f"Incident {incident_id} stored in DynamoDB")
        except Exception as e:
            logger.error(f"Failed to store incident: {str(e)}")
            # Continue with SMS even if DynamoDB fails
        
        # Compose emergency SMS message
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"EMERGENCY ALERT: {victim_name} is in DANGER! Emergency words detected: {', '.join(detected_words)}"
        elif detection_type == 'abrupt_noise':
            volume = detection_data.get('volume', 'high')
            danger_message = f"EMERGENCY ALERT: {victim_name} is in DANGER! Sudden loud noise detected"
        else:
            danger_message = f"EMERGENCY ALERT: {victim_name} is in DANGER! Emergency situation detected"
        
        # Add location information
        place_name = location.get('placeName', 'Miami Convention Center, FL')
        map_link = location.get('mapLink', 'https://maps.google.com/?q=25.7617,-80.1918')
        
        # Complete SMS message with tracking URL
        tracking_url = f"https://track.allsensesai.com?incident={incident_id}"
        sms_message = f"{danger_message}\n\nLIVE TRACKING:\n{tracking_url}\n\nInitial Location: {place_name}\n\nIncident: {incident_id}\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nFrom: AllSensesAI Guardian ({ORIGINATOR_NUMBER})"
        
        logger.info(f"Sending EUM SMS to {emergency_phone}")
        logger.info(f"Message preview: {sms_message[:100]}...")
        
        # Send via AWS End User Messaging
        sms_result = send_eum_sms(emergency_phone, sms_message)
        
        logger.info(f"EUM SMS result: {json.dumps(sms_result, default=str)}")
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Emergency alert processed via AWS End User Messaging with live tracking',
            'incidentId': incident_id,
            'trackingUrl': tracking_url,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
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
        logger.error(f"Error type: {type(e).__name__}")
        
        return cors_response({
            'status': 'error',
            'message': f'Emergency alert failed: {str(e)}',
            'errorType': type(e).__name__,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def handle_jury_test(body):
    """
    Handle jury test message with AWS End User Messaging
    """
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
    """
    Direct SMS test endpoint using AWS End User Messaging
    """
    try:
        phone = body.get('phoneNumber', '+19543483664')
        message = body.get('message', f'AllSensesAI Test Message via AWS End User Messaging. Time: {datetime.now().strftime("%H:%M:%S")}')
        
        logger.info(f"Direct EUM SMS test to {phone}")
        
        sms_result = send_eum_sms(phone, message)
        
        # Determine if this was a success or failure
        is_success = sms_result['status'] == 'sent'
        
        return cors_response({
            'status': 'success' if is_success else 'failed',
            'phone': phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': is_success,  # False if SMS failed
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
        logger.info(f"Region: {REGION}")
        
        # Get EUM client
        sms_client = get_sms_client()
        
        # Send via AWS End User Messaging API
        response = sms_client.send_text_message(
            DestinationPhoneNumber=phone_number,
            OriginationIdentity=ORIGINATOR_NUMBER,  # Our registered 10DLC number
            MessageBody=message,
            MessageType=MESSAGE_TYPE,  # TRANSACTIONAL for emergency alerts
            ConfigurationSetName=CONFIGURATION_SET  # Our campaign
        )
        
        message_id = response.get('MessageId')
        
        logger.info(f"=== EUM SMS SENT SUCCESSFULLY ===")
        logger.info(f"MessageId: {message_id}")
        logger.info(f"Full Response: {json.dumps(response, default=str)}")
        logger.info(f"This message WILL appear in AWS End User Messaging Dashboard")
        
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
        logger.error(f"Destination: {phone_number}")
        logger.error(f"Error: {str(e)}")
        logger.error(f"Error Type: {type(e).__name__}")
        logger.error(f"Error Details: {repr(e)}")
        
        # Check for specific errors
        error_code = getattr(e, 'response', {}).get('Error', {}).get('Code', 'Unknown')
        error_message = getattr(e, 'response', {}).get('Error', {}).get('Message', str(e))
        
        logger.error(f"AWS Error Code: {error_code}")
        logger.error(f"AWS Error Message: {error_message}")
        
        return {
            'status': 'failed',
            'error': str(e),
            'errorType': type(e).__name__,
            'errorCode': error_code,
            'errorMessage': error_message,
            'phone': phone_number,
            'eumCompliant': False,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }

def check_eum_configuration():
    """
    Check AWS End User Messaging configuration
    """
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
                'phoneNumbers': phone_numbers,
                'configurationSets': config_sets
            },
            'liveTrackingEnabled': True,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"EUM configuration check failed: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Configuration check failed: {str(e)}',
            'errorType': type(e).__name__
        }, 500)

# ============================================================================
# NEW: LIVE LOCATION TRACKING HANDLERS
# ============================================================================

def store_incident(incident_id, victim_name, emergency_phone, detection_type, location):
    """
    Store incident in DynamoDB Incidents table
    """
    try:
        dynamodb = get_dynamodb()
        table = dynamodb.Table(INCIDENTS_TABLE)
        
        # Calculate TTL (7 days from now)
        ttl = int((datetime.now(timezone.utc) + timedelta(days=7)).timestamp())
        
        item = {
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
            'initialLocation': {
                'latitude': location.get('latitude'),
                'longitude': location.get('longitude'),
                'placeName': location.get('placeName', 'Unknown'),
                'mapLink': location.get('mapLink', '')
            },
            'createdAt': datetime.now(timezone.utc).isoformat(),
            'status': 'active',
            'ttl': ttl
        }
        
        table.put_item(Item=item)
        logger.info(f"Incident {incident_id} stored successfully")
        
    except Exception as e:
        logger.error(f"Failed to store incident: {str(e)}")
        raise

def handle_update_location(body):
    """
    Store location update in DynamoDB LocationTracking table
    """
    try:
        incident_id = body.get('incidentId')
        location = body.get('location', {})
        battery_level = body.get('batteryLevel')
        
        if not incident_id:
            return cors_response({
                'status': 'error',
                'message': 'incidentId is required'
            }, 400)
        
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        # Use milliseconds timestamp for sort key
        timestamp = int(datetime.now(timezone.utc).timestamp() * 1000)
        
        # Calculate TTL (24 hours from now)
        ttl = int((datetime.now(timezone.utc) + timedelta(hours=24)).timestamp())
        
        item = {
            'incidentId': incident_id,
            'timestamp': timestamp,
            'latitude': Decimal(str(location.get('latitude', 0))),
            'longitude': Decimal(str(location.get('longitude', 0))),
            'accuracy': Decimal(str(location.get('accuracy', 0))),
            'speed': Decimal(str(location.get('speed', 0))) if location.get('speed') else None,
            'heading': Decimal(str(location.get('heading', 0))) if location.get('heading') else None,
            'batteryLevel': battery_level,
            'ttl': ttl
        }
        
        # Remove None values
        item = {k: v for k, v in item.items() if v is not None}
        
        table.put_item(Item=item)
        
        logger.info(f"Location update stored for incident {incident_id}")
        
        return cors_response({
            'status': 'success',
            'message': 'Location updated successfully',
            'incidentId': incident_id,
            'timestamp': timestamp
        })
        
    except Exception as e:
        logger.error(f"Update location error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

def handle_get_location(body):
    """
    Get latest location for an incident
    """
    try:
        incident_id = body.get('incidentId')
        
        if not incident_id:
            return cors_response({
                'status': 'error',
                'message': 'incidentId is required'
            }, 400)
        
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        # Query for latest location (descending order, limit 1)
        response = table.query(
            KeyConditionExpression='incidentId = :id',
            ExpressionAttributeValues={':id': incident_id},
            ScanIndexForward=False,  # Descending order
            Limit=1
        )
        
        items = response.get('Items', [])
        
        if not items:
            return cors_response({
                'status': 'success',
                'message': 'No location data found',
                'incidentId': incident_id,
                'location': None
            })
        
        location_data = items[0]
        
        # Convert Decimal to float for JSON serialization
        location = {
            'latitude': float(location_data['latitude']),
            'longitude': float(location_data['longitude']),
            'accuracy': float(location_data.get('accuracy', 0)),
            'speed': float(location_data['speed']) if location_data.get('speed') else None,
            'heading': float(location_data['heading']) if location_data.get('heading') else None,
            'batteryLevel': location_data.get('batteryLevel'),
            'timestamp': location_data['timestamp']
        }
        
        logger.info(f"Retrieved latest location for incident {incident_id}")
        
        return cors_response({
            'status': 'success',
            'incidentId': incident_id,
            'location': location
        })
        
    except Exception as e:
        logger.error(f"Get location error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

def handle_get_location_history(body):
    """
    Get location history for an incident (movement trail)
    """
    try:
        incident_id = body.get('incidentId')
        limit = body.get('limit', 100)  # Default to last 100 points
        
        if not incident_id:
            return cors_response({
                'status': 'error',
                'message': 'incidentId is required'
            }, 400)
        
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        # Query for location history
        response = table.query(
            KeyConditionExpression='incidentId = :id',
            ExpressionAttributeValues={':id': incident_id},
            ScanIndexForward=False,  # Most recent first
            Limit=limit
        )
        
        items = response.get('Items', [])
        
        # Convert Decimal to float for JSON serialization
        locations = []
        for item in items:
            locations.append({
                'latitude': float(item['latitude']),
                'longitude': float(item['longitude']),
                'accuracy': float(item.get('accuracy', 0)),
                'speed': float(item['speed']) if item.get('speed') else None,
                'heading': float(item['heading']) if item.get('heading') else None,
                'batteryLevel': item.get('batteryLevel'),
                'timestamp': item['timestamp']
            })
        
        logger.info(f"Retrieved {len(locations)} location points for incident {incident_id}")
        
        return cors_response({
            'status': 'success',
            'incidentId': incident_id,
            'locationCount': len(locations),
            'locations': locations
        })
        
    except Exception as e:
        logger.error(f"Get location history error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

def cors_response(data, status_code=200):
    """
    Return CORS-enabled response
    """
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
