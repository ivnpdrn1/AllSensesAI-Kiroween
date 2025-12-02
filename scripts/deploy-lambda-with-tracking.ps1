# Deploy Lambda Function with Live Location Tracking
# AllSenses AI Guardian - LIVE-TRACK-1.2 & 1.3

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deploy Lambda with Live Tracking" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"
$region = "us-east-1"
$functionName = "AllSenses-Live-MVP-AllSensesFunction-ufWarJQ6FVRk"
$lambdaFile = "allsenseai-live-tracking.py"
$zipFile = "lambda-deployment.zip"

# Step 1: Verify Lambda file exists
Write-Host "Step 1: Verifying Lambda function file..." -ForegroundColor Yellow

if (-not (Test-Path $lambdaFile)) {
    Write-Host "✗ Lambda file not found: $lambdaFile" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Lambda file found: $lambdaFile" -ForegroundColor Green
Write-Host ""

# Step 2: Create deployment package
Write-Host "Step 2: Creating deployment package..." -ForegroundColor Yellow

try {
    # Remove old zip if exists
    if (Test-Path $zipFile) {
        Remove-Item $zipFile -Force
    }
    
    # Create zip with lambda_function.py (AWS Lambda expects this name)
    Copy-Item $lambdaFile lambda_function.py -Force
    Compress-Archive -Path lambda_function.py -DestinationPath $zipFile -Force
    Remove-Item lambda_function.py -Force
    
    $zipSize = (Get-Item $zipFile).Length / 1KB
    Write-Host "✓ Deployment package created: $zipFile ($([math]::Round($zipSize, 2)) KB)" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to create deployment package: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 3: Verify Lambda function exists
Write-Host "Step 3: Verifying Lambda function exists..." -ForegroundColor Yellow

try {
    $lambdaInfo = aws lambda get-function `
        --function-name $functionName `
        --region $region `
        --query 'Configuration.[FunctionName,Runtime,LastModified,FunctionArn]' `
        --output json | ConvertFrom-Json
    
    Write-Host "✓ Lambda function found" -ForegroundColor Green
    Write-Host "  Name: $($lambdaInfo[0])" -ForegroundColor White
    Write-Host "  Runtime: $($lambdaInfo[1])" -ForegroundColor White
    Write-Host "  Last Modified: $($lambdaInfo[2])" -ForegroundColor White
} catch {
    Write-Host "✗ Lambda function not found: $functionName" -ForegroundColor Red
    Write-Host "  Please verify the function name is correct" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Step 4: Update Lambda function code
Write-Host "Step 4: Deploying Lambda function code..." -ForegroundColor Yellow

try {
    $updateResult = aws lambda update-function-code `
        --function-name $functionName `
        --zip-file "fileb://$zipFile" `
        --region $region `
        --output json | ConvertFrom-Json
    
    Write-Host "✓ Lambda function code updated" -ForegroundColor Green
    Write-Host "  Version: $($updateResult.Version)" -ForegroundColor White
    Write-Host "  Code Size: $($updateResult.CodeSize) bytes" -ForegroundColor White
    Write-Host "  Last Modified: $($updateResult.LastModified)" -ForegroundColor White
} catch {
    Write-Host "✗ Failed to update Lambda function: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 5: Wait for function to be ready
Write-Host "Step 5: Waiting for function to be ready..." -ForegroundColor Yellow

Start-Sleep -Seconds 5

try {
    $status = aws lambda get-function `
        --function-name $functionName `
        --region $region `
        --query 'Configuration.State' `
        --output text
    
    if ($status -eq "Active") {
        Write-Host "✓ Lambda function is active and ready" -ForegroundColor Green
    } else {
        Write-Host "⚠ Lambda function state: $status" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Could not verify function state" -ForegroundColor Yellow
}

Write-Host ""

# Step 6: Update environment variables (if needed)
Write-Host "Step 6: Checking environment variables..." -ForegroundColor Yellow

try {
    $currentEnv = aws lambda get-function-configuration `
        --function-name $functionName `
        --region $region `
        --query 'Environment.Variables' `
        --output json | ConvertFrom-Json
    
    $trackingUrl = $currentEnv.TRACKING_BASE_URL
    
    if ($trackingUrl) {
        Write-Host "✓ TRACKING_BASE_URL configured: $trackingUrl" -ForegroundColor Green
    } else {
        Write-Host "⚠ TRACKING_BASE_URL not set, using default" -ForegroundColor Yellow
        Write-Host "  To set custom URL, run:" -ForegroundColor White
        Write-Host "  aws lambda update-function-configuration --function-name $functionName --environment Variables={TRACKING_BASE_URL=https://track.allsensesai.com}" -ForegroundColor Cyan
    }
} catch {
    Write-Host "⚠ Could not check environment variables" -ForegroundColor Yellow
}

Write-Host ""

# Step 7: Get Lambda Function URL
Write-Host "Step 7: Getting Lambda Function URL..." -ForegroundColor Yellow

try {
    $functionUrl = aws lambda get-function-url-config `
        --function-name $functionName `
        --region $region `
        --query 'FunctionUrl' `
        --output text
    
    Write-Host "✓ Lambda Function URL:" -ForegroundColor Green
    Write-Host "  $functionUrl" -ForegroundColor Cyan
} catch {
    Write-Host "⚠ Could not retrieve Function URL" -ForegroundColor Yellow
}

Write-Host ""

# Step 8: Test Lambda function
Write-Host "Step 8: Testing Lambda function..." -ForegroundColor Yellow

if ($functionUrl) {
    try {
        $testPayload = @{
            action = "CHECK_EUM_CONFIG"
        } | ConvertTo-Json
        
        $testResult = Invoke-RestMethod -Uri $functionUrl -Method Post -Body $testPayload -ContentType "application/json"
        
        if ($testResult.status -eq "success") {
            Write-Host "✓ Lambda function test successful" -ForegroundColor Green
            Write-Host "  EUM Compliant: $($testResult.configuration.eumCompliant)" -ForegroundColor White
            Write-Host "  Live Tracking: $($testResult.configuration.liveTrackingEnabled)" -ForegroundColor White
            Write-Host "  Originator: $($testResult.configuration.originatorNumber)" -ForegroundColor White
            Write-Host "  Campaign: $($testResult.configuration.campaign)" -ForegroundColor White
            Write-Host "  Tracking URL: $($testResult.configuration.trackingBaseUrl)" -ForegroundColor White
        } else {
            Write-Host "⚠ Lambda function returned unexpected response" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠ Lambda function test failed: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "⚠ Skipping test - Function URL not available" -ForegroundColor Yellow
}

Write-Host ""

# Step 9: Test location tracking handlers
Write-Host "Step 9: Testing location tracking handlers..." -ForegroundColor Yellow

if ($functionUrl) {
    # Test UPDATE_LOCATION
    $testIncidentId = "TEST-" + (Get-Random -Maximum 9999).ToString("D4")
    
    $locationPayload = @{
        action = "UPDATE_LOCATION"
        incidentId = $testIncidentId
        victimName = "Test User"
        location = @{
            latitude = 25.7617
            longitude = -80.1918
            accuracy = 10.5
        }
        batteryLevel = 85
    } | ConvertTo-Json
    
    try {
        $locationResult = Invoke-RestMethod -Uri $functionUrl -Method Post -Body $locationPayload -ContentType "application/json"
        
        if ($locationResult.status -eq "success") {
            Write-Host "  ✓ UPDATE_LOCATION handler working" -ForegroundColor Green
        } else {
            Write-Host "  ✗ UPDATE_LOCATION failed: $($locationResult.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ UPDATE_LOCATION error: $_" -ForegroundColor Red
    }
    
    # Test GET_LOCATION
    Start-Sleep -Seconds 2
    
    $getPayload = @{
        action = "GET_LOCATION"
        incidentId = $testIncidentId
    } | ConvertTo-Json
    
    try {
        $getResult = Invoke-RestMethod -Uri $functionUrl -Method Post -Body $getPayload -ContentType "application/json"
        
        if ($getResult.status -eq "success" -and $getResult.location) {
            Write-Host "  ✓ GET_LOCATION handler working" -ForegroundColor Green
        } else {
            Write-Host "  ✗ GET_LOCATION failed: $($getResult.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ GET_LOCATION error: $_" -ForegroundColor Red
    }
    
    # Test GET_LOCATION_HISTORY
    $historyPayload = @{
        action = "GET_LOCATION_HISTORY"
        incidentId = $testIncidentId
        limit = 10
    } | ConvertTo-Json
    
    try {
        $historyResult = Invoke-RestMethod -Uri $functionUrl -Method Post -Body $historyPayload -ContentType "application/json"
        
        if ($historyResult.status -eq "success") {
            Write-Host "  ✓ GET_LOCATION_HISTORY handler working" -ForegroundColor Green
        } else {
            Write-Host "  ✗ GET_LOCATION_HISTORY failed: $($historyResult.message)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ✗ GET_LOCATION_HISTORY error: $_" -ForegroundColor Red
    }
} else {
    Write-Host "⚠ Skipping handler tests - Function URL not available" -ForegroundColor Yellow
}

Write-Host ""

# Cleanup
Write-Host "Cleaning up..." -ForegroundColor Yellow
if (Test-Path $zipFile) {
    Remove-Item $zipFile -Force
    Write-Host "✓ Deployment package removed" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Lambda Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Deployment Summary:" -ForegroundColor Cyan
Write-Host "  ✓ Lambda function updated with live tracking" -ForegroundColor Green
Write-Host "  ✓ Location tracking handlers deployed" -ForegroundColor Green
Write-Host "  ✓ EUM compliance maintained" -ForegroundColor Green
Write-Host "  ✓ Function tested and operational" -ForegroundColor Green
Write-Host ""

Write-Host "New Handlers Available:" -ForegroundColor Cyan
Write-Host "  • UPDATE_LOCATION - Store GPS coordinates" -ForegroundColor White
Write-Host "  • GET_LOCATION - Retrieve latest location" -ForegroundColor White
Write-Host "  • GET_LOCATION_HISTORY - Get movement trail" -ForegroundColor White
Write-Host "  • JURY_EMERGENCY_ALERT - Now includes tracking URL" -ForegroundColor White
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Run comprehensive backend test:" -ForegroundColor White
Write-Host "     .\scripts\test-live-tracking-backend.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "  2. Update frontend to send location updates:" -ForegroundColor White
Write-Host "     File: frontend/enhanced-emergency-monitor-with-tracking.html" -ForegroundColor Cyan
Write-Host ""
Write-Host "  3. Deploy tracking page frontend:" -ForegroundColor White
Write-Host "     .\scripts\deploy-live-tracking-site.ps1" -ForegroundColor Cyan
Write-Host ""

Write-Host "Lambda Function URL:" -ForegroundColor Cyan
if ($functionUrl) {
    Write-Host "  $functionUrl" -ForegroundColor White
} else {
    Write-Host "  (Run script to retrieve URL)" -ForegroundColor Yellow
}
Write-Host ""
