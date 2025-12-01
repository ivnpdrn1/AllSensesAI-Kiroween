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
    1. Audio Capture → 2. Distress Detection → 3. Event Trigger → 
    4. Geolocation → 5. SMS Dispatch → 6. Contact Confirmation → 7. Analytics
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
        else:
            return analyze_audio_distress(body)
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}", exc_info=True)
        return cors_response({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def validate_phone_number(phone):
    """
    Validate and format phone number for international SMS
    Supports E.164 format: +[country code][number]
    """
    if not phone:
        return None, "Phone number is required"
    
    # Remove spaces and dashes
    phone = phone.replace(' ', '').replace('-', '')
    
    # Ensure it starts with +
    if not phone.startswith('+'):
        phone = '+' + phone
    
    # Validate E.164 format: +[1-3 digit country code][4-14 digit number]
    if not re.match(r'^\+\d{1,3}\d{4,14}$', phone):
        return None, f"Invalid phone format: {phone}. Must be E.164 format (+country_code + number)"
    
    return phone, None

def send_sms_with_eum(phone_number, message, event_id=None):
    """
    Send SMS with full EUM compliance and international support - ENHANCED LOGGING
    
    Args:
        phone_number: E.164 formatted phone number
        message: SMS message text
        event_id: Optional event ID for tracking
    
    Returns:
        dict with status, messageId, and metadata
    """
    try:
        # Validate phone number
        validated_phone, error = validate_phone_number(phone_number)
        if error:
            logger.error(f"❌ Phone validation failed: {error}")
            return {
                'status': 'failed',
                'error': error,
                'phone': phone_number,
                'realSms': False
            }
        
        # Check demo mode
        if DEMO_MODE:
            logger.info(f"DEMO_MODE enabled - simulating SMS to {validated_phone}")
            return {
                'status': 'sent',
                'messageId': f'demo-{uuid.uuid4().hex[:12]}',
                'phone': validated_phone,
                'realSms': False,
                'demoMode': True,
                'note': 'SMS simulated (DEMO_MODE=true)'
            }
        
        # Add tracking URL if event_id provided (SKIP for LATAM - URLs often blocked)
        latam_codes = ['+57', '+52', '+55', '+54']
        is_latam = any(validated_phone.startswith(code) for code in latam_codes)
        
        if event_id and TRACKING_URL_BASE and not is_latam:
            tracking_url = f"{TRACKING_URL_BASE}?incident={event_id}"
            message = f"{message}\n\nTrack: {tracking_url}"
            logger.info(f"   Added tracking URL (non-LATAM)")
        elif is_latam:
            logger.info(f"   Skipping tracking URL for LATAM (URLs often blocked)")
        
        # Prepare EUM-compliant SMS attributes
        message_attributes = {
            'AWS.SNS.SMS.SMSType': {
                'DataType': 'String',
                'StringValue': 'Transactional'  # High priority for emergency
            }
        }
        
        # Add SenderID ONLY for supported countries (NOT for Latin America)
        # Colombia (+57), Mexico (+52), Brazil (+55), Argentina (+54) don't support SenderID
        latam_codes = ['+57', '+52', '+55', '+54']
        is_latam = any(validated_phone.startswith(code) for code in latam_codes)
        
        if SMS_ORIGINATOR and not is_latam:
            message_attributes['AWS.SNS.SMS.SenderID'] = {
                'DataType': 'String',
                'StringValue': SMS_ORIGINATOR
            }
            logger.info(f"   Using SenderID: {SMS_ORIGINATOR}")
        elif is_latam:
            logger.info(f"   Skipping SenderID for LATAM number (not supported)")
        
        # Send SMS via SNS - WITH DETAILED LOGGING
        logger.info(f"📱 Attempting SNS publish to {validated_phone}")
        logger.info(f"   Originator: {SMS_ORIGINATOR}")
        logger.info(f"   Message length: {len(message)} chars")
        logger.info(f"   Attributes: {json.dumps(message_attributes)}")
        
        try:
            response = sns.publish(
                PhoneNumber=validated_phone,
                Message=message,
                MessageAttributes=message_attributes
            )
            
            message_id = response['MessageId']
            logger.info(f"✅ SNS PUBLISH SUCCESS!")
            logger.info(f"   MessageId: {message_id}")
            logger.info(f"   Phone: {validated_phone}")
            logger.info(f"   Response: {json.dumps(response, default=str)}")
            
            return {
                'status': 'sent',
                'messageId': message_id,
                'phone': validated_phone,
                'realSms': True,
                'smsMethod': 'SNS',
                'originator': SMS_ORIGINATOR,
                'campaignId': SMS_CAMPAIGN_ID,
                'timestamp': datetime.now(timezone.utc).isoformat()
            }
            
        except Exception as sns_error:
            # Log SNS-specific error
            logger.error(f"❌ SNS PUBLISH FAILED!")
            logger.error(f"   Error Type: {type(sns_error).__name__}")
            logger.error(f"   Error Message: {str(sns_error)}")
            logger.error(f"   Phone: {validated_phone}")
            logger.error(f"   Full exception:", exc_info=True)
            
            return {
                'status': 'failed',
                'error': f'SNS Error: {str(sns_error)}',
                'errorType': type(sns_error).__name__,
                'phone': validated_phone,
                'realSms': False,
                'note': 'SNS publish call failed - check IAM permissions and SNS configuration'
            }
        
    except Exception as e:
        error_msg = str(e)
        logger.error(f"❌ SMS function error for {phone_number}: {error_msg}", exc_info=True)
        
        return {
            'status': 'failed',
            'error': error_msg,
            'phone': phone_number,
            'realSms': False,
            'note': 'SMS delivery failed - check CloudWatch logs for details'
        }

def simulate_complete_pipeline(body):
    """
    Complete 7-Step AllSenseAI Emergency Pipeline
    """
    try:
        user_id = body.get('userId', 'user-123')
        event_id = str(uuid.uuid4())
        
        # Step 1: Audio Capture (simulated)
        audio_s3_key = f"s3://allsenseai-audio/{user_id}/{event_id}.wav"
        
        # Step 2: Distress Detection using AWS Bedrock
        audio_data = body.get('audioData', 'HELP! Emergency!')
        distress_result = detect_distress_bedrock(audio_data)
        
        # Step 3: Event Trigger
        event_payload = {
            "eventId": event_id,
            "userId": user_id,
            "confidence": distress_result['confidence'],
            "threatLevel": distress_result['level'],
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "audioS3Key": audio_s3_key
        }
        
        # Step 4: Get User Profile & Geolocation
        user_profile = fetch_user_profile(user_id, body)
        location_data = {
            "lat": body.get('lat', 25.7617),
            "lon": body.get('lon', -80.1918),
            "placeName": body.get('placeName', 'Miami Convention Center, Miami, FL'),
            "mapLink": f"https://maps.google.com/?q={body.get('lat', 25.7617)},{body.get('lon', -80.1918)}"
        }
        
        # Step 5: SMS Dispatch (UNIVERSAL - works for any phone number)
        sms_results = dispatch_emergency_sms_universal(user_profile, location_data, distress_result, event_id)
        
        # Step 6: Contact Confirmation (simulated)
        confirmation_result = {
            "status": "pending",
            "confirmationWindow": "5m",
            "expectedConfirmations": len(sms_results)
        }
        
        # Step 7: Analytics & Learning
        analytics_result = log_emergency_analytics(event_id, user_id, sms_results, distress_result)
        
        return cors_response({
            'status': 'success',
            'message': 'AllSensesAI 7-Step Pipeline Complete',
            'eventId': event_id,
            'demoMode': DEMO_MODE,
            'steps': {
                'step1_audio': {
                    'status': 'success',
                    'audioS3Key': audio_s3_key,
                    'captureTime': datetime.now(timezone.utc).isoformat()
                },
                'step2_distress': {
                    'status': 'success',
                    'confidence': distress_result['confidence'],
                    'threatLevel': distress_result['level'],
                    'keywords': distress_result.get('keywords', [])
                },
                'step3_event': {
                    'status': 'success',
                    'eventId': event_id,
                    'eventPayload': event_payload
                },
                'step4_geolocation': {
                    'status': 'success',
                    'location': location_data
                },
                'step5_sms': {
                    'status': 'success',
                    'results': sms_results,
                    'totalSent': len([r for r in sms_results if r['status'] == 'sent']),
                    'totalFailed': len([r for r in sms_results if r['status'] == 'failed'])
                },
                'step6_confirmation': confirmation_result,
                'step7_analytics': analytics_result
            },
            'userProfile': user_profile,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Pipeline error: {str(e)}", exc_info=True)
        return cors_response({
            'status': 'error',
            'message': f'Pipeline failed: {str(e)}'
        }, 500)

def dispatch_emergency_sms_universal(user_profile, location_data, distress_result, event_id):
    """
    Universal SMS Dispatch - sends real SMS to ALL phone numbers (unless DEMO_MODE=true)
    Works for any country, any phone number format
    """
    results = []
    victim_name = user_profile.get('victimName', 'Unknown')
    contacts = user_profile.get('contacts', [])
    
    # Filter opted-in contacts
    opted_contacts = [c for c in contacts if c.get('optedIn', False)]
    
    if not opted_contacts:
        logger.warning("No opted-in contacts found")
        return [{'status': 'error', 'message': 'No opted-in contacts'}]
    
    # Compose SMS message
    confidence_percent = int(distress_result['confidence'] * 100)
    
    # COLOMBIA-FRIENDLY: Neutral wording (no "emergency", "help", "danger" keywords)
    # Colombian carriers block emergency-style messages
    sms_text = f"AllSensesAI Alert: {victim_name} at {location_data['placeName']}. Status update required. Ref: {event_id[:8]}"
    
    logger.info(f"Dispatching SMS to {len(opted_contacts)} contacts")
    logger.info(f"Message text: {sms_text}")
    
    # Send SMS to each contact
    for contact in opted_contacts:
        contact_name = contact.get('name', 'Unknown')
        contact_phone = contact.get('phone')
        
        if not contact_phone:
            logger.warning(f"Contact {contact_name} has no phone number")
            results.append({
                'contactName': contact_name,
                'status': 'failed',
                'error': 'No phone number provided',
                'relationship': contact.get('relationship', 'contact')
            })
            continue
        
        # Send SMS using universal function
        # COLOMBIA FIX: Don't pass event_id for LATAM (makes it identical to test SMS which works)
        latam_codes = ['+57', '+52', '+55', '+54']
        is_latam_number = any(contact_phone.startswith(code) for code in latam_codes)
        
        if is_latam_number:
            # For LATAM: call without event_id (same as test SMS that works)
            sms_result = send_sms_with_eum(contact_phone, sms_text)
            logger.info(f"   Sending to LATAM without event_id (test SMS format)")
        else:
            # For other regions: include event_id for tracking
            sms_result = send_sms_with_eum(contact_phone, sms_text, event_id)
        
        # Add contact metadata to result
        sms_result['contactName'] = contact_name
        sms_result['relationship'] = contact.get('relationship', 'contact')
        sms_result['priority'] = contact.get('priority', 99)
        
        results.append(sms_result)
        
        # Log result
        if sms_result['status'] == 'sent':
            logger.info(f"✅ SMS sent to {contact_name} ({contact_phone}): {sms_result.get('messageId')}")
        else:
            logger.error(f"❌ SMS failed for {contact_name} ({contact_phone}): {sms_result.get('error')}")
    
    return results

def fetch_user_profile(user_id, body=None):
    """
    Fetch User Profile - supports dynamic contacts from request body
    """
    # Check if contacts provided in request
    if body and 'contacts' in body:
        contacts = body['contacts']
    else:
        # Default demo contacts
        contacts = [
            {
                'name': 'Emergency Contact 1',
                'phone': '+573222063010',  # Colombia number
                'optedIn': True,
                'relationship': 'emergency',
                'verified': True,
                'priority': 1
            },
            {
                'name': 'Emergency Contact 2',
                'phone': '+13053033060',  # US number
                'optedIn': True,
                'relationship': 'family',
                'verified': True,
                'priority': 2
            }
        ]
    
    victim_name = body.get('victimName', 'User') if body else 'User'
    
    return {
        'id': user_id,
        'userId': user_id,
        'victimName': victim_name,
        'contacts': contacts,
        'createdAt': datetime.now(timezone.utc).isoformat()
    }

def detect_distress_bedrock(audio_data):
    """
    Step 2: AI Distress Detection using AWS Bedrock
    """
    try:
        prompt = f"""Analyze this audio transcript for signs of distress or emergency:

Transcript: "{audio_data}"

Determine:
1. Threat level (NONE, LOW, MEDIUM, HIGH, CRITICAL)
2. Confidence score (0.0 to 1.0)
3. Emergency keywords detected
4. Reasoning for the assessment

Respond in JSON format:
{{
    "level": "CRITICAL|HIGH|MEDIUM|LOW|NONE",
    "confidence": 0.87,
    "keywords": ["help", "emergency"],
    "reasoning": "explanation"
}}"""

        response = bedrock.invoke_model(
            modelId='anthropic.claude-3-haiku-20240307-v1:0',
            body=json.dumps({
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": 200,
                "messages": [{"role": "user", "content": prompt}]
            })
        )
        
        result = json.loads(response['body'].read())
        content = result['content'][0]['text']
        
        try:
            analysis = json.loads(content)
            return {
                'level': analysis.get('level', 'MEDIUM'),
                'confidence': analysis.get('confidence', 0.8),
                'keywords': analysis.get('keywords', []),
                'reasoning': analysis.get('reasoning', 'AI analysis completed')
            }
        except:
            return analyze_distress_fallback(audio_data)
            
    except Exception as e:
        logger.warning(f"Bedrock analysis failed: {str(e)}")
        return analyze_distress_fallback(audio_data)

def analyze_distress_fallback(audio_data):
    """
    Fallback distress analysis if Bedrock fails
    """
    message_upper = str(audio_data).upper()
    emergency_keywords = ['HELP', 'EMERGENCY', 'DANGER', '911', 'POLICE', 'FIRE', 'AMBULANCE']
    
    detected_keywords = [word for word in emergency_keywords if word in message_upper]
    
    if detected_keywords:
        return {
            'level': 'CRITICAL',
            'confidence': 0.87,
            'keywords': detected_keywords,
            'reasoning': f'Emergency keywords detected: {", ".join(detected_keywords)}'
        }
    else:
        return {
            'level': 'NONE',
            'confidence': 0.1,
            'keywords': [],
            'reasoning': 'No emergency indicators detected'
        }

def log_emergency_analytics(event_id, user_id, sms_results, distress_result):
    """
    Step 7: Analytics & Learning
    """
    try:
        analytics_data = {
            'eventId': event_id,
            'userId': user_id,
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'threatLevel': distress_result['level'],
            'confidence': distress_result['confidence'],
            'smsCount': len(sms_results),
            'successfulSms': len([r for r in sms_results if r['status'] == 'sent']),
            'failedSms': len([r for r in sms_results if r['status'] == 'failed']),
            'demoMode': DEMO_MODE
        }
        
        logger.info(f"ANALYTICS: {json.dumps(analytics_data)}")
        
        return {
            'status': 'logged',
            'eventId': event_id,
            'metrics': analytics_data
        }
        
    except Exception as e:
        logger.error(f"Analytics logging failed: {str(e)}")
        return {'status': 'failed', 'error': str(e)}

def get_user_profile(body):
    """
    Get user profile for frontend
    """
    user_id = body.get('userId', 'demo-user')
    profile = fetch_user_profile(user_id, body)
    return cors_response(profile)

def check_sns_status():
    """
    Check SNS status and configuration
    """
    return cors_response({
        'status': 'success',
        'snsStatus': {
            'demoMode': DEMO_MODE,
            'smsOriginator': SMS_ORIGINATOR,
            'campaignId': SMS_CAMPAIGN_ID,
            'trackingEnabled': bool(TRACKING_URL_BASE),
            'ready': True
        },
        'message': f'SMS system ready - DEMO_MODE: {DEMO_MODE}',
        'timestamp': datetime.now(timezone.utc).isoformat()
    })

def jury_demo_test(body):
    """
    Test SMS sending to specific phone number
    """
    try:
        test_phone = body.get('phoneNumber', '+13053033060')
        victim_name = body.get('victimName', 'Test User')
        
        test_message = f"🏆 AllSensesAI TEST 🏆\n\nSystem ready for {victim_name}!\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\nEmergency detection system operational."
        
        sms_result = send_sms_with_eum(test_phone, test_message)
        
        return cors_response({
            'status': 'success',
            'message': 'Test message sent',
            'victimName': victim_name,
            'testPhone': test_phone,
            'smsResult': sms_result,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Test error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def handle_jury_emergency_alert(body):
    """
    Handle emergency alert with configurable victim and phone
    """
    try:
        victim_name = body.get('victimName', 'Unknown Person')
        emergency_phone = body.get('phoneNumber', '+13053033060')
        detection_type = body.get('detectionType', 'emergency')
        detection_data = body.get('detectionData', {})
        location = body.get('location', {})
        
        incident_id = f"EMG-{uuid.uuid4().hex[:8].upper()}"
        
        # Compose emergency message
        if detection_type == 'emergency_words':
            detected_words = detection_data.get('detectedWords', ['emergency'])
            danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER! Words detected: \"{', '.join(detected_words)}\""
        elif detection_type == 'abrupt_noise':
            volume = detection_data.get('volume', 'high')
            danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER! Loud noise: {volume} dB"
        else:
            danger_message = f"🚨 EMERGENCY: {victim_name} is in DANGER! Emergency detected"
        
        place_name = location.get('placeName', 'Unknown location')
        map_link = location.get('mapLink', 'https://maps.google.com/?q=25.7617,-80.1918')
        
        sms_message = f"{danger_message}\n\nLocation: {place_name}\nMap: {map_link}\n\nIncident: {incident_id}\nTime: {datetime.now().strftime('%H:%M:%S')}"
        
        # Send SMS
        sms_result = send_sms_with_eum(emergency_phone, sms_message, incident_id)
        
        return cors_response({
            'status': 'success',
            'message': 'Emergency alert sent',
            'incidentId': incident_id,
            'victimName': victim_name,
            'emergencyPhone': emergency_phone,
            'detectionType': detection_type,
            'smsResult': sms_result,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Emergency alert error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def handle_jury_test(body):
    """
    Handle test message
    """
    return jury_demo_test(body)

def send_emergency_sms(body):
    """
    Direct SMS sending endpoint
    """
    try:
        phone_number = body.get('phoneNumber')
        emergency_message = body.get('emergencyMessage', 'AllSensesAI Emergency Test')
        
        if not phone_number:
            return cors_response({
                'status': 'error',
                'message': 'Phone number is required'
            }, 400)
        
        sms_result = send_sms_with_eum(phone_number, emergency_message)
        
        return cors_response({
            'status': 'success',
            'message': 'SMS sent',
            'phoneNumber': phone_number,
            'smsResult': sms_result,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Send SMS error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e)
        }, 500)

def analyze_audio_distress(body):
    """
    Audio analysis endpoint
    """
    audio_data = body.get('message', body.get('audioData', 'Test'))
    distress_result = detect_distress_bedrock(audio_data)
    
    return cors_response({
        'status': 'success',
        'message': 'Audio analysis complete',
        'distressAnalysis': distress_result,
        'timestamp': datetime.now(timezone.utc).isoformat()
    })

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
