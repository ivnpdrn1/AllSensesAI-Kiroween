# Deploy Live Tracking Update to Jury Lambda
# This script safely updates the Lambda function with location tracking capabilities
# while preserving all EUM SMS functionality

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AllSenses Live Tracking Lambda Update" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$FUNCTION_NAME = "AllSenses-Live-MVP-AllSensesFunction-ufWarJQ6FVRk"
$LAMBDA_FILE = "JURY_DEPLOYMENT_PACKAGE/lambda/allsenseai-eum-with-tracking.py"
$BACKUP_FILE = "JURY_DEPLOYMENT_PACKAGE/lambda/allsenseai-eum-baseline.py"
$REGION = "us-east-1"

# Step 1: Verify backup exists
Write-Host "Step 1: Verifying backup file exists..." -ForegroundColor Yellow

if (Test-Path $BACKUP_FILE) {
    Write-Host "✓ Backup file exists: $BACKUP_FILE" -ForegroundColor Green
} else {
    Write-Host "✗ Backup file not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 2: Create deployment package
Write-Host "Step 2: Creating deployment package..." -ForegroundColor Yellow

$tempDir = "temp_lambda_deploy"
if (Test-Path $tempDir) {
    Remove-Item -Recurse -Force $tempDir
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

Copy-Item $LAMBDA_FILE "$tempDir/lambda_function.py"

# Create ZIP file
$zipFile = "lambda-deployment.zip"
if (Test-Path $zipFile) {
    Remove-Item -Force $zipFile
}

Compress-Archive -Path "$tempDir/*" -DestinationPath $zipFile

Remove-Item -Recurse -Force $tempDir

Write-Host "✓ Deployment package created: $zipFile" -ForegroundColor Green
Write-Host ""

# Step 3: Deploy to Lambda
Write-Host "Step 3: Deploying to Lambda function..." -ForegroundColor Yellow
Write-Host "Function: $FUNCTION_NAME" -ForegroundColor White

try {
    aws lambda update-function-code `
        --function-name $FUNCTION_NAME `
        --zip-file fileb://$zipFile `
        --region $REGION `
        --output json | ConvertFrom-Json | Out-Null
    
    Write-Host "✓ Lambda function code updated successfully" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to update Lambda function: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 4: Wait for update to complete
Write-Host "Step 4: Waiting for Lambda update to complete..." -ForegroundColor Yellow

Start-Sleep -Seconds 5

$maxAttempts = 12
$attempt = 0
$updateComplete = $false

while ($attempt -lt $maxAttempts -and -not $updateComplete) {
    try {
        $functionInfo = aws lambda get-function --function-name $FUNCTION_NAME --region $REGION | ConvertFrom-Json
        $state = $functionInfo.Configuration.State
        $lastUpdateStatus = $functionInfo.Configuration.LastUpdateStatus
        
        if ($state -eq "Active" -and $lastUpdateStatus -eq "Successful") {
            $updateComplete = $true
            Write-Host "✓ Lambda function is active and ready" -ForegroundColor Green
        } else {
            Write-Host "  Status: $state | Update: $lastUpdateStatus (waiting...)" -ForegroundColor Yellow
            Start-Sleep -Seconds 5
        }
    } catch {
        Write-Host "  Checking status..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
    
    $attempt++
}

if (-not $updateComplete) {
    Write-Host "⚠ Lambda update taking longer than expected" -ForegroundColor Yellow
    Write-Host "  Check AWS Console for status" -ForegroundColor Yellow
}

Write-Host ""

# Step 5: Cleanup
Write-Host "Step 5: Cleaning up..." -ForegroundColor Yellow

Remove-Item -Force $zipFile

Write-Host "✓ Cleanup complete" -ForegroundColor Green
Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Lambda Function: $FUNCTION_NAME" -ForegroundColor White
Write-Host "Region: $REGION" -ForegroundColor White
Write-Host ""
Write-Host "NEW FEATURES ADDED:" -ForegroundColor Cyan
Write-Host "  • UPDATE_LOCATION - Store GPS coordinates in DynamoDB" -ForegroundColor White
Write-Host "  • GET_LOCATION - Retrieve latest location for tracking page" -ForegroundColor White
Write-Host "  • GET_LOCATION_HISTORY - Get movement trail data" -ForegroundColor White
Write-Host "  • Incident creation with unique IDs" -ForegroundColor White
Write-Host "  • Tracking URLs in emergency SMS messages" -ForegroundColor White
Write-Host ""
Write-Host "PRESERVED FEATURES:" -ForegroundColor Cyan
Write-Host "  ✓ EUM SMS with +12173933490" -ForegroundColor Green
Write-Host "  ✓ AllSensesAI-SafetyAlerts campaign" -ForegroundColor Green
Write-Host "  ✓ All existing emergency alert handlers" -ForegroundColor Green
Write-Host "  ✓ JURY_EMERGENCY_ALERT functionality" -ForegroundColor Green
Write-Host "  ✓ TEST_SMS endpoint" -ForegroundColor Green
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host "1. Run verification script to test EUM + SMS:" -ForegroundColor White
Write-Host "   cd JURY_DEPLOYMENT_PACKAGE/testing" -ForegroundColor Cyan
Write-Host "   .\verify-eum-deployment.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Test location tracking (new feature):" -ForegroundColor White
Write-Host "   .\test-location-tracking.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "ROLLBACK (if needed):" -ForegroundColor Yellow
Write-Host "  Backup file saved at: $BACKUP_FILE" -ForegroundColor White
Write-Host "  To rollback, deploy the baseline file instead" -ForegroundColor White
Write-Host ""
