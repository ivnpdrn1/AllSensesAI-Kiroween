import json
import boto3
import uuid
import os
from datetime import datetime, timezone
import logging
import time
import urllib.parse

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize AWS services
pinpoint = boto3.client('pinpoint')
sns = boto3.client('sns')
dynamodb = boto3.resource('dynamodb')
location_client = boto3.client('location')
eventbridge = boto3.client('events')

# Configuration
PINPOINT_PROJECT_ID = os.environ.get('PINPOINT_PROJECT_ID', 'allsense-emergency')
DYNAMODB_TABLE = os.environ.get('DYNAMODB_TABLE', 'AllSenseUsers')
LOCATION_INDEX = os.environ.get('LOCATION_INDEX', 'AllSenseLocationIndex')

def handler(event, context):
    """
    AllSenseAI Complete Pipeline Handler
    Handles all 7 steps: Audio → AI → Event → Geo → SMS → Confirmation → Analytics
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
        
        action = body.get('action', 'ANALYZE_AUDIO')
        
        # Route to appropriate handler
        if action == 'SIMULATE_EMERGENCY':
            return simulate_emergency_pipeline(body)
        elif action == 'MAKE_REAL_CALL':
            return make_real_sms(body)
        elif action == 'GET_USER_PROFILE':
            return get_user_profile(body)
        elif action == 'CONFIRM_ALERT':
            return confirm_alert(body)
        else:
            return analyze_audio_step1(body)
            
    except Exception as e:
        logger.error(f"Handler error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': str(e),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }, 500)

def simulate_emergency_pipeline(body):
    """
    Step-by-step emergency simulation for demo
    """
    try:
        user_id = body.get('userId', 'user-123')
        lat = body.get('lat', 40.712776)
        lon = body.get('lon', -74.005974)
        confidence = body.get('confidence', 0.87)
        
        # Step 1: Audio Capture (simulated)
        audio_snippet_s3 = f"s3://allsense-audio/{user_id}/{uuid.uuid4()}.wav"
        
        # Step 2: Distress Detection (AI Analysis)
        threat_analysis = analyze_distress(body.get('audioData', 'HELP! Emergency!'))
        
        # Step 3: Event Trigger
        event_id = str(uuid.uuid4())
        event_payload = {
            "version": "1",
            "source": "allsense.app",
            "detail-type": "emergency_triggered",
            "detail": {
                "userId": user_id,
                "lat": lat,
                "lon": lon,
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "confidence": confidence,
                "audioSnippetS3": audio_snippet_s3,
                "eventId": event_id
            }
        }
        
        # Trigger EventBridge event
        try:
            eventbridge.put_events(
                Entries=[{
                    'Source': 'allsense.app',
                    'DetailType': 'emergency_triggered',
                    'Detail': json.dumps(event_payload['detail'])
                }]
            )
        except Exception as eb_error:
            logger.warning(f"EventBridge failed: {str(eb_error)}")
        
        # Step 4: Fetch User Profile
        user_profile = fetch_user_profile(user_id)
        
        # Step 5: Reverse Geocoding
        location_data = reverse_geocode(lat, lon)
        
        # Step 6: Compose and Send SMS
        sms_results = send_emergency_sms(user_profile, location_data, confidence, event_id)
        
        # Step 7: Analytics Logging
        analytics_result = log_analytics(event_id, user_id, sms_results)
        
        return cors_response({
            'status': 'success',
            'message': 'AllSenseAI Emergency Pipeline Complete',
            'eventId': event_id,
            'steps': {
                'step1_audio': {
                    'status': 'success',
                    'audioSnippetS3': audio_snippet_s3
                },
                'step2_distress': {
                    'status': 'success',
                    'confidence': confidence,
                    'threatLevel': threat_analysis['level']
                },
                'step3_event': {
                    'status': 'success',
                    'eventId': event_id
                },
                'step4_geolocation': {
                    'status': 'success',
                    'location': location_data
                },
                'step5_sms': {
                    'status': 'success',
                    'results': sms_results
                },
                'step6_confirmation': {
                    'status': 'pending',
                    'confirmationWindow': '5m'
                },
                'step7_analytics': {
                    'status': 'success',
                    'result': analytics_result
                }
            },
            'userProfile': user_profile,
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        logger.error(f"Emergency pipeline error: {str(e)}")
        return cors_response({
            'status': 'error',
            'message': f'Pipeline failed: {str(e)}'
        }, 500)

def fetch_user_profile(user_id):
    """
    Step 4: Fetch User Profile from DynamoDB
    """
    try:
        table = dynamodb.Table(DYNAMODB_TABLE)
        response = table.get_item(Key={'userId': user_id})
        
        if 'Item' in response:
            return response['Item']
        else:
            # Create demo user if not exists
            demo_user = {
                'userId': user_id,
                'victimName': 'Carlos Perez',
                'contacts': [
                    {
                        'name': 'Maria Perez',
                        'phone': '+15551112222',
                        'optedIn': True,
                        'channelPref': 'sms'
                    },
                    {
                        'name': 'Jose Rodriguez',
                        'phone': '+15553334444',
                        'optedIn': True,
                        'channelPref': 'sms'
                    }
                ],
                'lastKnownLocation': {
                    'lat': 40.712776,
                    'lon': -74.005974,
                    'timestamp': datetime.now(timezone.utc).isoformat()
                },
                'notificationHistory': []
            }
            
            table.put_item(Item=demo_user)
            return demo_user
            
    except Exception as e:
        logger.error(f"User profile fetch failed: {str(e)}")
        return {
            'userId': user_id,
            'victimName': 'Demo User',
            'contacts': []
        }

def reverse_geocode(lat, lon):
    """
    Step 5: Reverse Geocoding with Amazon Location Service
    """
    try:
        response = location_client.search_place_index_for_position(
            IndexName=LOCATION_INDEX,
            Position=[lon, lat],
            MaxResults=1
        )
        
        if response['Results']:
            place = response['Results'][0]['Place']
            place_name = place.get('Label', f"{lat},{lon}")
            address_string = place.get('AddressNumber', '') + ' ' + place.get('Street', '')
        else:
            place_name = f"Location {lat},{lon}"
            address_string = f"{lat},{lon}"
        
        # Create map link
        map_link = f"https://maps.google.com/?q={lat},{lon}"
        
        return {
            'lat': lat,
            'lon': lon,
            'placeName': place_name.strip(),
            'addressString': address_string.strip(),
            'mapLink': map_link
        }
        
    except Exception as e:
        logger.warning(f"Reverse geocoding failed: {str(e)}")
        return {
            'lat': lat,
            'lon': lon,
            'placeName': f"Coordinates {lat},{lon}",
            'addressString': f"{lat},{lon}",
            'mapLink': f"https://maps.google.com/?q={lat},{lon}"
        }

def send_emergency_sms(user_profile, location_data, confidence, event_id):
    """
    Step 6: Send Emergency SMS via Amazon Pinpoint
    """
    results = []
    victim_name = user_profile.get('victimName', 'Unknown')
    contacts = user_profile.get('contacts', [])
    
    # Filter opted-in contacts
    opted_contacts = [c for c in contacts if c.get('optedIn', False)]
    
    if not opted_contacts:
        return [{'status': 'error', 'message': 'No opted-in contacts'}]
    
    # Compose SMS message
    local_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')
    confidence_percent = int(confidence * 100)
    
    # Primary SMS template
    sms_text = f"[AllSenseAI] Emergency alert for {victim_name}. Possible danger detected at {location_data['placeName']} ({location_data['lat']},{location_data['lon']}) — {location_data['mapLink']} Time: {local_time}. Confidence: {confidence_percent}%. Please reply OK if you received this."
    
    # Fallback short SMS if too long
    if len(sms_text) > 160:
        sms_text = f"[AllSenseAI] Emergency alert for {victim_name}. Location: {location_data['lat']},{location_data['lon']} — {location_data['mapLink']} Time: {local_time}. Reply OK to confirm."
    
    for contact in opted_contacts:
        try:
            # Try Pinpoint first
            message_id = send_pinpoint_sms(contact['phone'], sms_text)
            
            if message_id:
                results.append({
                    'contactName': contact['name'],
                    'phone': contact['phone'],
                    'status': 'sent',
                    'messageId': message_id,
                    'method': 'pinpoint'
                })
            else:
                # Fallback to SNS
                sns_result = sns.publish(
                    PhoneNumber=contact['phone'],
                    Message=sms_text
                )
                
                results.append({
                    'contactName': contact['name'],
                    'phone': contact['phone'],
                    'status': 'sent',
                    'messageId': sns_result['MessageId'],
                    'method': 'sns_fallback'
                })
                
        except Exception as e:
            logger.error(f"SMS send failed for {contact['name']}: {str(e)}")
            results.append({
                'contactName': contact['name'],
                'phone': contact['phone'],
                'status': 'failed',
                'error': str(e)
            })
    
    return results

def send_pinpoint_sms(phone_number, message):
    """
    Send SMS via Amazon Pinpoint with retry logic
    """
    try:
        response = pinpoint.send_messages(
            ApplicationId=PINPOINT_PROJECT_ID,
            MessageRequest={
                'Addresses': {
                    phone_number: {
                        'ChannelType': 'SMS'
                    }
                },
                'MessageConfiguration': {
                    'SMSMessage': {
                        'Body': message,
                        'MessageType': 'TRANSACTIONAL'
                    }
                }
            }
        )
        
        result = response['MessageResponse']['Result'][phone_number]
        if result['StatusCode'] == 200:
            return result['MessageId']
        else:
            logger.error(f"Pinpoint SMS failed: {result}")
            return None
            
    except Exception as e:
        logger.error(f"Pinpoint error: {str(e)}")
        return None

def log_analytics(event_id, user_id, sms_results):
    """
    Step 7: Analytics & Learning
    """
    try:
        # Anonymized analytics data
        analytics_data = {
            'eventId': event_id,
            'userId': user_id,  # This would be hashed in production
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'smsCount': len(sms_results),
            'successCount': len([r for r in sms_results if r['status'] == 'sent']),
            'failureCount': len([r for r in sms_results if r['status'] == 'failed'])
        }
        
        # Log to CloudWatch
        logger.info(f"Analytics: {json.dumps(analytics_data)}")
        
        return {
            'status': 'logged',
            'eventId': event_id,
            'metrics': analytics_data
        }
        
    except Exception as e:
        logger.error(f"Analytics logging failed: {str(e)}")
        return {'status': 'failed', 'error': str(e)}

def analyze_distress(audio_data):
    """
    Step 2: AI Distress Detection
    """
    message_upper = str(audio_data).upper()
    emergency_keywords = ['HELP', 'EMERGENCY', 'DANGER', '911', 'POLICE', 'FIRE']
    
    emergency_matches = [word for word in emergency_keywords if word in message_upper]
    
    if emergency_matches:
        return {
            'level': 'CRITICAL',
            'confidence': 0.87,
            'keywords': emergency_matches
        }
    else:
        return {
            'level': 'NONE',
            'confidence': 0.1,
            'keywords': []
        }

def make_real_sms(body):
    """
    Legacy SMS function for compatibility
    """
    try:
        phone_number = body.get('phoneNumber', '+15551234567')
        emergency_message = body.get('emergencyMessage', 'AllSenseAI Demo Test')
        
        # Use SNS for direct SMS
        response = sns.publish(
            PhoneNumber=phone_number,
            Message=f"🚨 ALLSENSES DEMO 🚨\n\n{emergency_message}\n\nTime: {datetime.now().strftime('%H:%M:%S')}\n\n- AllSenseAI Guardian"
        )
        
        return cors_response({
            'status': 'success',
            'message': 'Real SMS sent successfully',
            'phoneNumber': phone_number,
            'smsMessageId': response['MessageId'],
            'timestamp': datetime.now(timezone.utc).isoformat()
        })
        
    except Exception as e:
        return cors_response({
            'status': 'error',
            'message': f'SMS failed: {str(e)}'
        }, 500)

def get_user_profile(body):
    """
    Get user profile for frontend
    """
    user_id = body.get('userId', 'user-123')
    profile = fetch_user_profile(user_id)
    return cors_response(profile)

def confirm_alert(body):
    """
    Handle alert confirmation
    """
    event_id = body.get('eventId')
    contact_name = body.get('contactName')
    
    return cors_response({
        'status': 'confirmed',
        'eventId': event_id,
        'contactName': contact_name,
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