# One-Time Setup: Create KiroDeployRole
# Run this once to set up the deployment role

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "KiroDeployRole Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get AWS Account ID
Write-Host "Getting AWS Account ID..." -ForegroundColor Yellow
$accountId = aws sts get-caller-identity --query Account --output text

if (-not $accountId) {
    Write-Host "✗ Failed to get AWS Account ID" -ForegroundColor Red
    Write-Host "Please configure AWS CLI first: aws configure" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Account ID: $accountId" -ForegroundColor Green
Write-Host ""

# Update trust policy with account ID
Write-Host "Updating trust policy with Account ID..." -ForegroundColor Yellow
$trustPolicy = Get-Content "iam/KiroDeployRole-trust-policy.json" -Raw
$trustPolicy = $trustPolicy -replace "ACCOUNT_ID", $accountId
$trustPolicy | Set-Content "iam/KiroDeployRole-trust-policy-final.json"
Write-Host "✓ Trust policy updated" -ForegroundColor Green
Write-Host ""

# Create IAM Role
Write-Host "Creating IAM Role: KiroDeployRole..." -ForegroundColor Yellow

try {
    aws iam create-role `
        --role-name KiroDeployRole `
        --assume-role-policy-document file://iam/KiroDeployRole-trust-policy-final.json `
        --description "Kiro autonomous deployment role for AllSensesAI" `
        --tags Key=Project,Value=AllSensesAI Key=ManagedBy,Value=Kiro
    
    Write-Host "✓ Role created" -ForegroundColor Green
} catch {
    if ($_.Exception.Message -like "*EntityAlreadyExists*") {
        Write-Host "⚠ Role already exists, updating..." -ForegroundColor Yellow
    } else {
        Write-Host "✗ Failed to create role: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Create and attach inline policy
Write-Host "Attaching deployment policy..." -ForegroundColor Yellow

aws iam put-role-policy `
    --role-name KiroDeployRole `
    --policy-name KiroDeployPolicy `
    --policy-document file://iam/KiroDeployRole-policy.json

Write-Host "✓ Policy attached" -ForegroundColor Green
Write-Host ""

# Get role ARN
$roleArn = aws iam get-role --role-name KiroDeployRole --query 'Role.Arn' --output text

Write-Host "========================================" -ForegroundColor Green
Write-Host "✓ Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "Role ARN:" -ForegroundColor Cyan
Write-Host "  $roleArn" -ForegroundColor White
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Configure AWS profile to use this role:" -ForegroundColor White
Write-Host ""
Write-Host "   Add to ~/.aws/config:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   [profile kiro-deploy]" -ForegroundColor White
Write-Host "   role_arn = $roleArn" -ForegroundColor White
Write-Host "   source_profile = default" -ForegroundColor White
Write-Host ""
Write-Host "2. Test the profile:" -ForegroundColor White
Write-Host "   aws sts get-caller-identity --profile kiro-deploy" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Run deployments:" -ForegroundColor White
Write-Host "   .\deploy-allsensesai.ps1 -Component fix-sms" -ForegroundColor Cyan
Write-Host ""

# Cleanup
Remove-Item "iam/KiroDeployRole-trust-policy-final.json" -Force -ErrorAction SilentlyContinue
