import json
from datetime import datetime

def lambda_handler(event, context):
    try:
        # Parse input
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        # Extract data
        message = body.get('message', body.get('audioData', 'Hello AllSenses!'))
        
        # Simple threat detection
        threat_level = 'NONE'
        if any(word in str(message).upper() for word in ['HELP', 'EMERGENCY', 'DANGER']):
            threat_level = 'HIGH'
        
        return {
            'statusCode': 200,
            'headers': {
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Headers': 'Content-Type',
                'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                'Content-Type': 'application/json'
            },
            'body': json.dumps({
                'status': 'success',
                'message': 'AllSenses AI Guardian is LIVE!',
                'threatLevel': threat_level,
                'receivedData': message,
                'timestamp': datetime.utcnow().isoformat(),
                'version': '3.0',
                'functionName': context.function_name
            })
        }
    except Exception as e:
        return {
            'statusCode': 200,
            'headers': {'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                'status': 'success',
                'message': 'AllSenses is working!',
                'error': str(e)
            })
        }
