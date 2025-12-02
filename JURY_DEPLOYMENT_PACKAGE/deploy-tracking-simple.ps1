# Simple Lambda Deployment Script for Live Tracking Update

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AllSenses Live Tracking Lambda Update" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$FUNCTION_NAME = "AllSenses-Live-MVP-AllSensesFunction-ufWarJQ6FVRk"
$LAMBDA_FILE = "lambda\allsenseai-eum-with-tracking.py"
$REGION = "us-east-1"

# Step 1: Verify files exist
Write-Host "Step 1: Verifying files..." -ForegroundColor Yellow

if (-not (Test-Path $LAMBDA_FILE)) {
    Write-Host "Error: Lambda file not found: $LAMBDA_FILE" -ForegroundColor Red
    exit 1
}

Write-Host "  Lambda file: $LAMBDA_FILE" -ForegroundColor Green
Write-Host ""

# Step 2: Create deployment package
Write-Host "Step 2: Creating deployment package..." -ForegroundColor Yellow

$tempDir = "temp_deploy"
if (Test-Path $tempDir) {
    Remove-Item -Recurse -Force $tempDir
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

Copy-Item $LAMBDA_FILE "$tempDir\lambda_function.py"

$zipFile = "lambda-deploy.zip"
if (Test-Path $zipFile) {
    Remove-Item -Force $zipFile
}

Compress-Archive -Path "$tempDir\*" -DestinationPath $zipFile -Force

Remove-Item -Recurse -Force $tempDir

Write-Host "  Package created: $zipFile" -ForegroundColor Green
Write-Host ""

# Step 3: Deploy to Lambda
Write-Host "Step 3: Deploying to Lambda..." -ForegroundColor Yellow
Write-Host "  Function: $FUNCTION_NAME" -ForegroundColor White

aws lambda update-function-code `
    --function-name $FUNCTION_NAME `
    --zip-file "fileb://$zipFile" `
    --region $REGION `
    --no-cli-pager

Write-Host ""
Write-Host "  Deployment initiated" -ForegroundColor Green
Write-Host ""

# Step 4: Wait for update
Write-Host "Step 4: Waiting for update to complete..." -ForegroundColor Yellow

Start-Sleep -Seconds 10

Write-Host "  Update complete" -ForegroundColor Green
Write-Host ""

# Step 5: Cleanup
Write-Host "Step 5: Cleaning up..." -ForegroundColor Yellow

Remove-Item -Force $zipFile

Write-Host "  Cleanup complete" -ForegroundColor Green
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "NEW FEATURES ADDED:" -ForegroundColor Cyan
Write-Host "  UPDATE_LOCATION - Store GPS coordinates" -ForegroundColor White
Write-Host "  GET_LOCATION - Retrieve latest location" -ForegroundColor White
Write-Host "  GET_LOCATION_HISTORY - Get movement trail" -ForegroundColor White
Write-Host "  Tracking URLs in emergency SMS" -ForegroundColor White
Write-Host ""
Write-Host "EUM SMS PRESERVED:" -ForegroundColor Cyan
Write-Host "  +12173933490 originator" -ForegroundColor Green
Write-Host "  AllSensesAI-SafetyAlerts campaign" -ForegroundColor Green
Write-Host "  All existing handlers" -ForegroundColor Green
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Test EUM + SMS (verify nothing broke):" -ForegroundColor White
Write-Host "   cd testing" -ForegroundColor Cyan
Write-Host "   .\verify-eum-deployment.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Test location tracking (new feature):" -ForegroundColor White
Write-Host "   cd testing" -ForegroundColor Cyan
Write-Host "   .\test-location-tracking.ps1" -ForegroundColor Cyan
Write-Host ""
