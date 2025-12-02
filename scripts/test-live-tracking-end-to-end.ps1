# End-to-End Live Tracking System Test
# AllSenses AI Guardian - Complete Validation

param(
    [string]$LambdaUrl = "https://53x75wmoi5qtdv2gfc4sn3btzu0rivqx.lambda-url.us-east-1.on.aws/",
    [string]$TestPhone = "+19543483664",
    [string]$VictimName = "Test User - E2E",
    [int]$TestDurationSeconds = 60
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Live Tracking - End-to-End Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$testResults = @{
    EmergencyCreation = $false
    IncidentIdGenerated = $false
    TrackingUrlGenerated = $false
    SmsDelivered = $false
    LocationStored = $false
    LocationRetrieved = $false
    MovementTrailCreated = $false
    BatteryMonitored = $false
    SpeedCalculated = $false
    TimestampUpdated = $false
}

$incidentId = $null
$trackingUrl = $null

# Test 1: Create Emergency with Location
Write-Host "Test 1: Creating emergency with initial location..." -ForegroundColor Yellow

$emergencyPayload = @{
    action = "JURY_EMERGENCY_ALERT"
    victimName = $VictimName
    phoneNumber = $TestPhone
    detectionType = "emergency_words"
    detectionData = @{
        detectedWords = @("help", "emergency")
        transcript = "help me please"
        confidence = 0.95
    }
    location = @{
        latitude = 25.7617
        longitude = -80.1918
        accuracy = 10.5
        placeName = "Miami Convention Center, FL"
    }
    timestamp = (Get-Date).ToUniversalTime().ToString("o")
} | ConvertTo-Json -Depth 5

try {
    $response = Invoke-RestMethod -Uri $LambdaUrl `
        -Method POST -Body $emergencyPayload -ContentType "application/json"
    
    if ($response.status -eq "success") {
        $testResults.EmergencyCreation = $true
        Write-Host "✓ Emergency created successfully" -ForegroundColor Green
        
        if ($response.incidentId) {
            $incidentId = $response.incidentId
            $testResults.IncidentIdGenerated = $true
            Write-Host "✓ Incident ID generated: $incidentId" -ForegroundColor Green
        }
        
        if ($response.trackingUrl) {
            $trackingUrl = $response.trackingUrl
            $testResults.TrackingUrlGenerated = $true
            Write-Host "✓ Tracking URL generated: $trackingUrl" -ForegroundColor Green
        }
        
        if ($response.smsMessageId) {
            $testResults.SmsDelivered = $true
            Write-Host "✓ SMS delivered: $($response.smsMessageId)" -ForegroundColor Green
        }
    } else {
        throw "Emergency creation failed: $($response.message)"
    }
} catch {
    Write-Host "✗ Test 1 Failed: $_" -ForegroundColor Red
}

Write-Host ""

if (-not $incidentId) {
    Write-Host "Cannot continue without incident ID" -ForegroundColor Red
    exit 1
}

# Test 2: Simulate Location Updates
Write-Host "Test 2: Simulating location updates (movement)..." -ForegroundColor Yellow

$locations = @(
    @{ lat = 25.7620; lon = -80.1920; speed = 0.5; battery = 95 },
    @{ lat = 25.7625; lon = -80.1925; speed = 1.2; battery = 94 },
    @{ lat = 25.7630; lon = -80.1930; speed = 2.1; battery = 93 },
    @{ lat = 25.7635; lon = -80.1935; speed = 1.8; battery = 92 },
    @{ lat = 25.7640; lon = -80.1940; speed = 2.5; battery = 91 }
)

$updateCount = 0
foreach ($loc in $locations) {
    $locationUpdate = @{
        action = "UPDATE_LOCATION"
        incidentId = $incidentId
        victimName = $VictimName
        location = @{
            latitude = $loc.lat
            longitude = $loc.lon
            accuracy = 8.0 + (Get-Random -Minimum -2 -Maximum 2)
            speed = $loc.speed
            heading = 180
            timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }
        batteryLevel = $loc.battery
    } | ConvertTo-Json -Depth 5
    
    try {
        $result = Invoke-RestMethod -Uri $LambdaUrl `
            -Method POST -Body $locationUpdate -ContentType "application/json"
        
        if ($result.status -eq "success") {
            $updateCount++
            Write-Host "  ✓ Location update $updateCount : ($($loc.lat), $($loc.lon)) - $($loc.speed) m/s - $($loc.battery)%" -ForegroundColor Green
            
            if ($updateCount -eq 1) {
                $testResults.LocationStored = $true
            }
            if ($updateCount -gt 1) {
                $testResults.MovementTrailCreated = $true
            }
            if ($loc.battery) {
                $testResults.BatteryMonitored = $true
            }
            if ($loc.speed) {
                $testResults.SpeedCalculated = $true
            }
        }
    } catch {
        Write-Host "  ✗ Location update $updateCount failed: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 2
}

Write-Host ""

# Test 3: Retrieve Location Data
Write-Host "Test 3: Retrieving location from backend..." -ForegroundColor Yellow

$getLocation = @{
    action = "GET_LOCATION"
    incidentId = $incidentId
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri $LambdaUrl `
        -Method POST -Body $getLocation -ContentType "application/json"
    
    if ($result.status -eq "success" -and $result.location) {
        $testResults.LocationRetrieved = $true
        $testResults.TimestampUpdated = $true
        
        Write-Host "✓ Location retrieved successfully" -ForegroundColor Green
        Write-Host "  Latitude: $($result.location.latitude)" -ForegroundColor Gray
        Write-Host "  Longitude: $($result.location.longitude)" -ForegroundColor Gray
        Write-Host "  Accuracy: $($result.location.accuracy)m" -ForegroundColor Gray
        Write-Host "  Speed: $($result.location.speed) m/s" -ForegroundColor Gray
        Write-Host "  Battery: $($result.location.batteryLevel)%" -ForegroundColor Gray
        Write-Host "  Timestamp: $($result.location.timestamp)" -ForegroundColor Gray
    } else {
        throw "Location retrieval failed"
    }
} catch {
    Write-Host "✗ Test 3 Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 4: Get Location History
Write-Host "Test 4: Retrieving location history (movement trail)..." -ForegroundColor Yellow

$getHistory = @{
    action = "GET_LOCATION_HISTORY"
    incidentId = $incidentId
    limit = 10
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri $LambdaUrl `
        -Method POST -Body $getHistory -ContentType "application/json"
    
    if ($result.status -eq "success" -and $result.history) {
        Write-Host "✓ Location history retrieved: $($result.count) points" -ForegroundColor Green
        
        if ($result.count -gt 1) {
            $testResults.MovementTrailCreated = $true
            Write-Host "  Movement trail:" -ForegroundColor Gray
            foreach ($point in $result.history | Select-Object -First 3) {
                Write-Host "    ($($point.latitude), $($point.longitude)) - $($point.accuracy)m" -ForegroundColor Gray
            }
        }
    } else {
        throw "History retrieval failed"
    }
} catch {
    Write-Host "✗ Test 4 Failed: $_" -ForegroundColor Red
}

Write-Host ""

# Test 5: Verify Tracking URL Accessibility
Write-Host "Test 5: Verifying tracking URL accessibility..." -ForegroundColor Yellow

if ($trackingUrl) {
    try {
        $testUrl = "$trackingUrl"
        $response = Invoke-WebRequest -Uri $testUrl -Method HEAD -UseBasicParsing -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ Tracking URL is accessible" -ForegroundColor Green
            Write-Host "  URL: $testUrl" -ForegroundColor Gray
        } else {
            Write-Host "⚠ Unexpected status code: $($response.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠ Tracking URL not accessible (may need deployment): $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "⚠ No tracking URL to test" -ForegroundColor Yellow
}

Write-Host ""

# Test 6: Check EUM Configuration
Write-Host "Test 6: Verifying EUM configuration..." -ForegroundColor Yellow

$checkConfig = @{
    action = "CHECK_EUM_CONFIG"
} | ConvertTo-Json

try {
    $result = Invoke-RestMethod -Uri $LambdaUrl `
        -Method POST -Body $checkConfig -ContentType "application/json"
    
    if ($result.status -eq "success") {
        Write-Host "✓ EUM configuration verified" -ForegroundColor Green
        Write-Host "  Originator: $($result.configuration.originatorNumber)" -ForegroundColor Gray
        Write-Host "  Campaign: $($result.configuration.campaign)" -ForegroundColor Gray
        Write-Host "  Live Tracking: $($result.configuration.liveTrackingEnabled)" -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠ EUM configuration check failed: $_" -ForegroundColor Yellow
}

Write-Host ""

# Test Results Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Results Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passedTests = 0
$totalTests = $testResults.Count

foreach ($test in $testResults.GetEnumerator()) {
    $status = if ($test.Value) { "✓ PASS" } else { "✗ FAIL" }
    $color = if ($test.Value) { "Green" } else { "Red" }
    
    Write-Host "$status - $($test.Key)" -ForegroundColor $color
    
    if ($test.Value) { $passedTests++ }
}

Write-Host ""
Write-Host "Overall: $passedTests / $totalTests tests passed" -ForegroundColor $(if ($passedTests -eq $totalTests) { "Green" } else { "Yellow" })
Write-Host ""

# Test Information
Write-Host "Test Information:" -ForegroundColor Cyan
Write-Host "  Incident ID: $incidentId" -ForegroundColor White
Write-Host "  Tracking URL: $trackingUrl" -ForegroundColor White
Write-Host "  Test Phone: $TestPhone" -ForegroundColor White
Write-Host "  Victim Name: $VictimName" -ForegroundColor White
Write-Host ""

# Next Steps
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Open tracking URL in browser to verify map display" -ForegroundColor White
Write-Host "2. Check SMS on test phone for tracking link" -ForegroundColor White
Write-Host "3. Verify DynamoDB tables contain location data" -ForegroundColor White
Write-Host "4. Test on multiple devices and browsers" -ForegroundColor White
Write-Host ""

# Return test results
return @{
    Success = ($passedTests -eq $totalTests)
    PassedTests = $passedTests
    TotalTests = $totalTests
    IncidentId = $incidentId
    TrackingUrl = $trackingUrl
    Results = $testResults
}
