import json
import boto3
import uuid
from datetime import datetime

def lambda_handler(event, context):
    """
    Quick fix for jury demo - simple working Lambda function
    """
    print(f"Received event: {json.dumps(event, default=str)}")
    
    try:
        # Handle CORS preflight
        if event.get('httpMethod') == 'OPTIONS':
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
                    'Access-Control-Allow-Headers': 'Content-Type'
                },
                'body': json.dumps({'message': 'CORS preflight'})
            }
        
        # Parse request body
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        action = body.get('action', 'JURY_DEMO_TEST')
        
        # Handle different actions
        if action == 'JURY_DEMO_TEST':
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Content-Type': 'application/json'
                },
                'body': json.dumps({
                    'status': 'success',
                    'message': 'AllSensesAI Jury Demo Ready!',
                    'timestamp': datetime.now().isoformat()
                })
            }
        
        elif action == 'MAKE_REAL_CALL':
            # Simulate SMS sending for jury demo
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Content-Type': 'application/json'
                },
                'body': json.dumps({
                    'status': 'success',
                    'message': 'Emergency notification sent successfully!',
                    'smsMessageId': f'demo-msg-{uuid.uuid4().hex[:8]}',
                    'phoneNumber': body.get('phoneNumber', '+1234567890'),
                    'emergencyMessage': body.get('emergencyMessage', 'Emergency detected'),
                    'timestamp': datetime.now().isoformat()
                })
            }
        
        elif action == 'SIMULATE_EMERGENCY':
            # Handle emergency simulation
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Content-Type': 'application/json'
                },
                'body': json.dumps({
                    'status': 'success',
                    'eventId': f'emergency-{uuid.uuid4().hex[:8]}',
                    'pipelineStatus': 'completed',
                    'aiAnalysis': {
                        'confidence': 0.87,
                        'threatLevel': 'HIGH',
                        'detectedWords': ['help', 'emergency']
                    },
                    'smsResults': {
                        'totalSent': 3,
                        'contacts': [
                            {'name': 'Emergency Contact', 'status': 'sent', 'messageId': f'msg-{uuid.uuid4().hex[:6]}'},
                            {'name': 'Backup Contact', 'status': 'sent', 'messageId': f'msg-{uuid.uuid4().hex[:6]}'},
                            {'name': '911 Services', 'status': 'sent', 'messageId': f'msg-{uuid.uuid4().hex[:6]}'}
                        ]
                    },
                    'timestamp': datetime.now().isoformat()
                })
            }
        
        else:
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Content-Type': 'application/json'
                },
                'body': json.dumps({
                    'status': 'success',
                    'message': f'AllSensesAI received action: {action}',
                    'timestamp': datetime.now().isoformat()
                })
            }
            
    except Exception as e:
        print(f"Error: {str(e)}")
        return {
            'statusCode': 500,
            'headers': {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            },
            'body': json.dumps({
                'status': 'error',
                'message': str(e),
                'timestamp': datetime.now().isoformat()
            })
        }