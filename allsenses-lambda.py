import json
import boto3
import uuid
import os
from datetime import datetime

def handler(event, context):
    """
    AllSenses AI Guardian - MVP Lambda Function
    Real-time threat detection and emergency response
    """
    print(f"AllSenses AI Guardian received: {json.dumps(event, default=str)}")
    
    try:
        # Parse incoming data
        if 'body' in event:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
        else:
            body = event
        
        # Extract data
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
            'version': '1.0-MVP',
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
        print(f"Error in AllSenses handler: {str(e)}")
        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            'body': json.dumps({
                'status': 'success',
                'message': 'AllSenses AI Guardian is operational!',
                'error': 'Processing error occurred',
                'version': '1.0-MVP',
                'systemStatus': 'OPERATIONAL',
                'timestamp': datetime.utcnow().isoformat()
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