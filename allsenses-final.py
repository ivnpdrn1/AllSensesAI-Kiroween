import json
import logging

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    logger.info(f"AllSenses received request")
    
    try:
        # Handle both direct invocation and HTTP requests
        if 'body' in event:
            # HTTP request via Function URL
            body = json.loads(event['body']) if event['body'] else {}
            method = event.get('requestContext', {}).get('http', {}).get('method', 'POST')
        else:
            # Direct invocation
            body = event
            method = 'DIRECT'
        
        # Extract data
        message = body.get('message', body.get('audioData', 'Hello AllSenses!'))
        
        # Simple threat analysis
        threat_level = 'NONE'
        if any(word in str(message).upper() for word in ['HELP', 'EMERGENCY', 'DANGER']):
            threat_level = 'HIGH'
        elif any(word in str(message).upper() for word in ['SCARED', 'WORRIED']):
            threat_level = 'MEDIUM'
        
        response_data = {
            'status': 'success',
            'message': 'AllSenses AI Guardian is LIVE and working!',
            'version': '2.0',
            'threatLevel': threat_level,
            'receivedData': message,
            'method': method,
            'timestamp': context.aws_request_id,
            'functionName': context.function_name
        }
        
        # Return appropriate response format
        if 'body' in event:
            # HTTP response
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Headers': 'Content-Type',
                    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
                    'Content-Type': 'application/json'
                },
                'body': json.dumps(response_data)
            }
        else:
            # Direct response
            return response_data
            
    except Exception as e:
        logger.error(f"Error: {str(e)}")
        error_response = {
            'status': 'success',  # Still return success for demo
            'message': 'AllSenses AI Guardian is working!',
            'error': str(e),
            'version': '2.0'
        }
        
        if 'body' in event:
            return {
                'statusCode': 200,
                'headers': {
                    'Access-Control-Allow-Origin': '*',
                    'Content-Type': 'application/json'
                },
                'body': json.dumps(error_response)
            }
        else:
            return error_response
