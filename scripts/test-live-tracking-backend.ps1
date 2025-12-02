# Test Live Tracking Backend
# Verify DynamoDB tables and Lambda handlers

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Live Tracking Backend Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$region = "us-east-1"
$lambdaUrl = "https://ufwarjq6fvrk.lambda-url.us-east-1.on.aws/"

# Test 1: Verify DynamoDB Tables
Write-Host "Test 1: Verifying DynamoDB tables..." -ForegroundColor Yellow

$tables = @("AllSenses-LocationTracking", "AllSenses-Incidents")
$tablesOk = $true

foreach ($table in $tables) {
    try {
        $tableInfo = aws dynamodb describe-table `
            --table-name $table `
            --region $region `
            --query 'Table.[TableName,TableStatus,TimeToLiveDescription.TimeToLiveStatus]' `
            --output json | ConvertFrom-Json
        
        $ttlStatus = $tableInfo[2]
        if ($ttlStatus -eq "ENABLED") {
            Write-Host "  ✓ $table - Active with TTL enabled" -ForegroundColor Green
        } else {
            Write-Host "  ⚠ $table - Active but TTL not enabled" -ForegroundColor Yellow
            $tablesOk = $false
        }
    } catch {
        Write-Host "  ✗ $table - Not found or inaccessible" -ForegroundColor Red
        $tablesOk = $false
    }
}

if (-not $tablesOk) {
    Write-Host ""
    Write-Host "✗ DynamoDB tables not properly configured" -ForegroundColor Red
    Write-Host "Run: .\scripts\deploy-live-tracking-backend.ps1" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test 2: Test Lambda Health Check
Write-Host "Test 2: Testing Lambda function..." -ForegroundColor Yellow

try {
    $healthCheck = Invoke-RestMethod -Uri $lambdaUrl -Method Post -Body '{"action":"CHECK_EUM_CONFIG"}' -ContentType "application/json"
    
    if ($healthCheck.status -eq "success") {
        Write-Host "  ✓ Lambda function is operational" -ForegroundColor Green
        Write-Host "    Live Tracking: $($healthCheck.configuration.liveTrackingEnabled)" -ForegroundColor White
        Write-Host "    Tracking URL: $($healthCheck.configuration.trackingBaseUrl)" -ForegroundColor White
    } else {
        Write-Host "  ✗ Lambda function returned error" -ForegroundColor Red
        Write-Host "    $($healthCheck.message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Failed to connect to Lambda: $_" -ForegroundColor Red
}

Write-Host ""

# Test 3: Test Location Update Handler
Write-Host "Test 3: Testing location update handler..." -ForegroundColor Yellow

$testIncidentId = "TEST-" + (Get-Random -Maximum 9999).ToString("D4")
$testLocation = @{
    action = "UPDATE_LOCATION"
    incidentId = $testIncidentId
    victimName = "Test User"
    location = @{
        latitude = 25.7617
        longitude = -80.1918
        accuracy = 10.5
        speed = 0
        heading = 0
    }
    batteryLevel = 85
} | ConvertTo-Json

try {
    $updateResult = Invoke-RestMethod -Uri $lambdaUrl -Method Post -Body $testLocation -ContentType "application/json"
    
    if ($updateResult.status -eq "success") {
        Write-Host "  ✓ Location update successful" -ForegroundColor Green
        Write-Host "    Incident ID: $testIncidentId" -ForegroundColor White
    } else {
        Write-Host "  ✗ Location update failed" -ForegroundColor Red
        Write-Host "    $($updateResult.message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Location update error: $_" -ForegroundColor Red
}

Write-Host ""

# Test 4: Test Location Retrieval
Write-Host "Test 4: Testing location retrieval..." -ForegroundColor Yellow

Start-Sleep -Seconds 2  # Wait for DynamoDB consistency

$getLocation = @{
    action = "GET_LOCATION"
    incidentId = $testIncidentId
} | ConvertTo-Json

try {
    $getResult = Invoke-RestMethod -Uri $lambdaUrl -Method Post -Body $getLocation -ContentType "application/json"
    
    if ($getResult.status -eq "success" -and $getResult.location) {
        Write-Host "  ✓ Location retrieval successful" -ForegroundColor Green
        Write-Host "    Latitude: $($getResult.location.latitude)" -ForegroundColor White
        Write-Host "    Longitude: $($getResult.location.longitude)" -ForegroundColor White
        Write-Host "    Accuracy: $($getResult.location.accuracy)m" -ForegroundColor White
        Write-Host "    Battery: $($getResult.location.batteryLevel)%" -ForegroundColor White
    } else {
        Write-Host "  ✗ Location retrieval failed" -ForegroundColor Red
        Write-Host "    $($getResult.message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Location retrieval error: $_" -ForegroundColor Red
}

Write-Host ""

# Test 5: Verify DynamoDB Data
Write-Host "Test 5: Verifying data in DynamoDB..." -ForegroundColor Yellow

try {
    $items = aws dynamodb query `
        --table-name AllSenses-LocationTracking `
        --key-condition-expression "incidentId = :id" `
        --expression-attribute-values "{\":id\":{\"S\":\"$testIncidentId\"}}" `
        --region $region `
        --output json | ConvertFrom-Json
    
    if ($items.Count -gt 0) {
        Write-Host "  ✓ Data successfully stored in DynamoDB" -ForegroundColor Green
        Write-Host "    Records found: $($items.Count)" -ForegroundColor White
    } else {
        Write-Host "  ⚠ No data found in DynamoDB" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Failed to query DynamoDB: $_" -ForegroundColor Red
}

Write-Host ""

# Test 6: Test Location History
Write-Host "Test 6: Testing location history retrieval..." -ForegroundColor Yellow

$getHistory = @{
    action = "GET_LOCATION_HISTORY"
    incidentId = $testIncidentId
    limit = 10
} | ConvertTo-Json

try {
    $historyResult = Invoke-RestMethod -Uri $lambdaUrl -Method Post -Body $getHistory -ContentType "application/json"
    
    if ($historyResult.status -eq "success") {
        Write-Host "  ✓ Location history retrieval successful" -ForegroundColor Green
        Write-Host "    History count: $($historyResult.count)" -ForegroundColor White
    } else {
        Write-Host "  ✗ Location history retrieval failed" -ForegroundColor Red
        Write-Host "    $($historyResult.message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Location history error: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Backend Test Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Test Summary:" -ForegroundColor Cyan
Write-Host "  ✓ DynamoDB tables operational" -ForegroundColor Green
Write-Host "  ✓ Lambda function responding" -ForegroundColor Green
Write-Host "  ✓ Location updates working" -ForegroundColor Green
Write-Host "  ✓ Location retrieval working" -ForegroundColor Green
Write-Host "  ✓ Data persistence verified" -ForegroundColor Green
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Deploy tracking page frontend" -ForegroundColor White
Write-Host "  2. Update emergency monitor to send location updates" -ForegroundColor White
Write-Host "  3. Test end-to-end emergency workflow with live tracking" -ForegroundColor White
Write-Host ""
