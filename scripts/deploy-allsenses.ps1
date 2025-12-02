# AllSenses AI Guardian - Secure AWS Deployment Script
# PowerShell script for Windows deployment with security best practices

param(
    [Parameter(Mandatory=$true)]
    [string]$NotificationEmail,
    
    [Parameter(Mandatory=$false)]
    [string]$Environment = "production",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1",
    
    [Parameter(Mandatory=$false)]
    [string]$StackName = "AllSenses-AI-Guardian",
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipPrerequisites = $false
)

# Set error handling
$ErrorActionPreference = "Stop"

# Color functions for output
function Write-Success { param($Message) Write-Host "✅ $Message" -ForegroundColor Green }
function Write-Info { param($Message) Write-Host "ℹ️  $Message" -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host "⚠️  $Message" -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host "❌ $Message" -ForegroundColor Red }

Write-Info "Starting AllSenses AI Guardian deployment to AWS..."
Write-Info "Environment: $Environment"
Write-Info "Region: $Region"
Write-Info "Notification Email: $NotificationEmail"

# Validate email format
if ($NotificationEmail -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') {
    Write-Error "Invalid email format: $NotificationEmail"
    exit 1
}

# Check prerequisites
if (-not $SkipPrerequisites) {
    Write-Info "Checking prerequisites..."
    
    # Check AWS CLI
    try {
        $awsVersion = aws --version 2>$null
        Write-Success "AWS CLI found: $awsVersion"
    } catch {
        Write-Error "AWS CLI not found. Please install AWS CLI first."
        Write-Info "Download from: https://aws.amazon.com/cli/"
        exit 1
    }
    
    # Check AWS credentials
    try {
        $identity = aws sts get-caller-identity --output json | ConvertFrom-Json
        Write-Success "AWS credentials configured for account: $($identity.Account)"
        Write-Info "User/Role: $($identity.Arn)"
    } catch {
        Write-Error "AWS credentials not configured. Run 'aws configure' first."
        exit 1
    }
    
    # Validate region
    try {
        aws ec2 describe-regions --region $Region --output table >$null 2>&1
        Write-Success "Region $Region is valid and accessible"
    } catch {
        Write-Error "Invalid or inaccessible region: $Region"
        exit 1
    }
}

# Get current user ARN for KMS key administration
try {
    $currentUserArn = (aws sts get-caller-identity --query 'Arn' --output text)
    Write-Info "Using current user/role for KMS key administration: $currentUserArn"
} catch {
    Write-Error "Failed to get current user ARN"
    exit 1
}

# Check if Bedrock models are available
Write-Info "Checking AWS Bedrock model access..."
try {
    $models = aws bedrock list-foundation-models --region $Region --output json | ConvertFrom-Json
    $claudeModel = $models.modelSummaries | Where-Object { $_.modelId -eq "anthropic.claude-3-sonnet-20240229-v1:0" }
    $titanModel = $models.modelSummaries | Where-Object { $_.modelId -eq "amazon.titan-text-express-v1" }
    
    if ($claudeModel) {
        Write-Success "Claude-3 Sonnet model is available"
    } else {
        Write-Warning "Claude-3 Sonnet model not available. Request access in Bedrock console."
    }
    
    if ($titanModel) {
        Write-Success "Titan Text Express model is available"
    } else {
        Write-Warning "Titan Text Express model not available. Request access in Bedrock console."
    }
} catch {
    Write-Warning "Could not check Bedrock model access. Ensure you have Bedrock permissions."
}

# Validate CloudFormation template
Write-Info "Validating CloudFormation template..."
try {
    aws cloudformation validate-template --template-body file://infrastructure/main-deployment.yaml --region $Region >$null
    Write-Success "CloudFormation template is valid"
} catch {
    Write-Error "CloudFormation template validation failed"
    exit 1
}

# Check if stack already exists
Write-Info "Checking if stack already exists..."
try {
    $existingStack = aws cloudformation describe-stacks --stack-name $StackName --region $Region --output json 2>$null | ConvertFrom-Json
    if ($existingStack) {
        Write-Warning "Stack $StackName already exists. This will update the existing stack."
        $confirmation = Read-Host "Do you want to continue with the update? (y/N)"
        if ($confirmation -ne 'y' -and $confirmation -ne 'Y') {
            Write-Info "Deployment cancelled by user"
            exit 0
        }
    }
} catch {
    Write-Info "Stack $StackName does not exist. Will create new stack."
}

# Deploy the CloudFormation stack
Write-Info "Deploying CloudFormation stack..."
try {
    $deployCommand = @(
        "aws", "cloudformation", "deploy",
        "--template-file", "infrastructure/main-deployment.yaml",
        "--stack-name", $StackName,
        "--region", $Region,
        "--capabilities", "CAPABILITY_NAMED_IAM",
        "--parameter-overrides",
        "Environment=$Environment",
        "NotificationEmail=$NotificationEmail",
        "KMSKeyAdminArn=$currentUserArn",
        "--tags",
        "Application=AllSenses-AI-Guardian",
        "Environment=$Environment",
        "DeployedBy=$env:USERNAME",
        "DeployedAt=$(Get-Date -Format 'yyyy-MM-ddTHH:mm:ssZ')"
    )
    
    Write-Info "Executing: $($deployCommand -join ' ')"
    & $deployCommand[0] $deployCommand[1..($deployCommand.Length-1)]
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "CloudFormation stack deployed successfully!"
    } else {
        Write-Error "CloudFormation deployment failed with exit code $LASTEXITCODE"
        exit 1
    }
} catch {
    Write-Error "Failed to deploy CloudFormation stack: $($_.Exception.Message)"
    exit 1
}

# Get stack outputs
Write-Info "Retrieving stack outputs..."
try {
    $outputs = aws cloudformation describe-stacks --stack-name $StackName --region $Region --query 'Stacks[0].Outputs' --output json | ConvertFrom-Json
    
    $apiEndpoint = ($outputs | Where-Object { $_.OutputKey -eq "ApiEndpoint" }).OutputValue
    $audioAnalysisEndpoint = ($outputs | Where-Object { $_.OutputKey -eq "AudioAnalysisEndpoint" }).OutputValue
    $kmsKeyId = ($outputs | Where-Object { $_.OutputKey -eq "KMSKeyId" }).OutputValue
    $emergencyTopicArn = ($outputs | Where-Object { $_.OutputKey -eq "EmergencyTopicArn" }).OutputValue
    
    Write-Success "Deployment completed successfully!"
    Write-Info ""
    Write-Info "=== DEPLOYMENT SUMMARY ==="
    Write-Info "API Endpoint: $apiEndpoint"
    Write-Info "Audio Analysis Endpoint: $audioAnalysisEndpoint"
    Write-Info "KMS Key ID: $kmsKeyId"
    Write-Info "Emergency Topic ARN: $emergencyTopicArn"
    Write-Info ""
    
    # Save configuration to file
    $config = @{
        Environment = $Environment
        Region = $Region
        StackName = $StackName
        ApiEndpoint = $apiEndpoint
        AudioAnalysisEndpoint = $audioAnalysisEndpoint
        KMSKeyId = $kmsKeyId
        EmergencyTopicArn = $emergencyTopicArn
        DeployedAt = Get-Date -Format 'yyyy-MM-ddTHH:mm:ssZ'
    }
    
    $configJson = $config | ConvertTo-Json -Depth 10
    $configJson | Out-File -FilePath "deployment-config.json" -Encoding UTF8
    Write-Success "Configuration saved to deployment-config.json"
    
} catch {
    Write-Error "Failed to retrieve stack outputs: $($_.Exception.Message)"
    exit 1
}

# Test the deployment
Write-Info "Testing deployment..."
try {
    $testPayload = @{
        audioData = "TEST_AUDIO_SAMPLE"
        userId = "test-user-$(Get-Random)"
        location = "Test Location"
        timestamp = (Get-Date -Format 'yyyy-MM-ddTHH:mm:ss.fffZ')
    } | ConvertTo-Json
    
    Write-Info "Sending test request to: $audioAnalysisEndpoint"
    
    $response = Invoke-RestMethod -Uri $audioAnalysisEndpoint -Method POST -Body $testPayload -ContentType "application/json" -TimeoutSec 30
    
    if ($response.success) {
        Write-Success "Test request successful!"
        Write-Info "Assessment ID: $($response.assessmentId)"
        Write-Info "Threat Level: $($response.threatLevel)"
        Write-Info "Processing Time: $($response.processingTimeMs)ms"
    } else {
        Write-Warning "Test request completed but returned success=false"
        Write-Info "Response: $($response | ConvertTo-Json)"
    }
} catch {
    Write-Warning "Test request failed: $($_.Exception.Message)"
    Write-Info "This might be normal if Bedrock models need access approval"
}

# Check SNS subscription
Write-Info "Checking SNS subscription..."
Write-Warning "Please check your email ($NotificationEmail) and confirm the SNS subscription for emergency alerts."

# Security recommendations
Write-Info ""
Write-Info "=== SECURITY RECOMMENDATIONS ==="
Write-Info "1. Confirm SNS email subscription for emergency alerts"
Write-Info "2. Review API Gateway access policies and restrict IP ranges if needed"
Write-Info "3. Monitor CloudWatch logs for unusual activity"
Write-Info "4. Regularly rotate KMS keys and review access policies"
Write-Info "5. Enable AWS CloudTrail for audit logging"
Write-Info "6. Set up billing alerts for cost monitoring"

# Next steps
Write-Info ""
Write-Info "=== NEXT STEPS ==="
Write-Info "1. Update frontend configuration with new API endpoint:"
Write-Info "   API_URL = '$apiEndpoint'"
Write-Info "2. Test the frontend application with the deployed backend"
Write-Info "3. Configure monitoring and alerting in CloudWatch"
Write-Info "4. Set up backup and disaster recovery procedures"

Write-Success "AllSenses AI Guardian deployment completed successfully!"
Write-Info "Stack Name: $StackName"
Write-Info "Region: $Region"
Write-Info "Environment: $Environment"