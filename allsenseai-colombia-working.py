import json
import boto3
import uuid
import os
from datetime import datetime, timezone, timedelta
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Lazy-loaded AWS clients
_eum_client = None
_sns_client = None
_dynamodb = None

# AWS END USER MESSAGING CONFIGURATION (US ONLY)
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
    '+1': {
        'name': 'USA',
        'emergency_number': '911',
        'language': 'en',
        'use_eum': True,  # Use EUM for US (10DLC compliant)
        'use_sns': False
    },
    '+57': {
        'name': 'Colombia',
        'emergency_number': '123',
        'language': 'es',
        'use_eum': False,  # Cannot use EUM for international
        'use_sns': True    # Must use SNS for international
    },
    '+56': {
        'name': 'Chile',
        'emergency_number': '133',
        'language': 'es',
        'use_eum': False,
        'use_sns': True
    },
    '+58': {
        'name': 'Venezuela',
        'emergency_number': '911',
        'language': 'es',
        'use_eum': False,
        'use_sns': True
    },
    '+52': {
        'name': 'Mexico',
        'emergency_number': '911',
        'language': 'es',
        'use_eum': False,
        'use_sns': True
    }
}

# TRANSLATIONS
TRANSLATIONS = {
    'en': {
        'emergency_alert': 'EMERGENCY ALERT',
        'is_in_danger': 'is in DANGER!',
        'emergency_words_detected': 'Emergency words detected',
        'loud_noise_detected': 'Sudden loud noise detected',
        'live_tracking': 'LIVE TRACKING',
        'initial_location': 'Initial Location',
        'incident': 'Incident',
        'time': 'Time',
        'from': 'From',
        'system_test': 'SYSTEM TEST',
        'system_ready': 'System ready for',
        'emergency_detection_operational': 'Emergency detection operational'
    },
    'es': {
        'emergency_alert': 'ALERTA DE EMERGENCIA',
        'is_in_danger': 'está en PELIGRO!',
        'emergency_words_detected': 'Palabras de emergencia detectadas',
        'loud_noise_detected': 'Ruido fuerte repentino detectado',
        'live_tracking': 'RASTREO EN VIVO',
        'initial_location': 'Ubicación Inicial',
        'incident': 'Incidente',
        'time': 'Hora',
        'from': 'De',
        'system_test': 'PRUEBA DEL SISTEMA',
        'system_ready': 'Sistema listo para',
        'emergency_detection_operational': 'Detección de emergencias operativa'
    }
}

def get_eum_client():
    """Lazy-load AWS End User Messaging client (US only)"""
    global _eum_client
    if _eum_client is None:
        _eum_client = boto3.client('pinpoint-sms-voice-v2', region_name=REGION)
        logger.info("EUM client initialized (US only)")
    return _eum_client

def get_sns_client():
    """Lazy-load AWS SNS client (International)"""
    global _sns_client
    if _sns_client is None:
        _sns_client = boto3.client('sns', region_name=REGION)
        logger.info("SNS client initialized (International)")
    return _sns_client

def get_dynamodb():
    """Lazy-load DynamoDB resource"""
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.resource('dynamodb', region_name=REGION)
    return _dynamodb

def detect_country(phone_number):
    """Detect country from phone number"""
    phone = phone_number.strip().replace(' ', '').replace('-', '').replace('(', '').replace(')', '')
    
    # Ensure it starts with +
    if not phone.startswith('+'):
        phone = '+' + phone
    
    # Check each country code (longest first to avoid conflicts)
    for code in sorted(COUNTRY_CONFIG.keys(), key=len, reverse=True):
        if phone.startswith(code):
            logger.info(f"Detected country: {COUNTRY_CONFIG[code]['name']} ({code}) from {phone}")
            return code, COUNTRY_CONFIG[code]
    
    # Default to US
    logger.warning(f"Unknown country code for {phone}, defaulting to US")
    return '+1', COUNTRY_CONFIG['+1']

def normalize_phone_number(phone_number):
    """Normalize phone number to E.164 format"""
    phone = phone_number.strip().replace(' ', '').replace('-', '').replace('(', '').replace(')', '')
    
    if not phone.startswith('+'):
        phone = '+' + phone
    
    logger.info(f"Normalized phone: {phone_number} → {phone}")
    return phone

def get_translation(key, language='en'):
    """Get translated text"""
    return TRANSLATIONS.get(language, TRANSLATIONS['en']).get(key, key)

def handler(event, context):
    """
    AllSensesAI Emergency SMS Handler - Hybrid EUM/SNS
    US: AWS EUM (10DLC compliant)
    International: AWS SNS (works globally)
    """
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
        elif action == 'SIMULATE_EMERGENCY':
            # Real emergency workflow - maps parameters and uses hybrid routing
            return handle_simulate_emergency(body)
        elif action == 'JURY_TEST':
            return handle_jury_test(body)
        elif action == 'TEST_SMS':
            return test_sms_direct(body)
        elif action == 'UPDATE_LOCATION':
            return handle_update_location(body)
        elif action == 'GET_LOCATION':
            return handle_get_location(body)
        elif action == 'CHECK_EUM_CONFIG':
            return check_configuration()
        else:
            return cors_response({
                'status': 'success',
                'message': 'AllSensesAI Lambda operational - Hybrid SMS routing',
                'routing': 'US=EUM, International=SNS',
                'supportedCountries': ['USA', 'Colombia', 'Chile', 'Venezuela', 'Mexico'],
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

def handle_simulate_emergency(body):
    """Handle real emergency workflow - maps parameters to emergency alert handler"""
    # Map SIMULATE_EMERGENCY parameters to JURY_EMERGENCY_ALERT format
    mapped_body = {
        'victimName': body.get('victimName', body.get('userId', 'Unknown Person')),
        'phoneNumber': body.get('emergencyPhone', body.get('phoneNumber')),
        'detectionType': 'emergency_words',
        'detectionData': {
            'detectedWords': ['emergency', 'help']
        },
        'location': body.get('location', {})
    }
    
    logger.info(f"SIMULATE_EMERGENCY: Mapped emergencyPhone={body.get('emergencyPhone')} to phoneNumber={mapped_body['phoneNumber']}")
    
    # Use the same handler as JURY_EMERGENCY_ALERT
    return handle_jury_emergency_alert(mapped_body)

def handle_jury_emergency_alert(body):
    """Handle emergency alert with hybrid SMS routing"""
    try:
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber')
        detection_type = body.get('detectionType', 'emergency')
        detection_data = body.get('detectionData', {})
        location = body.get('location', {})
        
        if not emergency_phone:
            return cors_response({'status': 'error', 'message': 'phoneNumber required'}, 400)
        
        # Normalize and detect country
        emergency_phone = normalize_phone_number(emergency_phone)
        country_code, country_config = detect_country(emergency_phone)
        language = country_config['language']
        
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        
        logger.info(f"Emergency for {victim_name} in {country_config['name']}")
        logger.info(f"Phone: {emergency_phone}, Language: {language}")
        logger.info(f"Routing: {'EUM' if country_config['use_eum'] else 'SNS'}")
        
        # Store incident
        store_incident(incident_id, victim_name, emergency_phone, detection_type, location, country_code)
        
        # Store location
        if location.get('latitude') and location.get('longitude'):
            store_location_update(incident_id, victim_name, location)
        
        # Generate Google Maps link for live location tracking
        lat = location.get('latitude', 0)
        lon = location.get('longitude', 0)
        google_maps_url = f"https://www.google.com/maps?q={lat},{lon}"
        
        # Also keep tracking URL for reference
        tracking_url = f"{TRACKING_BASE_URL}?incident={incident_id}"
        
        # Compose message (localized)
        t = lambda key: get_translation(key, language)
        
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"{t('emergency_alert')}: {victim_name} {t('is_in_danger')}\n{t('emergency_words_detected')}: {', '.join(detected_words)}"
        elif detection_type == 'abrupt_noise':
            danger_message = f"{t('emergency_alert')}: {victim_name} {t('is_in_danger')}\n{t('loud_noise_detected')}"
        else:
            danger_message = f"{t('emergency_alert')}: {victim_name} {t('is_in_danger')}"
        
        place_name = location.get('placeName', 'Unknown Location')
        
        # Complete SMS message with Google Maps link
        sms_message = f"🚨 {danger_message}\n\n"
        sms_message += f"📍 {t('live_tracking')}:\n{google_maps_url}\n\n"
        sms_message += f"{t('initial_location')}: {place_name}\n\n"
        sms_message += f"{t('incident')}: {incident_id}\n"
        sms_message += f"{t('time')}: {datetime.now().strftime('%H:%M:%S')}\n\n"
        sms_message += f"{t('from')}: AllSensesAI Guardian"
        
        logger.info(f"Sending SMS to {emergency_phone} ({country_config['name']})")
        logger.info(f"Message length: {len(sms_message)} chars")
        
        # Send SMS (hybrid routing)
        sms_result = send_sms_hybrid(emergency_phone, sms_message, country_config)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'partial',
            'message': 'Emergency alert processed',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'country': country_config['name'],
            'countryCode': country_code,
            'language': language,
            'emergencyNumber': country_config['emergency_number'],
            'trackingUrl': tracking_url,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsMethod': sms_result.get('method'),
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

def send_sms_hybrid(phone_number, message, country_config):
    """
    Hybrid SMS sending:
    - US: Use AWS EUM (10DLC compliant)
    - International: Use AWS SNS (works globally)
    """
    try:
        if country_config['use_eum']:
            # US: Use EUM with 10DLC
            return send_via_eum(phone_number, message)
        else:
            # International: Use SNS
            return send_via_sns(phone_number, message, country_config)
    except Exception as e:
        logger.error(f"SMS sending failed: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e),
            'method': 'unknown'
        }

def send_via_eum(phone_number, message):
    """Send SMS via AWS End User Messaging (US only)"""
    try:
        logger.info(f"=== SENDING VIA EUM (US 10DLC) ===")
        logger.info(f"Destination: {phone_number}")
        logger.info(f"Originator: {ORIGINATOR_NUMBER_US}")
        
        eum_client = get_eum_client()
        
        response = eum_client.send_text_message(
            DestinationPhoneNumber=phone_number,
            OriginationIdentity=ORIGINATOR_NUMBER_US,
            MessageBody=message,
            MessageType=MESSAGE_TYPE,
            ConfigurationSetName=CONFIGURATION_SET
        )
        
        message_id = response.get('MessageId')
        logger.info(f"EUM SUCCESS: MessageId={message_id}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'method': 'EUM',
            'originator': ORIGINATOR_NUMBER_US
        }
        
    except Exception as e:
        logger.error(f"EUM FAILED: {str(e)}")
        return {
            'status': 'failed',
            'error': str(e),
            'method': 'EUM'
        }

def send_via_sns(phone_number, message, country_config):
    """Send SMS via AWS SNS (International)"""
    try:
        logger.info(f"=== SENDING VIA SNS (INTERNATIONAL) ===")
        logger.info(f"Destination: {phone_number} ({country_config['name']})")
        logger.info(f"Message length: {len(message)} chars")
        
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
        logger.info(f"SNS SUCCESS: MessageId={message_id}")
        logger.info(f"Country: {country_config['name']}")
        
        return {
            'status': 'sent',
            'messageId': message_id,
            'method': 'SNS',
            'country': country_config['name']
        }
        
    except Exception as e:
        logger.error(f"SNS FAILED: {str(e)}")
        logger.error(f"Phone: {phone_number}, Country: {country_config['name']}")
        return {
            'status': 'failed',
            'error': str(e),
            'method': 'SNS',
            'country': country_config['name']
        }

def handle_jury_test(body):
    """Handle test message"""
    try:
        test_phone = body.get('phoneNumber')
        victim_name = body.get('victimName', 'Test User')
        
        if not test_phone:
            return cors_response({'status': 'error', 'message': 'phoneNumber required'}, 400)
        
        test_phone = normalize_phone_number(test_phone)
        country_code, country_config = detect_country(test_phone)
        language = country_config['language']
        t = lambda key: get_translation(key, language)
        
        test_message = f"AllSensesAI {t('system_test')}\n\n"
        test_message += f"{t('system_ready')} {victim_name}!\n\n"
        test_message += f"{t('time')}: {datetime.now().strftime('%H:%M:%S')}\n\n"
        test_message += f"{t('emergency_detection_operational')}.\n\n"
        test_message += f"{t('from')}: AllSensesAI Guardian"
        
        logger.info(f"Test SMS to {test_phone} ({country_config['name']})")
        
        sms_result = send_sms_hybrid(test_phone, test_message, country_config)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'message': 'Test message processed',
            'victimName': victim_name,
            'testPhone': test_phone,
            'country': country_config['name'],
            'language': language,
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsMethod': sms_result.get('method'),
            'smsError': sms_result.get('error'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

def test_sms_direct(body):
    """Direct SMS test"""
    try:
        phone = body.get('phoneNumber')
        message = body.get('message', f'AllSensesAI Test. Time: {datetime.now().strftime("%H:%M:%S")}')
        
        if not phone:
            return cors_response({'status': 'error', 'message': 'phoneNumber required'}, 400)
        
        phone = normalize_phone_number(phone)
        country_code, country_config = detect_country(phone)
        
        logger.info(f"Direct test to {phone} ({country_config['name']})")
        
        sms_result = send_sms_hybrid(phone, message, country_config)
        
        return cors_response({
            'status': 'success' if sms_result['status'] == 'sent' else 'failed',
            'phone': phone,
            'country': country_config['name'],
            'smsMessageId': sms_result.get('messageId'),
            'smsStatus': sms_result['status'],
            'smsMethod': sms_result.get('method'),
            'smsError': sms_result.get('error'),
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Direct test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'errorType': type(e).__name__
        }, 500)

# Location tracking functions (simplified)
def handle_update_location(body):
    """Store location update"""
    try:
        incident_id = body.get('incidentId')
        if not incident_id:
            return cors_response({'status': 'error', 'message': 'incidentId required'}, 400)
        
        store_location_update(incident_id, body.get('victimName', 'Unknown'), body.get('location', {}), body.get('batteryLevel'))
        return cors_response({'status': 'success', 'incidentId': incident_id})
    except Exception as e:
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
        return cors_response({'status': 'error', 'message': 'No location found'}, 404)
    except Exception as e:
        return cors_response({'status': 'error', 'message': str(e)}, 500)

def store_incident(incident_id, victim_name, emergency_phone, detection_type, location, country_code):
    """Store incident"""
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
        logger.error(f"Store incident failed: {str(e)}")

def store_location_update(incident_id, victim_name, location, battery_level=None):
    """Store location"""
    try:
        dynamodb = get_dynamodb()
        table = dynamodb.Table(LOCATION_TABLE)
        item = {
            'incidentId': incident_id,
            'timestamp': int(datetime.now(timezone.utc).timestamp() * 1000),
            'victimName': victim_name,
            'latitude': float(location.get('latitude', 0)),
            'longitude': float(location.get('longitude', 0)),
            'accuracy': float(location.get('accuracy', 0)),
            'ttl': int((datetime.now(timezone.utc) + timedelta(hours=24)).timestamp())
        }
        if battery_level:
            item['batteryLevel'] = int(battery_level)
        table.put_item(Item=item)
    except Exception as e:
        logger.error(f"Store location failed: {str(e)}")

def check_configuration():
    """Check configuration"""
    return cors_response({
        'status': 'success',
        'message': 'Hybrid SMS routing active',
        'routing': {
            'USA': 'AWS EUM (10DLC)',
            'International': 'AWS SNS'
        },
        'supportedCountries': {
            code: {
                'name': config['name'],
                'method': 'EUM' if config['use_eum'] else 'SNS',
                'emergency_number': config['emergency_number'],
                'language': config['language']
            } for code, config in COUNTRY_CONFIG.items()
        }
    })

def cors_response(data, status_code=200):
    """CORS response"""
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
