$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "KiroDeployRole Setup"
Write-Host "========================================"
Write-Host ""

$accountId = "794289527784"
Write-Host "Account ID: $accountId"
Write-Host ""

Write-Host "Updating trust policy..."
$trustPolicy = Get-Content "iam/KiroDeployRole-trust-policy.json" -Raw
$trustPolicy = $trustPolicy -replace "ACCOUNT_ID", $accountId
$trustPolicy | Set-Content "iam/KiroDeployRole-trust-policy-final.json"
Write-Host "Trust policy updated"
Write-Host ""

Write-Host "Creating IAM Role: KiroDeployRole..."
$ErrorActionPreference = "Continue"
$roleCheck = aws iam get-role --role-name KiroDeployRole 2>&1 | Out-Null
$ErrorActionPreference = "Stop"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Role does not exist, creating..."
    aws iam create-role --role-name KiroDeployRole --assume-role-policy-document file://iam/KiroDeployRole-trust-policy-final.json --description "Kiro deployment role" --tags Key=Project,Value=AllSensesAI | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Role created successfully"
    } else {
        Write-Host "Failed to create role"
        exit 1
    }
} else {
    Write-Host "Role already exists, skipping creation"
}
Write-Host ""

Write-Host "Attaching policy..."
aws iam put-role-policy --role-name KiroDeployRole --policy-name KiroDeployPolicy --policy-document file://iam/KiroDeployRole-policy.json
if ($LASTEXITCODE -eq 0) {
    Write-Host "Policy attached"
} else {
    Write-Host "Failed to attach policy"
    exit 1
}
Write-Host ""

$roleArn = aws iam get-role --role-name KiroDeployRole --query 'Role.Arn' --output text
Write-Host "========================================"
Write-Host "Setup Complete!"
Write-Host "========================================"
Write-Host ""
Write-Host "Role ARN:"
Write-Host "  $roleArn"
Write-Host ""

if (Test-Path "iam/KiroDeployRole-trust-policy-final.json") {
    Remove-Item "iam/KiroDeployRole-trust-policy-final.json" -Force
}
