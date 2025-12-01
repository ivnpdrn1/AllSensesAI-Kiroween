import json
import boto3
import uuid
import os
from datetime import datetime, timezone
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize AWS services
sns = boto3.client('sns')
dynamodb = boto3.resource('dynamodb')
bedrock = boto3.client('bedrock-runtime')

def handler(event, context):
    """
    AllSensesAI Complete 7-Step Pipeline - HYBRID VERSION
    Real SMS for verified numbers, simulation for others
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
        
        action = body.get('action', 'SIMULATE_EMERGENCY')
        
        # Route to appropriate handler
        if action == 'SIMULATE_EMERGENCY':
            return simulate_complete_pipeline(body)
        elif action == 'GET_USER_PROFILE':
            return get_user_profile(body)
        elif action == 'MAKE_REAL_CALL':
            return send_emergency_sms_hybrid(body)
        elif action == 'CHECK_SNS_STATUS':
            return check_sns_status_hybrid()
        elif action == 'VERIFY_PHONE':
            return verify_phone_number(body)
        else:
            return analyze_audio_distress(body)
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def check_sns_status_hybrid():
    """
    Check SNS status and verified numbers
    """
    try:
        # Check sandbox status
        sandbox_response = sns.get_sms_sandbox_account_status()
        is_in_sandbox = sandbox_response.get('IsInSandbox', True)
        
        # Get verified numbers
        verified_numbers = []
        try:
            verified_response = sns.list_sms_sandbox_phone_numbers()
            verified_numbers = [num['PhoneNumber'] for num in verified_response.get('PhoneNumbers', [])]
        except Exception as e:
            logger.warning(f"Could not get verified numbers: {str(e)}")
        
        return cors_response({
            'status': 'success',
            'snsStatus': {
                'isInSandbox': is_in_sandbox,
                'mode': 'HYBRID_REAL_SMS',
                'verifiedNumbers': verified_numbers,
                'canSendReal': len(verified_numbers) > 0,
                'totalVerified': len(verified_numbers)
            },
            'message': f'Hybrid mode - {len(verified_numbers)} verified numbers for real SMS',
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"SNS status check failed: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Could not check SNS status: {str(e)}'
        }, 500)

def verify_phone_number(body):
    """
    Start phone number verification process
    """
    try:
        phone_number = body.get('phoneNumber')
        verification_code = body.get('verificationCode')
        
        if not phone_number:
            return cors_response({
                'status': 'error',
                'message': 'Phone number required'
            }, 400)
        
        if not verification_code:
            # Start verification
            try:
                sns.create_sms_sandbox_phone_number(PhoneNumber=phone_number)
                return cors_response({
                    'status': 'verification_sent',
                    'message': f'Verification code sent to {phone_number}',
                    'phoneNumber': phone_number
                })
            except Exception as e:
                return cors_response({
                    'status': 'error',
                    'message': f'Failed to send verification: {str(e)}'
                }, 500)
        else:
            # Complete verification
            try:
                sns.verify_sms_sandbox_phone_number(
                    PhoneNumber=phone_number,
                    OneTimePassword=verification_code
                )
                
                # Test SMS immediately
                test_message = f"🚨 AllSensesAI - Phone verified! Ready for emergency alerts. Time: {datetime.now().strftime('%H:%M:%S')}"
                test_result = sns.publish(
                    PhoneNumber=phone_number,
                    Message=test_message
                )
                
                return cors_response({
                    'status': 'verified',
                    'message': f'Phone {phone_number} verified and test SMS sent',
                    'phoneNumber': phone_number,
                    'testMessageId': test_result['MessageId']
                })
                
            except Exception as e:
                return cors_response({
                    'status': 'error',
                    'message': f'Verification failed: {str(e)}'
                }, 500)
                
    except Exception as e:
        return cors_response({
            'status': 'error',
            'message': f'Verification process failed: {str(e)}'
        }, 500)

def simulate_complete_pipeline(body):
    """
    Complete 7-Step AllSenseAI Emergency Pipeline - HYBRID VERSION
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
        user_profile = fetch_user_profile_hybrid(user_id)
        location_data = {
            "lat": body.get('lat', 40.712776),
            "lon": body.get('lon', -74.005974),
            "placeName": "123 Main St, New York, NY",
            "mapLink": f"https://maps.google.com/?q={body.get('lat', 40.712776)},{body.get('lon', -74.005974)}"
        }
        
        # Step 5: SMS Dispatch (HYBRID - real + simulated)
        sms_results = dispatch_emergency_sms_hybrid(user_profile, location_data, distress_result, event_id)
        
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
            'message': 'AllSensesAI 7-Step Pipeline Complete (HYBRID MODE)',
            'eventId': event_id,
            'hybridMode': True,
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
                    'realSms': len([r for r in sms_results if r.get('realSms', False)]),
                    'simulatedSms': len([r for r in sms_results if not r.get('realSms', False)])
                },
                'step6_confirmation': confirmation_result,
                'step7_analytics': analytics_result
            },
            'userProfile': user_profile,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Pipeline error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Pipeline failed: {str(e)}'
        }, 500)

def detect_distress_bedrock(audio_data):
    """
    Step 2: AI Distress Detection using AWS Bedrock
    """
    try:
        # Prepare prompt for Claude
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

        # Call Bedrock Claude model
        response = bedrock.invoke_model(
            modelId='anthropic.claude-3-haiku-20240307-v1:0',
            body=json.dumps({
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": 200,
                "messages": [
                    {
                        "role": "user",
                        "content": prompt
                    }
                ]
            })
        )
        
        result = json.loads(response['body'].read())
        content = result['content'][0]['text']
        
        # Parse JSON response
        try:
            analysis = json.loads(content)
            return {
                'level': analysis.get('level', 'MEDIUM'),
                'confidence': analysis.get('confidence', 0.8),
                'keywords': analysis.get('keywords', []),
                'reasoning': analysis.get('reasoning', 'AI analysis completed')
            }
        except:
            # Fallback analysis
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

def fetch_user_profile_hybrid(user_id):
    """
    Step 4: Hybrid User Profile - includes verified numbers
    """
    # Get verified numbers from SNS
    verified_numbers = []
    try:
        verified_response = sns.list_sms_sandbox_phone_numbers()
        verified_numbers = [num['PhoneNumber'] for num in verified_response.get('PhoneNumbers', [])]
    except Exception as e:
        logger.warning(f"Could not get verified numbers: {str(e)}")
    
    # Create user profile with mix of verified and demo numbers
    demo_profile = {
        'id': user_id,
        'userId': user_id,
        'victimName': 'Carlos Perez',
        'contacts': [],
        'createdAt': datetime.now(timezone.utc).isoformat(),
        'hybridMode': True
    }
    
    # Add verified numbers as real contacts
    for i, verified_num in enumerate(verified_numbers):
        demo_profile['contacts'].append({
            'name': f'Verified Contact {i+1}',
            'phone': verified_num,
            'optedIn': True,
            'relationship': 'verified',
            'verified': True,
            'realSms': True
        })
    
    # Add demo contacts for simulation
    demo_contacts = [
        {
            'name': 'Maria Perez',
            'phone': '+15551112222',
            'optedIn': True,
            'relationship': 'spouse',
            'verified': False,
            'realSms': False
        },
        {
            'name': 'Jose Rodriguez',
            'phone': '+15553334444',
            'optedIn': True,
            'relationship': 'friend',
            'verified': False,
            'realSms': False
        }
    ]
    
    demo_profile['contacts'].extend(demo_contacts)
    
    return demo_profile

def dispatch_emergency_sms_hybrid(user_profile, location_data, distress_result, event_id):
    """
    Step 5: HYBRID SMS Dispatch - Real SMS for verified, simulation for others
    """
    results = []
    victim_name = user_profile.get('victimName', 'Unknown')
    contacts = user_profile.get('contacts', [])
    
    # Filter opted-in contacts
    opted_contacts = [c for c in contacts if c.get('optedIn', False)]
    
    if not opted_contacts:
        return [{'status': 'error', 'message': 'No opted-in contacts'}]
    
    # Compose SMS message
    confidence_percent = int(distress_result['confidence'] * 100)
    
    sms_text = f"🚨 [AllSensesAI] Emergency alert for {victim_name}! Possible danger detected near {location_data['placeName']} — {location_data['mapLink']} Confidence: {confidence_percent}%. Please reply OK if received."
    
    # Send SMS to each contact (real or simulated)
    for contact in opted_contacts:
        if contact.get('realSms', False) and contact.get('verified', False):
            # Send REAL SMS to verified numbers
            try:
                response = sns.publish(
                    PhoneNumber=contact['phone'],
                    Message=sms_text
                )
                
                results.append({
                    'contactName': contact['name'],
                    'phone': contact['phone'],
                    'status': 'sent',
                    'messageId': response['MessageId'],
                    'relationship': contact.get('relationship', 'contact'),
                    'realSms': True,
                    'verified': True,
                    'timestamp': datetime.now(timezone.utc).isoformat()
                })
                
                logger.info(f"REAL SMS sent to {contact['name']}: {response['MessageId']}")
                
            except Exception as e:
                results.append({
                    'contactName': contact['name'],
                    'phone': contact['phone'],
                    'status': 'failed',
                    'error': str(e),
                    'realSms': True
                })
                logger.error(f"Real SMS failed for {contact['name']}: {str(e)}")
        else:
            # SIMULATE SMS for unverified numbers
            demo_message_id = f"demo-msg-{uuid.uuid4().hex[:8]}"
            
            results.append({
                'contactName': contact['name'],
                'phone': contact['phone'],
                'status': 'sent',
                'messageId': demo_message_id,
                'relationship': contact.get('relationship', 'contact'),
                'realSms': False,
                'verified': False,
                'demoNote': 'SMS simulated - would be sent in production',
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
            
            logger.info(f"SIMULATED SMS to {contact['name']}: {demo_message_id}")
    
    return results

def send_emergency_sms_hybrid(body):
    """
    Direct SMS sending - HYBRID VERSION
    """
    try:
        phone_number = body.get('phoneNumber', '+15551234567')
        emergency_message = body.get('emergencyMessage', 'AllSenseAI Demo Test')
        
        # Check if number is verified
        verified_numbers = []
        try:
            verified_response = sns.list_sms_sandbox_phone_numbers()
            verified_numbers = [num['PhoneNumber'] for num in verified_response.get('PhoneNumbers', [])]
        except Exception as e:
            logger.warning(f"Could not check verified numbers: {str(e)}")
        
        if phone_number in verified_numbers:
            # Send REAL SMS
            try:
                response = sns.publish(
                    PhoneNumber=phone_number,
                    Message=f"🚨 ALLSENSESAI REAL SMS 🚨\n\n{emergency_message}\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\n- AllSensesAI Guardian"
                )
                
                return cors_response({
                    'status': 'success',
                    'message': 'REAL SMS sent successfully',
                    'phoneNumber': phone_number,
                    'smsMessageId': response['MessageId'],
                    'realSms': True,
                    'verified': True,
                    'timestamp': datetime.now(timezone.utc).isoformat()
                })
                
            except Exception as e:
                return cors_response({
                    'status': 'error',
                    'message': f'Real SMS failed: {str(e)}'
                }, 500)
        else:
            # SIMULATE SMS
            demo_message_id = f"demo-msg-{uuid.uuid4().hex[:8]}"
            
            return cors_response({
                'status': 'success',
                'message': 'Demo SMS simulated (number not verified)',
                'phoneNumber': phone_number,
                'smsMessageId': demo_message_id,
                'realSms': False,
                'verified': False,
                'demoNote': 'SMS simulated - verify number for real SMS',
                'actualMessage': f"🚨 ALLSENSESAI DEMO 🚨\n\n{emergency_message}\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\n- AllSensesAI Guardian",
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
        
    except Exception as e:
        return cors_response({
            'status': 'error',
            'message': f'SMS failed: {str(e)}'
        }, 500)

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
            'realSms': len([r for r in sms_results if r.get('realSms', False)]),
            'simulatedSms': len([r for r in sms_results if not r.get('realSms', False)]),
            'hybridMode': True
        }
        
        # Log to CloudWatch
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
    user_id = body.get('userId', 'user-123')
    profile = fetch_user_profile_hybrid(user_id)
    return cors_response(profile)

def analyze_audio_distress(body):
    """
    Audio analysis endpoint
    """
    audio_data = body.get('message', body.get('audioData', 'Test'))
    distress_result = detect_distress_bedrock(audio_data)
    
    return cors_response({
        'status': 'success',
        'message': 'AllSensesAI Audio Analysis Complete',
        'distressAnalysis': distress_result,
        'hybridMode': True,
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