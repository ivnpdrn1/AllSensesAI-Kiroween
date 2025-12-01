import json
import boto3
import uuid
import os
import re
from datetime import datetime, timezone
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize AWS services
sns = boto3.client('sns')
dynamodb = boto3.resource('dynamodb')
bedrock = boto3.client('bedrock-runtime')

# ============================================================================
# CONFIGURATION - Can be overridden via Lambda Environment Variables
# ============================================================================
DEMO_MODE = os.environ.get('DEMO_MODE', 'false').lower() == 'true'
SMS_ORIGINATOR = os.environ.get('SMS_ORIGINATOR', 'AllSensesAI')
SMS_CAMPAIGN_ID = os.environ.get('SMS_CAMPAIGN_ID', 'AllSensesAI-SafetyAlerts')
TRACKING_URL_BASE = os.environ.get('TRACKING_URL_BASE', 'https://d2s71iq0yaelxo.cloudfront.net/live-tracking.html')

logger.info(f"Lambda initialized - DEMO_MODE: {DEMO_MODE}, SMS_ORIGINATOR: {SMS_ORIGINATOR}")

def handler(event, context):
    """
    AllSensesAI Complete 7-Step Pipeline
    1. Audio Capture â†’ 2. Distress Detection â†’ 3. Event Trigger â†’ 
    4. Geolocation â†’ 5. SMS Dispatch â†’ 6. Contact Confirmation â†’ 7. Analytics
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
        
        # LEGACY COMPATIBILITY: Convert old phoneNumber format to contacts array
        if 'phoneNumber' in body and 'contacts' not in body:
            logger.info("Converting legacy phoneNumber format to contacts array")
            body['contacts'] = [{
                'name': body.get('victimName', 'Emergency Contact'),
                'phone': body['phoneNumber'],
                'optedIn': True,
                'relationship': 'emergency',
                'priority': 1
            }]
        
        action = body.get('action', 'SIMULATE_EMERGENCY')
        
        # Route to appropriate handler
        if action == 'SIMULATE_EMERGENCY':
            return simulate_complete_pipeline(body)
        elif action == 'GET_USER_PROFILE':
            return get_user_profile(body)
        elif action == 'MAKE_REAL_CALL':
            return send_emergency_sms(body)
        elif action == 'JURY_EMERGENCY_ALERT':
            return handle_jury_emergency_alert(body)
        elif action == 'JURY_TEST':
            return handle_jury_test(body)
        elif action == 'CHECK_SNS_STATUS':
            return check_sns_status()
        elif action == 'JURY_DEMO_TEST':
            return jury_demo_test(body)
        elif action == 'TEST_SMS':
            return test_sms_direct(body)
        else:
            return analyze_audio_distress(body)
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}", exc_info=True)
        return cors_response({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def test_sms_direct(body):
    """
    Direct SMS test - matches TEST_SMS action from test scripts
    """
    try:
        phone = body.get('phoneNumber', '+573222063010')
        message = body.get('message', f'AllSensesAI Test. Time: {datetime.now().strftime("%H:%M:%S")}')
        
        logger.info(f"TEST_SMS to {phone}")
        
        sms_result = send_sms_with_eum(phone, message)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'phone': phone,
            'smsMessageId': sms_result.get('messageId'),  # âœ… ROOT LEVEL
            'smsStatus': sms_result['status'],
            'smsResult': sms_result,  # Keep for compatibility
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Test SMS error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def handle_jury_emergency_alert(body):
    """
    Handle emergency alert with configurable victim and phone
    âœ… FIXED: Returns smsMessageId at root level for frontend compatibility
    """
    try:
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber', '+13053033060')
        detection_type = body.get('detectionType', 'emergency')
        detection_data = body.get('detectionData', {})
        location = body.get('location', {})
        
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        
        logger.info(f"JURY_EMERGENCY_ALERT for {victim_name} to {emergency_phone}")
        
        # Compose emergency message
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"ðŸš¨ EMERGENCY: {victim_name} is in DANGER! Words detected: \"{', '.join(detected_words)}\""
        elif detection_type == 'abrupt_noise':
            volume = detection_data.get('volume', 'high')
            danger_message = f"ðŸš¨ EMERGENCY: {victim_name} is in DANGER! Loud noise: {volume} dB"
        else:
            danger_message = f"ðŸš¨ EMERGENCY: {victim_name} is in DANGER! Emergency detected"
        
        place_name = location.get('placeName', 'Unknown location')
        map_link = location.get('mapLink', 'https://maps.google.com/?q=25.7617,-80.1918')
        
        sms_message = f"{danger_message}\n\nLocation: {place_name}\nMap: {map_link}\n\nIncident: {incident_id}\nTime: {datetime.now().strftime('%H:%M:%S')}"
        
        # Send SMS
        sms_result = send_sms_with_eum(emergency_phone, sms_message, incident_id)
        
        logger.info(f"SMS Result: {json.dumps(sms_result, default=str)}")
        
        # âœ… FIX: Return smsMessageId at root level (what frontend expects)
        return cors_response({
            'status': 'success',
            'message': 'Emergency alert sent',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
            'smsMessageId': sms_result.get('messageId'),  # âœ… ROOT LEVEL - Frontend expects this
            'smsStatus': sms_result.get('status'),        # âœ… ROOT LEVEL - Frontend expects this
            'smsMethod': sms_result.get('smsMethod'),     # âœ… ROOT LEVEL
            'eumCompliant': sms_result.get('realSms', False),  # âœ… ROOT LEVEL
            'smsResult': sms_result,  # Keep nested version for backward compatibility
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Emergency alert error: {str(e)}", exc_info=True)
        return cors_response({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

# ... (rest of the code remains the same - send_sms_with_eum, etc.)
