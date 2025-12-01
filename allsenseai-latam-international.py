import json
import boto3
import uuid
import os
from datetime import datetime, timezone, timedelta
import logging
import re

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Lazy-loaded AWS clients
_sms_client = None
_dynamodb = None

# AWS END USER MESSAGING CONFIGURATION
ORIGINATOR_NUMBER_US = "+12173933490"  # US 10DLC number
CONFIGURATION_SET = "AllSensesAI-SafetyAlerts"
MESSAGE_TYPE = "TRANSACTIONAL"
REGION = "us-east-1"

# DYNAMODB CONFIGURATION
LOCATION_TABLE = "AllSenses-LocationTracking"
INCIDENT_TABLE = "AllSenses-Incidents"

# TRACKING PAGE URL
TRACKING_BASE_URL = "https://track.allsensesai.com"

# COUNTRY CONFIGURATIONS
COUNTRY_CONFIG = {
    '+1': {  # USA/Canada
        'name': 'USA',
        'emergency_number': '911',
        'language': 'en',
        'timezone': 'America/New_York',
        'originator': ORIGINATOR_NUMBER_US,
        'use_eum': True
    },
    '+57': {  # Colombia
        'name': 'Colombia',
        'emergency_number': '123',
        'language': 'es',
        'timezone': 'America/Bogota',
        'originator': ORIGINATOR_NUMBER_US,  # Use US number for international
        'use_eum': True
    },
    '+56': {  # Chile
        'name': 'Chile',
        'emergency_number': '133',  # Ambulance (also 131 Police, 132 Fire)
        'language': 'es',
        'timezone': 'America/Santiago',
        'originator': ORIGINATOR_NUMBER_US,
        'use_eum': True
    },
    '+58': {  # Venezuela
        'name': 'Venezuela',
        'emergency_number': '911',
        'language': 'es',
        'timezone': 'America/Caracas',
        'originator': ORIGINATOR_NUMBER_US,
        'use_eum': True
    },
    '+52': {  # Mexico
        'name': 'Mexico',
        'emergency_number': '911',
        'language': 'es',
        'timezone': 'America/Mexico_City',
        'originator': ORIGINATOR_NUMBER_US,
        'use_eum': True
    }
}

# TRANSLATIONS
TRANSLATIONS = {
    'en': {
        'emergency_alert': 'EMERGENCY ALERT',
        'is_in_danger': 'is in DANGER!',
        'emergency_words_detected': 'Emergency words detected',
        'loud_noise_detected': 'Sudden loud noise detected',
        'emergency_detected': 'Emergency situation detected',
        'live_tracking': 'LIVE TRACKING',
        'initial_location': 'Initial Location',
        'incident': 'Incident',
        'time': 'Time',
        'from': 'From',
        'system_test': 'SYSTEM TEST',
        'system_ready': 'System ready for',
        'emergency_detection_operational': 'Emergency detection operational',
        'live_tracking_enabled': 'Live tracking enabled'
    },
    'es': {
        'emergency_alert': 'ALERTA DE EMERGENCIA',
        'is_in_danger': 'está en PELIGRO!',
        'emergency_words_detected': 'Palabras de emergencia detectadas',
        'loud_noise_detected': 'Ruido fuerte repentino detectado',
        'emergency_detected': 'Situación de emergencia detectada',
        'live_tracking': 'RASTREO EN VIVO',
        'initial_location': 'Ubicación Inicial',
        'incident': 'Incidente',
        'time': 'Hora',
        'from': 'De',
        'system_test': 'PRUEBA DEL SISTEMA',
        'system_ready': 'Sistema listo para',
        'emergency_detection_operational': 'Detección de emergencias operativa',
        'live_tracking_enabled': 'Rastreo en vivo habilitado'
    }
}

def get_sms_client():
    """Lazy-load AWS End User Messaging client"""
    global _sms_client
    if _sms_client is None:
        try:
            _sms_client = boto3.client('pinpoint-sms-voice-v2', region_name=REGION)
            logger.info("AWS End User Messaging client initialized")
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

def detect_country(phone_number):
    """
    Detect country from phone number
    Returns country config or default to US
    """
    # Normalize phone number
    phone = phone_number.strip().replace(' ', '').replace('-', '')
    
    # Check each country code
    for code, config in COUNTRY_CONFIG.items():
        if phone.startswith(code):
            logger.info(f"Detected country: {config['name']} ({code})")
            return code, config
    
    # Default to US
    logger.info(f"Unknown country code, defaulting to US")
    return '+1', COUNTRY_CONFIG['+1']

def get_translation(key, language='en'):
    """Get translated text"""
    return TRANSLATIONS.get(language, TRANSLATIONS['en']).get(key, key)

def format_phone_number(phone_number):
    """Format phone number for display"""
    # Remove + and spaces
    phone = phone_number.replace('+', '').replace(' ', '').replace('-', '')
    
    # Format based on country
    if phone.startswith('1'):  # USA/Canada
        return f"+1-{phone[1:4]}-{phone[4:7]}-{phone[7:]}"
    elif phone.startswith('57'):  # Colombia
        return f"+57-{phone[2:5]}-{phone[5:]}"
    elif phone.startswith('56'):  # Chile
        return f"+56-{phone[2:3]}-{phone[3:]}"
    elif phone.startswith('58'):  # Venezuela
        return f"+58-{phone[2:5]}-{phone[5:]}"
    elif phone.startswith('52'):  # Mexico
        return f"+52-{phone[2:4]}-{phone[4:]}"
    
    return phone_number

def handler(event, context):
    """
    AllSensesAI Emergency SMS Handler - International Support
    Supports: USA, Colombia, Chile, Venezuela, Mexico
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
                'internationalSupport': True,
                'supportedCountries': ['USA', 'Colombia', 'Chile', 'Venezuela', 'Mexico'],
                'liveTrackingEnabled': True,
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def handle_jury_emergency_alert(body):
    """
    Handle emergency alert with international support
    """
    try:
        # Extract configuration
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber')
        detection_type = body.get('detectionType', 'emergency')
        detection_data = body.get('detectionData', {})
        location = body.get('location', {})
        
        if not emergency_phone:
            return cors_response({
                'status': 'error',
                'message': 'phoneNumber is required'
            }, 400)
        
        # Detect country and get config
        country_code, country_config = detect_country(emergency_phone)
        language = country_config['language']
        
        # Generate incident ID
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        
        logger.info(f"Processing emergency for {victim_name} in {country_config['name']}")
        logger.info(f"Language: {language}, Emergency number: {country_config['emergency_number']}")
        
        # Store incident
        store_incident(incident_id, victim_name, emergency_phone, detection_type, location, country_code)
        
        # Store initial location
        if location.get('latitude') and location.get('longitude'):
            store_location_update(incident_id, victim_name, location)
        
        # Generate live tracking URL
        tracking_url = f"{TRACKING_BASE_URL}?incident={incident_id}"
        
        # Compose emergency SMS message (localized)
        t = lambda key: get_translation(key, language)
        
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"{t('emergency_alert')}: {victim_name} {t('is_in_danger')}\n{t('emergency_words_detected')}: {', '.join(detected_words)}"
        elif detection_type == 'abrupt_noise':
            danger_message = f"{t('emergency_alert')}: {victim_name} {t('is_in_danger')}\n{t('loud_noise_detected')}"
        else:
            danger_message = f"{t('emergency_alert')}: {victim_name} {t('is_in_danger')}\n{t('emergency_detected')}"
        
        # Add location information
        place_name = location.get('placeName', 'Unknown Location')
        
        # Complete SMS message (localized)
        sms_message = f"🚨 {danger_message}\n\n"
        sms_message += f"{t('live_tracking')}:\n{tracking_url}\n\n"
        sms_message += f"{t('initial_location')}: {place_name}\n\n"
        sms_message += f"{t('incident')}: {incident_id}\n"
        sms_message += f"{t('time')}: {datetime.now().strftime('%H:%M:%S')}\n\n"
        sms_message += f"{t('from')}: AllSensesAI Guardian ({format_phone_number(country_config['originator'])})"
        
        logger.info(f"Sending SMS to {emergency_phone} via {country_config['name']}")
        
        # Send SMS
        sms_result = send_international_sms(
            emergency_phone,
            sms_message,
            country_config
        )
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Emergency alert processed with international support',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'country': country_config['name'],
            'countryCode': country_code,
            'language': language,
            'emergencyNumber': country_config['emergency_number'],
            'detectionType': detection_type,
            'trackingUrl': tracking_url,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': True,
            'internationalSupport': True,
            'liveTrackingEnabled': True,
            'originatorNumber': country_config['originator'],
            'smsError': sms_result.get('error'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Emergency alert error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Emergency alert failed: {str(e)}',
            'errorType': type(e).__name__
        }, 500)

def send_international_sms(phone_number, message, country_config):
    """
    Send SMS internationally via AWS EUM
    """
    try:
        logger.info(f"=== INTERNATIONAL SMS SEND ===")
        logger.info(f"Destination: {phone_number} ({country_config['name']})")
        logger.info(f"Originator: {country_config['originator']}")
        logger.info(f"Language: {country_config['language']}")
        
        sms_client = get_sms_client()
        
        # Send via AWS End User Messaging
        response = sms_client.send_text_message(
            DestinationPhoneNumber=phone_number,
            OriginationIdentity=country_config['originator'],
            MessageBody=message,
            MessageType=MESSAGE_TYPE,
            ConfigurationSetName=CONFIGURATION_SET
        )
        
        message_id = response.get('MessageId')
        
        logger.info(f"=== SMS SENT SUCCESSFULLY ===")
        logger.info(f"MessageId: {message_id}")
        logger.info(f"Country: {country_config['name']}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'phone': phone_number,
            'country': country_config['name'],
            'eumCompliant': True,
            'timestamp': datetime.now(timezone.utc).isoformat()
        }
        
    except Exception as e:
        logger.error(f"=== SMS SENDING FAILED ===")
        logger.error(f"Error: {str(e)}")
        
        return {
            'status': 'failed',
            'error': str(e),
            'errorType': type(e).__name__,
            'phone': phone_number,
            'country': country_config['name'],
            'timestamp': datetime.now(timezone.utc).isoformat()
        }

def handle_jury_test(body):
    """Handle test message with international support"""
    try:
        test_phone = body.get('phoneNumber')
        victim_name = body.get('victimName', 'Test User')
        
        if not test_phone:
            return cors_response({
                'status': 'error',
                'message': 'phoneNumber is required'
            }, 400)
        
        # Detect country
        country_code, country_config = detect_country(test_phone)
        language = country_config['language']
        t = lambda key: get_translation(key, language)
        
        test_message = f"AllSensesAI {t('system_test')}\n\n"
        test_message += f"{t('system_ready')} {victim_name}!\n\n"
        test_message += f"{t('time')}: {datetime.now().strftime('%H:%M:%S')}\n\n"
        test_message += f"{t('emergency_detection_operational')}.\n"
        test_message += f"{t('live_tracking_enabled')}.\n\n"
        test_message += f"{t('from')}: AllSensesAI Guardian ({format_phone_number(country_config['originator'])})"
        
        logger.info(f"Sending test SMS to {test_phone} ({country_config['name']})")
        
        sms_result = send_international_sms(test_phone, test_message, country_config)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Test message processed with international support',
            'victimName': victim_name,
            'testPhone': test_phone,
            'country': country_config['name'],
            'language': language,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': True,
            'internationalSupport': True,
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
    """Direct SMS test with international support"""
    try:
        phone = body.get('phoneNumber')
        message = body.get('message')
        
        if not phone:
            return cors_response({
                'status': 'error',
                'message': 'phoneNumber is required'
            }, 400)
        
        # Detect country
        country_code, country_config = detect_country(phone)
        
        if not message:
            message = f'AllSensesAI Test Message. Time: {datetime.now().strftime("%H:%M:%S")}'
        
        logger.info(f"Direct SMS test to {phone} ({country_config['name']})")
        
        sms_result = send_international_sms(phone, message, country_config)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'phone': phone,
            'country': country_config['name'],
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'eumCompliant': sms_result['status'] == 'sent',
            'internationalSupport': True,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Direct SMS test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

# Location tracking functions (same as before)
def handle_update_location(body):
    """Store location update"""
    try:
        incident_id = body.get('incidentId')
        victim_name = body.get('victimName', 'Unknown')
        location = body.get('location', {})
        battery_level = body.get('batteryLevel')
        
        if not incident_id:
            return cors_response({'status': 'error', 'message': 'incidentId required'}, 400)
        
        store_location_update(incident_id, victim_name, location, battery_level)
        
        return cors_response({
            'status': 'success',
            'message': 'Location updated',
            'incidentId': incident_id
        })
    except Exception as e:
        logger.error(f"Location update error: {str(e)}")
        return cors_response({'status': 'error', 'message': str(e)}, 500)

def handle_get_location(body):
    """Get latest location"""
    try:
        incident_id = body.get('incidentId')
        if not incident_id:
            return cors_response({'status': 'error', 'message': 'incidentId required'}, 400)
        
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        response = table.query(
            KeyConditionExpression='incidentId = :id',
            ExpressionAttributeValues={':id': incident_id},
            ScanIndexForward=False,
            Limit=1
        )
        
        if response['Items']:
            return cors_response({'status': 'success', 'location': response['Items'][0]})
        else:
            return cors_response({'status': 'error', 'message': 'No location found'}, 404)
    except Exception as e:
        logger.error(f"Get location error: {str(e)}")
        return cors_response({'status': 'error', 'message': str(e)}, 500)

def handle_get_location_history(body):
    """Get location history"""
    try:
        incident_id = body.get('incidentId')
        limit = body.get('limit', 100)
        
        if not incident_id:
            return cors_response({'status': 'error', 'message': 'incidentId required'}, 400)
        
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        response = table.query(
            KeyConditionExpression='incidentId = :id',
            ExpressionAttributeValues={':id': incident_id},
            ScanIndexForward=True,
            Limit=limit
        )
        
        return cors_response({
            'status': 'success',
            'history': response['Items'],
            'count': len(response['Items'])
        })
    except Exception as e:
        logger.error(f"Get history error: {str(e)}")
        return cors_response({'status': 'error', 'message': str(e)}, 500)

def store_incident(incident_id, victim_name, emergency_phone, detection_type, location, country_code):
    """Store incident in DynamoDB"""
    try:
        dynamodb = get_dynamodb()
        table = dynamodb.Table(INCIDENT_TABLE)
        
        table.put_item(
            Item={
                'incidentId': incident_id,
                'victimName': victim_name,
                'emergencyPhone': emergency_phone,
                'countryCode': country_code,
                'detectionType': detection_type,
                'initialLocation': location,
                'createdAt': datetime.now(timezone.utc).isoformat(),
                'status': 'active',
                'ttl': int((datetime.now(timezone.utc) + timedelta(days=7)).timestamp())
            }
        )
    except Exception as e:
        logger.error(f"Failed to store incident: {str(e)}")

def store_location_update(incident_id, victim_name, location, battery_level=None):
    """Store location update"""
    try:
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        
        timestamp = int(datetime.now(timezone.utc).timestamp() * 1000)
        
        item = {
            'incidentId': incident_id,
            'timestamp': timestamp,
            'victimName': victim_name,
            'latitude': float(location.get('latitude', 0)),
            'longitude': float(location.get('longitude', 0)),
            'accuracy': float(location.get('accuracy', 0)),
            'ttl': int((datetime.now(timezone.utc) + timedelta(hours=24)).timestamp())
        }
        
        if location.get('speed') is not None:
            item['speed'] = float(location['speed'])
        if location.get('heading') is not None:
            item['heading'] = float(location['heading'])
        if battery_level is not None:
            item['batteryLevel'] = int(battery_level)
        
        table.put_item(Item=item)
    except Exception as e:
        logger.error(f"Failed to store location: {str(e)}")

def check_eum_configuration():
    """Check configuration"""
    try:
        return cors_response({
            'status': 'success',
            'message': 'International configuration active',
            'configuration': {
                'internationalSupport': True,
                'supportedCountries': list(COUNTRY_CONFIG.keys()),
                'countryDetails': {code: {
                    'name': config['name'],
                    'emergency_number': config['emergency_number'],
                    'language': config['language']
                } for code, config in COUNTRY_CONFIG.items()},
                'liveTrackingEnabled': True,
                'region': REGION
            }
        })
    except Exception as e:
        logger.error(f"Config check failed: {str(e)}")
        return cors_response({'status': 'error', 'message': str(e)}, 500)

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
