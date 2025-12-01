import json
import boto3
import uuid
import os
from datetime import datetime, timezone
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Lazy-loaded AWS clients (only initialize when needed)
_sns_client = None
_dynamodb_resource = None
_bedrock_client = None

# JURY DEMO CONFIGURATION
JURY_PHONE_NUMBER = "+13053033060"

def get_sns_client():
    """Lazy-load SNS client with explicit region"""
    global _sns_client
    if _sns_client is None:
        try:
            _sns_client = boto3.client('sns', region_name='us-east-1')
            logger.info("SNS client initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize SNS client: {str(e)}")
            raise
    return _sns_client

def get_dynamodb_resource():
    """Lazy-load DynamoDB resource (optional)"""
    global _dynamodb_resource
    if _dynamodb_resource is None:
        try:
            _dynamodb_resource = boto3.resource('dynamodb', region_name='us-east-1')
            logger.info("DynamoDB resource initialized successfully")
        except Exception as e:
            logger.warning(f"DynamoDB initialization failed (optional): {str(e)}")
            _dynamodb_resource = None
    return _dynamodb_resource

def get_bedrock_client():
    """Lazy-load Bedrock client (optional)"""
    global _bedrock_client
    if _bedrock_client is None:
        try:
            _bedrock_client = boto3.client('bedrock-runtime', region_name='us-east-1')
            logger.info("Bedrock client initialized successfully")
        except Exception as e:
            logger.warning(f"Bedrock initialization failed (optional): {str(e)}")
            _bedrock_client = None
    return _bedrock_client

def handler(event, context):
    """
    AllSensesAI Complete 7-Step Pipeline
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
        
        action = body.get('action', 'SIMULATE_EMERGENCY')
        
        # Route to appropriate handler
        if action == 'JURY_EMERGENCY_ALERT':
            return handle_jury_emergency_alert(body)
        elif action == 'JURY_TEST':
            return handle_jury_test(body)
        elif action == 'TEST_SMS':
            return test_sms_direct(body)
        else:
            return cors_response({
                'status': 'success',
                'message': 'AllSensesAI Lambda is operational',
                'action': action,
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
    Handle jury emergency alert with REAL SMS sending
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
        
        # Complete SMS message
        sms_message = f"{danger_message}\n\nLocation: {place_name}\nView: {map_link}\n\nIncident: {incident_id}\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nFrom: AllSensesAI Guardian (+1-217-393-3490)"
        
        logger.info(f"Sending SMS to {emergency_phone}: {sms_message[:100]}...")
        
        # Send REAL SMS
        sms_result = send_real_sms(emergency_phone, sms_message)
        
        logger.info(f"SMS result: {json.dumps(sms_result)}")
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Emergency alert processed',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsRealDelivery': sms_result.get('realSms', False),
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
    Handle jury test message with REAL SMS
    """
    try:
        test_phone = body.get('phoneNumber', JURY_PHONE_NUMBER)
        victim_name = body.get('victimName', 'Test User')
        
        test_message = f"AllSensesAI SYSTEM TEST\n\nSystem ready for {victim_name}!\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nEmergency detection operational.\n\nFrom: AllSensesAI Guardian (+1-217-393-3490)"
        
        logger.info(f"Sending test SMS to {test_phone}")
        
        sms_result = send_real_sms(test_phone, test_message)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Test message processed',
            'victimName': victim_name,
            'testPhone': test_phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsRealDelivery': sms_result.get('realSms', False),
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
    Direct SMS test endpoint
    """
    try:
        phone = body.get('phoneNumber', '+19543483664')
        message = body.get('message', 'AllSensesAI Test Message')
        
        logger.info(f"Direct SMS test to {phone}")
        
        sms_result = send_real_sms(phone, message)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'phone': phone,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsRealDelivery': sms_result.get('realSms', False),
            'smsError': sms_result.get('error'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Direct SMS test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

def send_real_sms(phone_number, message):
    """
    Send REAL SMS via AWS SNS - NO FAKE RESPONSES
    """
    try:
        logger.info(f"Attempting to send SMS to {phone_number}")
        logger.info(f"Message length: {len(message)} characters")
        
        # Get SNS client
        sns = get_sns_client()
        
        # Send SMS
        response = sns.publish(
            PhoneNumber=phone_number,
            Message=message
        )
        
        message_id = response.get('MessageId')
        
        logger.info(f"SMS SENT SUCCESSFULLY! MessageId: {message_id}")
        logger.info(f"Full SNS response: {json.dumps(response, default=str)}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'phone': phone_number,
            'realSms': True,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        
    except Exception as e:
        logger.error(f"SMS SENDING FAILED for {phone_number}")
        logger.error(f"Error: {str(e)}")
        logger.error(f"Error type: {type(e).__name__}")
        logger.error(f"Error details: {repr(e)}")
        
        # Return actual error - NO FAKE SUCCESS
        return {
            'status': 'failed',
            'error': str(e),
            'errorType': type(e).__name__,
            'phone': phone_number,
            'realSms': False,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }

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
