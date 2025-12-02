# AllSenses AI Guardian - AWS API Gateway Deployment Script (PowerShell)
# This script deploys the API Gateway infrastructure for the MVP

param(
    [string]$Environment = "dev",
    [string]$Region = "us-east-1",
    [string]$ApplicationName = "allsenses-ai-guardian"
)

# Configuration
$StackName = "allsenses-api-gateway"
$TemplateFile = "infrastructure/api-gateway.yaml"

Write-Host "🚀 Deploying AllSenses AI Guardian API Gateway..." -ForegroundColor Green
Write-Host "Stack Name: $StackName" -ForegroundColor Cyan
Write-Host "Environment: $Environment" -ForegroundColor Cyan
Write-Host "Region: $Region" -ForegroundColor Cyan

# Check if AWS CLI is installed
try {
    aws --version | Out-Null
} catch {
    Write-Host "❌ AWS CLI is not installed. Please install it first." -ForegroundColor Red
    exit 1
}

# Check if user is authenticated
try {
    aws sts get-caller-identity | Out-Null
} catch {
    Write-Host "❌ AWS credentials not configured. Please run 'aws configure' first." -ForegroundColor Red
    exit 1
}

# Validate CloudFormation template
Write-Host "📋 Validating CloudFormation template..." -ForegroundColor Yellow
try {
    aws cloudformation validate-template --template-body file://$TemplateFile --region $Region
    Write-Host "✅ Template validation successful" -ForegroundColor Green
} catch {
    Write-Host "❌ Template validation failed" -ForegroundColor Red
    exit 1
}

# Deploy the stack
Write-Host "🔧 Deploying API Gateway stack..." -ForegroundColor Yellow
try {
    aws cloudformation deploy `
        --template-file $TemplateFile `
        --stack-name $StackName `
        --parameter-overrides Environment=$Environment ApplicationName=$ApplicationName `
        --capabilities CAPABILITY_IAM `
        --region $Region `
        --no-fail-on-empty-changeset
    
    Write-Host "✅ Stack deployment successful" -ForegroundColor Green
} catch {
    Write-Host "❌ Stack deployment failed" -ForegroundColor Red
    exit 1
}

# Get stack outputs
Write-Host "📊 Getting deployment outputs..." -ForegroundColor Yellow

$ApiGatewayUrl = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayUrl`].OutputValue' `
    --output text

$ThreatDetectionEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`ThreatDetectionEndpoint`].OutputValue' `
    --output text

$EmergencyEventEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`EmergencyEventEndpoint`].OutputValue' `
    --output text

$UserManagementEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`UserManagementEndpoint`].OutputValue' `
    --output text

$AiAgentWorkflowEndpoint = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`AiAgentWorkflowEndpoint`].OutputValue' `
    --output text

Write-Host ""
Write-Host "✅ API Gateway deployment completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📡 API Gateway Endpoints:" -ForegroundColor Cyan
Write-Host "  Base URL: $ApiGatewayUrl" -ForegroundColor White
Write-Host ""
Write-Host "🎯 AI Agent Endpoints:" -ForegroundColor Cyan
Write-Host "  Threat Detection: $ThreatDetectionEndpoint" -ForegroundColor White
Write-Host "  Emergency Events: $EmergencyEventEndpoint" -ForegroundColor White
Write-Host "  User Management: $UserManagementEndpoint" -ForegroundColor White
Write-Host "  Complete Workflow: $AiAgentWorkflowEndpoint" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Deploy your Spring Boot application to ECS or EC2" -ForegroundColor White
Write-Host "  2. Update the Load Balancer target group with your application instances" -ForegroundColor White
Write-Host "  3. Test the API endpoints using the URLs above" -ForegroundColor White
Write-Host "  4. Configure your frontend to use these API Gateway endpoints" -ForegroundColor White
Write-Host ""
Write-Host "📝 Example API Test (PowerShell):" -ForegroundColor Yellow
Write-Host "  Invoke-RestMethod -Uri '$ThreatDetectionEndpoint' -Method Post -ContentType 'application/json' -Body '{\"userId\":\"test-user\",\"location\":\"Test Location\",\"audioData\":\"test-audio\"}'" -ForegroundColor White