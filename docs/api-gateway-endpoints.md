# AllSenses AI Guardian - API Gateway Endpoints

This document describes all REST API endpoints exposed through AWS API Gateway for the AllSenses AI Guardian MVP.

## Base Configuration

- **Base URL**: `https://{api-gateway-id}.execute-api.{region}.amazonaws.com/{stage}`
- **Content-Type**: `application/json`
- **CORS**: Enabled for all endpoints
- **Authentication**: None (MVP version)

## API Endpoints

### 1. Threat Detection API

#### POST /api/v1/threat-detection/analyze
Perform comprehensive threat analysis with LLM reasoning.

**Request Body:**
```json
{
  "userId": "string",
  "location": "string",
  "audioData": "string",
  "motionData": "string",
  "environmentalData": "string",
  "biometricData": "string",
  "additionalContext": "string",
  "enhancedConfidenceScoring": false,
  "advancedClassification": false
}
```

**Response:**
```json
{
  "threatAssessment": {
    "assessmentId": "string",
    "userId": "string",
    "threatLevel": "LOW|MEDIUM|HIGH|CRITICAL",
    "confidenceScore": 0.85,
    "recommendedAction": "string",
    "llmReasoning": "string",
    "requiresEmergencyResponse": false,
    "processingDurationMs": 1250
  },
  "confidenceAnalysis": {
    "initialConfidence": 0.75,
    "finalConfidence": 0.85,
    "adjustmentReasoning": "string",
    "success": true
  },
  "classificationResult": {
    "finalThreatLevel": "HIGH",
    "finalConfidenceScore": 0.85,
    "classificationReasoning": "string",
    "success": true
  },
  "analysisComplete": true
}
```

#### POST /api/v1/threat-detection/quick-assess
Perform quick threat assessment.

**Request Body:**
```json
{
  "userId": "string",
  "location": "string",
  "audioData": "string",
  "motionData": "string",
  "environmentalData": "string",
  "biometricData": "string",
  "context": "string"
}
```

**Response:**
```json
{
  "assessmentId": "string",
  "threatLevel": "HIGH",
  "confidenceScore": 0.85,
  "recommendedAction": "string",
  "requiresEmergencyResponse": true,
  "llmReasoning": "string",
  "processingTimeMs": 850
}
```

#### GET /api/v1/threat-detection/statistics
Get threat detection statistics.

**Response:**
```json
{
  "total_assessments": 150,
  "high_threat_assessments": 12,
  "average_confidence_score": 0.78,
  "average_processing_time_ms": 1200,
  "bedrock_integration": "ACTIVE",
  "status": "SUCCESS"
}
```

### 2. Emergency Event API

#### POST /api/v1/emergency-events/create-and-process
Create and process emergency event autonomously.

**Request Body:**
```json
{
  "assessmentId": "string",
  "userId": "string",
  "threatLevel": "HIGH",
  "confidenceScore": 0.85,
  "llmReasoning": "string",
  "location": "string",
  "timeContext": "string",
  "environmentalFactors": "string",
  "audioData": "string",
  "motionData": "string",
  "biometricData": "string",
  "sendNotifications": true
}
```

**Response:**
```json
{
  "emergencyEventId": "string",
  "emergencyEvent": {
    "eventId": "string",
    "userId": "string",
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00Z",
    "threatLevel": "HIGH",
    "location": "string"
  },
  "decisionResult": {
    "decisionId": "string",
    "finalDecision": {
      "responseType": "EMERGENCY_SERVICES",
      "priorityLevel": "HIGH",
      "contactEmergencyServices": true,
      "notifyTrustedContacts": true
    },
    "llmReasoning": "string",
    "success": true
  },
  "notificationResult": {
    "notificationId": "string",
    "totalNotificationsSent": 3,
    "successfulNotifications": 3,
    "success": true
  },
  "success": true,
  "autonomousProcessingCompleted": true
}
```

#### GET /api/v1/emergency-events/{eventId}
Get emergency event by ID.

**Response:**
```json
{
  "eventId": "string",
  "userId": "string",
  "status": "ACTIVE|RESOLVED|FALSE_ALARM",
  "createdAt": "2024-01-15T10:30:00Z",
  "resolvedAt": null,
  "threatLevel": "HIGH",
  "location": "string",
  "responseActions": []
}
```

#### GET /api/v1/emergency-events/user/{userId}
Get emergency events for user.

#### GET /api/v1/emergency-events/active
Get active emergency events.

#### GET /api/v1/emergency-events/statistics
Get emergency processing statistics.

### 3. User Management API

#### POST /api/v1/users/register
Register new user with consent.

**Request Body:**
```json
{
  "email": "user@example.com",
  "phoneNumber": "+1234567890",
  "firstName": "John",
  "lastName": "Doe",
  "consentGiven": true,
  "consentVersion": "1.0"
}
```

**Response:**
```json
{
  "userId": "string",
  "user": {
    "userId": "string",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "consentStatus": "VALID",
    "consentTimestamp": "2024-01-15T10:30:00Z"
  },
  "success": true,
  "consentStatus": "VALID"
}
```

#### GET /api/v1/users/{userId}
Get user by ID.

#### GET /api/v1/users/email/{email}
Get user by email.

#### PUT /api/v1/users/{userId}/consent
Update user consent.

**Request Body:**
```json
{
  "consentGiven": true,
  "consentVersion": "1.0"
}
```

#### POST /api/v1/users/{userId}/trusted-contacts
Add trusted contact.

**Request Body:**
```json
{
  "name": "Emergency Contact",
  "phoneNumber": "+1234567890",
  "email": "contact@example.com",
  "relationship": "Family",
  "preferredContactMethod": "SMS",
  "isPrimary": true
}
```

#### GET /api/v1/users/statistics
Get user management statistics.

**Response:**
```json
{
  "total_users": 50,
  "users_with_consent": 45,
  "active_users": 42,
  "consent_rate": 0.9,
  "database_integration": "DynamoDB",
  "status": "SUCCESS"
}
```

### 4. AI Agent Comprehensive API

#### POST /api/v1/ai-agent/complete-workflow
Execute complete AI agent workflow from sensor data to emergency response.

**Request Body:**
```json
{
  "userId": "string",
  "location": "string",
  "audioData": "string",
  "motionData": "string",
  "environmentalData": "string",
  "biometricData": "string",
  "additionalContext": "string"
}
```

**Response:**
```json
{
  "workflowId": "string",
  "threatAssessment": {
    "assessmentId": "string",
    "threatLevel": "HIGH",
    "confidenceScore": 0.85,
    "requiresEmergencyResponse": true
  },
  "emergencyProcessingResult": {
    "processingId": "string",
    "success": true,
    "actionExecutionResult": {
      "actionsExecuted": ["EMERGENCY_SERVICES_CONTACTED", "TRUSTED_CONTACTS_NOTIFIED"]
    },
    "serverlessProcessingTriggered": true
  },
  "workflowStatus": "EMERGENCY_RESPONSE_TRIGGERED",
  "autonomousActionsExecuted": 2
}
```

#### POST /api/v1/ai-agent/threat-analysis
Perform threat analysis only.

#### POST /api/v1/ai-agent/emergency-response/{assessmentId}
Trigger emergency response for existing threat assessment.

#### POST /api/v1/ai-agent/emergency-decision
Make emergency decision without executing actions.

#### GET /api/v1/ai-agent/system-status
Get AI agent system status.

**Response:**
```json
{
  "threat_analysis": {
    "total_assessments": 150,
    "status": "SUCCESS"
  },
  "emergency_processing": {
    "total_events": 12,
    "status": "SUCCESS"
  },
  "lambda_integration": {
    "status": "CONNECTED",
    "functions_available": 3
  },
  "sns_integration": {
    "status": "CONNECTED",
    "topics_configured": 2
  },
  "ai_agent_qualification": {
    "condition_1_llm_integration": {
      "status": "FULLY_IMPLEMENTED",
      "description": "AWS Bedrock LLMs power all reasoning and decision-making"
    },
    "condition_2_aws_services": {
      "status": "FULLY_IMPLEMENTED",
      "services": "Bedrock, DynamoDB, Lambda, SNS"
    },
    "condition_3_ai_agent_qualification": {
      "reasoning_llms": "IMPLEMENTED",
      "autonomous_capabilities": "IMPLEMENTED",
      "external_integrations": "IMPLEMENTED",
      "status": "FULLY_QUALIFIED"
    },
    "overall_qualification": "AWS AI AGENT FULLY QUALIFIED"
  }
}
```

#### POST /api/v1/ai-agent/test/complete-functionality
Test complete AI agent functionality.

## Error Responses

All endpoints return standard HTTP status codes and error responses:

```json
{
  "success": false,
  "errorMessage": "Detailed error description",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## AWS AI Agent Qualification

This API Gateway setup demonstrates compliance with all three AWS AI Agent conditions:

1. **LLM Integration**: All reasoning and decision-making powered by AWS Bedrock
2. **AWS Services**: Complete integration with Bedrock, DynamoDB, Lambda, SNS, and API Gateway
3. **AI Agent Qualification**: 
   - Autonomous threat assessment and emergency response
   - Multi-stage LLM reasoning for decisions
   - Complete API, database, and external service integration

## Testing the API

### Using curl:
```bash
# Test threat detection
curl -X POST https://your-api-gateway-url/api/v1/threat-detection/analyze \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user","location":"Test Location","audioData":"test-audio"}'

# Test complete AI agent workflow
curl -X POST https://your-api-gateway-url/api/v1/ai-agent/complete-workflow \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user","location":"Test Location","audioData":"emergency-audio"}'
```

### Using PowerShell:
```powershell
# Test threat detection
Invoke-RestMethod -Uri "https://your-api-gateway-url/api/v1/threat-detection/analyze" `
  -Method Post -ContentType "application/json" `
  -Body '{"userId":"test-user","location":"Test Location","audioData":"test-audio"}'
```

## Deployment

Use the provided deployment scripts:
- Linux/Mac: `./scripts/deploy-api-gateway.sh`
- Windows: `.\scripts\deploy-api-gateway.ps1`