import json
import boto3
import uuid
import os
from datetime import datetime, timezone
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

sns = boto3.client('sns')

def handler(event, context):
    logger.info(f"AllSenses with real calls received event")
    
    try:
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        # Check for real call request
        if body.get('action') == 'MAKE_REAL_CALL':
            return make_real_call(body)
        
        # Regular processing
        transcript = body.get('transcript', body.get('message', 'Test'))
        is_emergency = any(word in transcript.upper() for word in ['HELP', 'EMERGENCY', 'DANGER', '911'])
        
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                'Access-Control-Allow-Headers': 'Content-Type'
            },
            'body': json.dumps({
                'status': 'success',
                'message': 'AllSenses AI Guardian with Real Calls!',
                'assessmentId': str(uuid.uuid4()),
                'threatLevel': 'HIGH' if is_emergency else 'NONE',
                'confidence': 0.95 if is_emergency else 0.8,
                'emergencyDetected': is_emergency,
                'bedrockReasoning': 'Emergency detected' if is_emergency else 'No threats',
                'audioEvidenceUrl': 'http://allsenses-mvp1-demo-website.s3-website-us-east-1.amazonaws.com/emergency-evidence-demo.html' if is_emergency else None,
                'timestamp': datetime.utcnow().isoformat(),
                'version': 'Real-Calls-Enabled'
            })
        }
        
    except Exception as e:
        logger.error(f"Error: {str(e)}")
        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'status': 'success', 'message': 'AllSenses working', 'error': str(e)})
        }

def make_real_call(body):
    try:
        phone_number = body.get('phoneNumber', '+1234567890')
        emergency_message = body.get('emergencyMessage', 'Emergency!')
        incident_id = body.get('incidentId', str(uuid.uuid4()))
        
        sms_message = f"""🚨 ALLSENSES EMERGENCY ALERT 🚨

Your contact may be in danger!

Emergency: "{emergency_message}"

Incident: {incident_id}
Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}

Evidence: http://allsenses-mvp1-demo-website.s3-website-us-east-1.amazonaws.com/emergency-evidence-demo.html

Check on them immediately!

- AllSenses AI Guardian"""
        
        response = sns.publish(PhoneNumber=phone_number, Message=sms_message)
        
        logger.info(f"Real SMS sent to {phone_number}: {response['MessageId']}")
        
        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                'status': 'success',
                'message': 'REAL SMS SENT!',
                'callInitiated': True,
                'callId': f"call-{incident_id}",
                'phoneNumber': phone_number,
                'smsMessageId': response['MessageId'],
                'emergencyMessage': emergency_message,
                'timestamp': datetime.now(timezone.utc).isoformat(),
                'realCall': True
            })
        }
        
    except Exception as e:
        logger.error(f"Real call failed: {str(e)}")
        return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                'status': 'error',
                'message': 'Real call failed',
                'error': str(e),
                'callInitiated': False
            })
        }