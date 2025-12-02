#!/bin/bash

# AllSenses AI Guardian - AWS API Gateway Deployment Script
# This script deploys the API Gateway infrastructure for the MVP

set -e

# Configuration
STACK_NAME="allsenses-api-gateway"
TEMPLATE_FILE="infrastructure/api-gateway.yaml"
REGION="us-east-1"
ENVIRONMENT="dev"
APPLICATION_NAME="allsenses-ai-guardian"

echo "🚀 Deploying AllSenses AI Guardian API Gateway..."
echo "Stack Name: $STACK_NAME"
echo "Environment: $ENVIRONMENT"
echo "Region: $REGION"

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed. Please install it first."
    exit 1
fi

# Check if user is authenticated
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS credentials not configured. Please run 'aws configure' first."
    exit 1
fi

# Validate CloudFormation template
echo "📋 Validating CloudFormation template..."
aws cloudformation validate-template \
    --template-body file://$TEMPLATE_FILE \
    --region $REGION

# Deploy the stack
echo "🔧 Deploying API Gateway stack..."
aws cloudformation deploy \
    --template-file $TEMPLATE_FILE \
    --stack-name $STACK_NAME \
    --parameter-overrides \
        Environment=$ENVIRONMENT \
        ApplicationName=$APPLICATION_NAME \
    --capabilities CAPABILITY_IAM \
    --region $REGION \
    --no-fail-on-empty-changeset

# Get stack outputs
echo "📊 Getting deployment outputs..."
API_GATEWAY_URL=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' \
    --output text)

THREAT_DETECTION_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`ThreatDetectionEndpoint`].OutputValue' \
    --output text)

EMERGENCY_EVENT_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`EmergencyEventEndpoint`].OutputValue' \
    --output text)

USER_MANAGEMENT_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`UserManagementEndpoint`].OutputValue' \
    --output text)

AI_AGENT_WORKFLOW_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`AiAgentWorkflowEndpoint`].OutputValue' \
    --output text)

echo ""
echo "✅ API Gateway deployment completed successfully!"
echo ""
echo "📡 API Gateway Endpoints:"
echo "  Base URL: $API_GATEWAY_URL"
echo ""
echo "🎯 AI Agent Endpoints:"
echo "  Threat Detection: $THREAT_DETECTION_ENDPOINT"
echo "  Emergency Events: $EMERGENCY_EVENT_ENDPOINT"
echo "  User Management: $USER_MANAGEMENT_ENDPOINT"
echo "  Complete Workflow: $AI_AGENT_WORKFLOW_ENDPOINT"
echo ""
echo "🔧 Next Steps:"
echo "  1. Deploy your Spring Boot application to ECS or EC2"
echo "  2. Update the Load Balancer target group with your application instances"
echo "  3. Test the API endpoints using the URLs above"
echo "  4. Configure your frontend to use these API Gateway endpoints"
echo ""
echo "📝 Example API Test:"
echo "  curl -X POST $THREAT_DETECTION_ENDPOINT \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"userId\":\"test-user\",\"location\":\"Test Location\",\"audioData\":\"test-audio\"}'"