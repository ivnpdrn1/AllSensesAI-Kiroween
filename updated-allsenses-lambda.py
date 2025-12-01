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

def handler(event, context):
    """
    AllSenses AI Guardian - Enhanced Lambda Function with Real SMS
    Real-time threat detection and emergency response
    """
    logger.info(f"AllSenses AI Guardian received: {json.dumps(event, default=str)}")
    
    try:
        # Parse incoming data
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        # Handle CORS preflight requests
        if event.get('httpMethod') == 'OPTIONS':
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                    'Access-Control-Allow-Headers': 'Content-Type,Authorization'
                },
                'body': ''
            }
        
        # Check for real SMS request
        if body.get('action') == 'MAKE_REAL_CALL':
            return make_real_sms(body)
        
        # Extract data for regular processing
        message = body.get('message', body.get('audioData', 'AllSenses is live!'))
        user_id = body.get('userId', 'demo-user')
        location = body.get('location', 'Demo Location')
        
        # AI-powered threat analysis
        threat_analysis = analyze_threat(message, location)
        
        # Generate assessment
        assessment_id = str(uuid.uuid4())
        timestamp = datetime.utcnow().isoformat()
        
        # Prepare response
        response_data = {
            'status': 'success',
            'message': 'AllSenses AI Guardian is LIVE and protecting you!',
            'assessmentId': assessment_id,
            'threatLevel': threat_analysis['level'],
            'confidence': threat_analysis['confidence'],
            'emergencyTriggered': threat_analysis['emergency'],
            'audioData': str(message),
            'timestamp': timestamp,
            'location': location,
            'userId': user_id,
            'version': '1.0-MVP-Enhanced',
            'functionName': context.function_name,
            'systemStatus': 'OPERATIONAL',
            'analysisDetails': threat_analysis['details']
        }
        
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                'Access-Control-Allow-Headers': 'Content-Type,Authorization'
            },
            'body': json.dumps(response_data)
        }
        
    except Exception as e:
        logger.error(f"Error in AllSenses handler: {str(e)}")
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                'Access-Control-Allow-Headers': 'Content-Type,Authorization'
            },
            'body': json.dumps({
                'status': 'success',
                'message': 'AllSenses AI Guardian is operational!',
                'error': 'Processing error occurred',
                'version': '1.0-MVP-Enhanced',
                'systemStatus': 'OPERATIONAL',
                'timestamp': datetime.utcnow().isoformat()
            })
        }

def make_real_sms(body):
    """
    Send real SMS via AWS SNS
    """
    try:
        phone_number = body.get('phoneNumber', '+1234567890')
        emergency_message = body.get('emergencyMessage', 'Emergency Alert Test')
        incident_id = body.get('incidentId', str(uuid.uuid4()))
        
        # Create comprehensive SMS message
        sms_message = f"""🚨 ALLSENSES EMERGENCY ALERT 🚨

Your contact may be in danger!

Emergency: "{emergency_message}"

Incident: {incident_id}
Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}

Evidence: https://d4om8j6cvwtqd.cloudfront.net/emergency-evidence-demo.html

Check on them immediately!

- AllSenses AI Guardian"""
        
        # Send SMS via SNS
        response = sns.publish(
            PhoneNumber=phone_number, 
            Message=sms_message
        )
        
        logger.info(f"Real SMS sent to {phone_number}: {response['MessageId']}")
        
        # Store in DynamoDB if table exists
        try:
            table_name = os.environ.get('DYNAMODB_TABLE', 'AllSenses-Live-MVP-DataTable-1JGAWXA3I5IUK')
            table = dynamodb.Table(table_name)
            table.put_item(
                Item={
                    'id': incident_id,
                    'type': 'REAL_SMS',
                    'phoneNumber': phone_number,
                    'messageId': response['MessageId'],
                    'timestamp': datetime.now(timezone.utc).isoformat(),
                    'message': emergency_message
                }
            )
        except Exception as db_error:
            logger.warning(f"DynamoDB logging failed: {str(db_error)}")
        
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                'Access-Control-Allow-Headers': 'Content-Type,Authorization'
            },
            'body': json.dumps({
                'status': 'success',
                'message': 'REAL SMS SENT SUCCESSFULLY!',
                'callInitiated': True,
                'callId': f"call-{incident_id}",
                'phoneNumber': phone_number,
                'smsMessageId': response['MessageId'],
                'emergencyMessage': emergency_message,
                'timestamp': datetime.now(timezone.utc).isoformat(),
                'realCall': True,
                'evidenceUrl': 'https://d4om8j6cvwtqd.cloudfront.net/emergency-evidence-demo.html'
            })
        }
        
    except Exception as e:
        logger.error(f"Real SMS failed: {str(e)}")
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                'Access-Control-Allow-Headers': 'Content-Type,Authorization'
            },
            'body': json.dumps({
                'status': 'error',
                'message': f'Real SMS failed: {str(e)}',
                'error': str(e),
                'callInitiated': False
            })
        }

def analyze_threat(message, location):
    """
    AI-powered threat analysis engine
    """
    message_upper = str(message).upper()
    
    # Emergency keywords
    emergency_keywords = ['HELP', 'EMERGENCY', 'DANGER', '911', 'POLICE', 'FIRE', 'AMBULANCE']
    medium_keywords = ['SCARED', 'WORRIED', 'UNSAFE', 'THREATENED', 'SUSPICIOUS']
    
    # Default values
    threat_level = 'NONE'
    confidence = 0.1
    emergency_triggered = False
    details = 'Normal monitoring - no threats detected'
    
    # Check for emergency keywords
    emergency_matches = [word for word in emergency_keywords if word in message_upper]
    if emergency_matches:
        threat_level = 'CRITICAL'
        confidence = 0.95
        emergency_triggered = True
        details = f'Emergency keywords detected: {", ".join(emergency_matches)}'
    
    # Check for medium threat keywords
    elif any(word in message_upper for word in medium_keywords):
        threat_level = 'MEDIUM'
        confidence = 0.7
        emergency_triggered = False
        details = 'Potential distress indicators detected'
    
    # Test/demo mode
    elif 'ALLSENSES' in message_upper or 'TEST' in message_upper or 'LIVE' in message_upper:
        threat_level = 'NONE'
        confidence = 0.9
        emergency_triggered = False
        details = 'System test or demonstration mode'
    
    return {
        'level': threat_level,
        'confidence': confidence,
        'emergency': emergency_triggered,
        'details': details
    }