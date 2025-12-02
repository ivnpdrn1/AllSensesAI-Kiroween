#!/bin/bash

# AllSenses AI Guardian - Secure AWS Deployment Script
# Bash script for Linux/macOS deployment with security best practices

set -euo pipefail

# Default values
ENVIRONMENT="production"
REGION="us-east-1"
STACK_NAME="AllSenses-AI-Guardian"
SKIP_PREREQUISITES=false

# Color functions for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

success() { echo -e "${GREEN}✅ $1${NC}"; }
info() { echo -e "${CYAN}ℹ️  $1${NC}"; }
warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
error() { echo -e "${RED}❌ $1${NC}"; }

# Usage function
usage() {
    cat << EOF
Usage: $0 -e <email> [OPTIONS]

Required:
    -e, --email <email>         Notification email address

Options:
    -n, --environment <env>     Environment (default: production)
    -r, --region <region>       AWS region (default: us-east-1)
    -s, --stack-name <name>     CloudFormation stack name (default: AllSenses-AI-Guardian)
    --skip-prerequisites        Skip prerequisite checks
    -h, --help                  Show this help message

Examples:
    $0 -e admin@company.com
    $0 -e admin@company.com -n staging -r us-west-2
    $0 -e admin@company.com --skip-prerequisites

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--email)
            NOTIFICATION_EMAIL="$2"
            shift 2
            ;;
        -n|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -r|--region)
            REGION="$2"
            shift 2
            ;;
        -s|--stack-name)
            STACK_NAME="$2"
            shift 2
            ;;
        --skip-prerequisites)
            SKIP_PREREQUISITES=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Check required parameters
if [[ -z "${NOTIFICATION_EMAIL:-}" ]]; then
    error "Notification email is required"
    usage
    exit 1
fi

info "Starting AllSenses AI Guardian deployment to AWS..."
info "Environment: $ENVIRONMENT"
info "Region: $REGION"
info "Notification Email: $NOTIFICATION_EMAIL"

# Validate email format
if [[ ! "$NOTIFICATION_EMAIL" =~ ^[^\s@]+@[^\s@]+\.[^\s@]+$ ]]; then
    error "Invalid email format: $NOTIFICATION_EMAIL"
    exit 1
fi

# Check prerequisites
if [[ "$SKIP_PREREQUISITES" != "true" ]]; then
    info "Checking prerequisites..."
    
    # Check AWS CLI
    if command -v aws >/dev/null 2>&1; then
        AWS_VERSION=$(aws --version 2>&1)
        success "AWS CLI found: $AWS_VERSION"
    else
        error "AWS CLI not found. Please install AWS CLI first."
        info "Download from: https://aws.amazon.com/cli/"
        exit 1
    fi
    
    # Check jq for JSON parsing
    if ! command -v jq >/dev/null 2>&1; then
        warning "jq not found. Installing jq for JSON parsing..."
        if command -v apt-get >/dev/null 2>&1; then
            sudo apt-get update && sudo apt-get install -y jq
        elif command -v yum >/dev/null 2>&1; then
            sudo yum install -y jq
        elif command -v brew >/dev/null 2>&1; then
            brew install jq
        else
            error "Could not install jq. Please install it manually."
            exit 1
        fi
    fi
    
    # Check AWS credentials
    if aws sts get-caller-identity >/dev/null 2>&1; then
        IDENTITY=$(aws sts get-caller-identity --output json)
        ACCOUNT=$(echo "$IDENTITY" | jq -r '.Account')
        ARN=$(echo "$IDENTITY" | jq -r '.Arn')
        success "AWS credentials configured for account: $ACCOUNT"
        info "User/Role: $ARN"
    else
        error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    fi
    
    # Validate region
    if aws ec2 describe-regions --region "$REGION" --output table >/dev/null 2>&1; then
        success "Region $REGION is valid and accessible"
    else
        error "Invalid or inaccessible region: $REGION"
        exit 1
    fi
fi

# Get current user ARN for KMS key administration
CURRENT_USER_ARN=$(aws sts get-caller-identity --query 'Arn' --output text)
info "Using current user/role for KMS key administration: $CURRENT_USER_ARN"

# Check if Bedrock models are available
info "Checking AWS Bedrock model access..."
if aws bedrock list-foundation-models --region "$REGION" --output json >/dev/null 2>&1; then
    MODELS=$(aws bedrock list-foundation-models --region "$REGION" --output json)
    
    if echo "$MODELS" | jq -e '.modelSummaries[] | select(.modelId == "anthropic.claude-3-sonnet-20240229-v1:0")' >/dev/null; then
        success "Claude-3 Sonnet model is available"
    else
        warning "Claude-3 Sonnet model not available. Request access in Bedrock console."
    fi
    
    if echo "$MODELS" | jq -e '.modelSummaries[] | select(.modelId == "amazon.titan-text-express-v1")' >/dev/null; then
        success "Titan Text Express model is available"
    else
        warning "Titan Text Express model not available. Request access in Bedrock console."
    fi
else
    warning "Could not check Bedrock model access. Ensure you have Bedrock permissions."
fi

# Validate CloudFormation template
info "Validating CloudFormation template..."
if aws cloudformation validate-template --template-body file://infrastructure/main-deployment.yaml --region "$REGION" >/dev/null 2>&1; then
    success "CloudFormation template is valid"
else
    error "CloudFormation template validation failed"
    exit 1
fi

# Check if stack already exists
info "Checking if stack already exists..."
if aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" >/dev/null 2>&1; then
    warning "Stack $STACK_NAME already exists. This will update the existing stack."
    read -p "Do you want to continue with the update? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        info "Deployment cancelled by user"
        exit 0
    fi
else
    info "Stack $STACK_NAME does not exist. Will create new stack."
fi

# Deploy the CloudFormation stack
info "Deploying CloudFormation stack..."
DEPLOY_START_TIME=$(date +%s)

aws cloudformation deploy \
    --template-file infrastructure/main-deployment.yaml \
    --stack-name "$STACK_NAME" \
    --region "$REGION" \
    --capabilities CAPABILITY_NAMED_IAM \
    --parameter-overrides \
        Environment="$ENVIRONMENT" \
        NotificationEmail="$NOTIFICATION_EMAIL" \
        KMSKeyAdminArn="$CURRENT_USER_ARN" \
    --tags \
        Application=AllSenses-AI-Guardian \
        Environment="$ENVIRONMENT" \
        DeployedBy="$(whoami)" \
        DeployedAt="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

DEPLOY_END_TIME=$(date +%s)
DEPLOY_DURATION=$((DEPLOY_END_TIME - DEPLOY_START_TIME))

success "CloudFormation stack deployed successfully in ${DEPLOY_DURATION} seconds!"

# Get stack outputs
info "Retrieving stack outputs..."
OUTPUTS=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" --query 'Stacks[0].Outputs' --output json)

API_ENDPOINT=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey == "ApiEndpoint") | .OutputValue')
AUDIO_ANALYSIS_ENDPOINT=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey == "AudioAnalysisEndpoint") | .OutputValue')
KMS_KEY_ID=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey == "KMSKeyId") | .OutputValue')
EMERGENCY_TOPIC_ARN=$(echo "$OUTPUTS" | jq -r '.[] | select(.OutputKey == "EmergencyTopicArn") | .OutputValue')

success "Deployment completed successfully!"
echo
info "=== DEPLOYMENT SUMMARY ==="
info "API Endpoint: $API_ENDPOINT"
info "Audio Analysis Endpoint: $AUDIO_ANALYSIS_ENDPOINT"
info "KMS Key ID: $KMS_KEY_ID"
info "Emergency Topic ARN: $EMERGENCY_TOPIC_ARN"
echo

# Save configuration to file
CONFIG_FILE="deployment-config.json"
cat > "$CONFIG_FILE" << EOF
{
  "environment": "$ENVIRONMENT",
  "region": "$REGION",
  "stackName": "$STACK_NAME",
  "apiEndpoint": "$API_ENDPOINT",
  "audioAnalysisEndpoint": "$AUDIO_ANALYSIS_ENDPOINT",
  "kmsKeyId": "$KMS_KEY_ID",
  "emergencyTopicArn": "$EMERGENCY_TOPIC_ARN",
  "deployedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "deploymentDuration": ${DEPLOY_DURATION}
}
EOF

success "Configuration saved to $CONFIG_FILE"

# Test the deployment
info "Testing deployment..."
TEST_PAYLOAD=$(cat << EOF
{
  "audioData": "TEST_AUDIO_SAMPLE",
  "userId": "test-user-$(date +%s)",
  "location": "Test Location",
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)"
}
EOF
)

info "Sending test request to: $AUDIO_ANALYSIS_ENDPOINT"

if RESPONSE=$(curl -s -X POST "$AUDIO_ANALYSIS_ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$TEST_PAYLOAD" \
    --max-time 30); then
    
    if echo "$RESPONSE" | jq -e '.success' >/dev/null 2>&1; then
        success "Test request successful!"
        ASSESSMENT_ID=$(echo "$RESPONSE" | jq -r '.assessmentId')
        THREAT_LEVEL=$(echo "$RESPONSE" | jq -r '.threatLevel')
        PROCESSING_TIME=$(echo "$RESPONSE" | jq -r '.processingTimeMs')
        info "Assessment ID: $ASSESSMENT_ID"
        info "Threat Level: $THREAT_LEVEL"
        info "Processing Time: ${PROCESSING_TIME}ms"
    else
        warning "Test request completed but returned success=false"
        info "Response: $RESPONSE"
    fi
else
    warning "Test request failed. This might be normal if Bedrock models need access approval."
fi

# Check SNS subscription
info "Checking SNS subscription..."
warning "Please check your email ($NOTIFICATION_EMAIL) and confirm the SNS subscription for emergency alerts."

# Security recommendations
echo
info "=== SECURITY RECOMMENDATIONS ==="
info "1. Confirm SNS email subscription for emergency alerts"
info "2. Review API Gateway access policies and restrict IP ranges if needed"
info "3. Monitor CloudWatch logs for unusual activity"
info "4. Regularly rotate KMS keys and review access policies"
info "5. Enable AWS CloudTrail for audit logging"
info "6. Set up billing alerts for cost monitoring"

# Next steps
echo
info "=== NEXT STEPS ==="
info "1. Update frontend configuration with new API endpoint:"
info "   API_URL = '$API_ENDPOINT'"
info "2. Test the frontend application with the deployed backend"
info "3. Configure monitoring and alerting in CloudWatch"
info "4. Set up backup and disaster recovery procedures"

success "AllSenses AI Guardian deployment completed successfully!"
info "Stack Name: $STACK_NAME"
info "Region: $REGION"
info "Environment: $ENVIRONMENT"
info "Total deployment time: ${DEPLOY_DURATION} seconds"

# Create a simple test script
cat > test-deployment.sh << 'EOF'
#!/bin/bash
# Quick test script for AllSenses deployment

if [[ ! -f "deployment-config.json" ]]; then
    echo "❌ deployment-config.json not found. Run deployment first."
    exit 1
fi

ENDPOINT=$(jq -r '.audioAnalysisEndpoint' deployment-config.json)

echo "🧪 Testing AllSenses deployment..."
echo "Endpoint: $ENDPOINT"

curl -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d '{
        "audioData": "EMERGENCY_HELP_ME",
        "userId": "test-user-emergency",
        "location": "Test Emergency Location"
    }' | jq '.'
EOF

chmod +x test-deployment.sh
success "Created test-deployment.sh for quick testing"