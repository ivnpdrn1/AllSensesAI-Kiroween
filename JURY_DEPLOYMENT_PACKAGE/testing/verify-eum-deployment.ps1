# Comprehensive EUM Deployment Verification Script
$LAMBDA_URL = "https://53x75wmoi5qtdv2gfc4sn3btzu0rivqx.lambda-url.us-east-1.on.aws/"
$TEST_PHONE = "+19543483664"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  EUM Deployment Verification" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check if Lambda responds
Write-Host "TEST 1: Lambda Connectivity" -ForegroundColor Yellow
Write-Host "Testing Lambda URL: $LAMBDA_URL" -ForegroundColor White
Write-Host ""

try {
    $healthCheck = @{
        action = "CHECK_EUM_CONFIG"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri $LAMBDA_URL -Method POST -Body $healthCheck -ContentType "application/json" -ErrorAction Stop
    
    Write-Host "Lambda Response Received:" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 5) -ForegroundColor White
    Write-Host ""
    
    # Check for EUM compliance indicators
    if ($response.configuration) {
        Write-Host "EUM Configuration Found:" -ForegroundColor Green
        Write-Host "  Originator: $($response.configuration.originatorNumber)" -ForegroundColor White
        Write-Host "  Campaign: $($response.configuration.campaign)" -ForegroundColor White
        Write-Host "  Message Type: $($response.configuration.messageType)" -ForegroundColor White
        Write-Host "  Region: $($response.configuration.region)" -ForegroundColor White
        
        if ($response.configuration.originatorNumber -eq "+12173933490") {
            Write-Host "  ✅ Correct originator number!" -ForegroundColor Green
        } else {
            Write-Host "  ❌ WRONG originator number!" -ForegroundColor Red
        }
        
        if ($response.configuration.campaign -eq "AllSensesAI-SafetyAlerts") {
            Write-Host "  ✅ Correct campaign!" -ForegroundColor Green
        } else {
            Write-Host "  ❌ WRONG campaign!" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ NO EUM configuration found - Lambda may not be EUM-compliant!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "❌ Lambda connectivity failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "This means the Lambda is not responding or crashed." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

# Test 2: Send actual SMS and check response
Write-Host ""
Write-Host "TEST 2: SMS Sending with Full Response Capture" -ForegroundColor Yellow
Write-Host "Sending SMS to: $TEST_PHONE" -ForegroundColor White
Write-Host ""

$smsPayload = @{
    action = "TEST_SMS"
    phoneNumber = $TEST_PHONE
    message = "EUM Verification Test - $(Get-Date -Format 'HH:mm:ss')"
} | ConvertTo-Json

try {
    $smsResponse = Invoke-RestMethod -Uri $LAMBDA_URL -Method POST -Body $smsPayload -ContentType "application/json" -ErrorAction Stop
    
    Write-Host "SMS Response Received:" -ForegroundColor Green
    Write-Host ($smsResponse | ConvertTo-Json -Depth 5) -ForegroundColor White
    Write-Host ""
    
    # Critical checks
    Write-Host "CRITICAL VERIFICATION CHECKS:" -ForegroundColor Yellow
    Write-Host ""
    
    # Check 1: EUM Compliance Flag
    if ($smsResponse.eumCompliant -eq $true) {
        Write-Host "✅ CHECK 1: eumCompliant = true" -ForegroundColor Green
    } else {
        Write-Host "❌ CHECK 1: eumCompliant = false or missing" -ForegroundColor Red
        Write-Host "   This means Lambda is NOT using EUM API!" -ForegroundColor Red
    }
    
    # Check 2: Originator Number
    if ($smsResponse.originatorNumber -eq "+12173933490") {
        Write-Host "✅ CHECK 2: originatorNumber = +12173933490" -ForegroundColor Green
    } else {
        Write-Host "❌ CHECK 2: originatorNumber = $($smsResponse.originatorNumber)" -ForegroundColor Red
        Write-Host "   Expected: +12173933490" -ForegroundColor Red
    }
    
    # Check 3: Campaign
    if ($smsResponse.campaign -eq "AllSensesAI-SafetyAlerts") {
        Write-Host "✅ CHECK 3: campaign = AllSensesAI-SafetyAlerts" -ForegroundColor Green
    } else {
        Write-Host "❌ CHECK 3: campaign = $($smsResponse.campaign)" -ForegroundColor Red
        Write-Host "   Expected: AllSensesAI-SafetyAlerts" -ForegroundColor Red
    }
    
    # Check 4: MessageId format
    if ($smsResponse.smsMessageId) {
        Write-Host "✅ CHECK 4: MessageId present = $($smsResponse.smsMessageId)" -ForegroundColor Green
        
        # Check if it's a fake demo MessageId
        if ($smsResponse.smsMessageId -like "demo-*" -or $smsResponse.smsMessageId -like "jury-demo-*") {
            Write-Host "   ⚠️  WARNING: This is a DEMO MessageId!" -ForegroundColor Yellow
            Write-Host "   SMS was NOT actually sent!" -ForegroundColor Red
        } else {
            Write-Host "   ✅ Real MessageId (not demo)" -ForegroundColor Green
        }
    } else {
        Write-Host "❌ CHECK 4: No MessageId in response!" -ForegroundColor Red
    }
    
    # Check 5: SMS Status
    if ($smsResponse.smsStatus -eq "sent") {
        Write-Host "✅ CHECK 5: smsStatus = sent" -ForegroundColor Green
    } else {
        Write-Host "❌ CHECK 5: smsStatus = $($smsResponse.smsStatus)" -ForegroundColor Red
    }
    
    # Check 6: Error field
    if ($smsResponse.smsError) {
        Write-Host "❌ CHECK 6: SMS Error present!" -ForegroundColor Red
        Write-Host "   Error: $($smsResponse.smsError)" -ForegroundColor Red
    } else {
        Write-Host "✅ CHECK 6: No SMS errors" -ForegroundColor Green
    }
    
    Write-Host ""
    
    # Final verdict
    $allChecksPass = (
        $smsResponse.eumCompliant -eq $true -and
        $smsResponse.originatorNumber -eq "+12173933490" -and
        $smsResponse.campaign -eq "AllSensesAI-SafetyAlerts" -and
        $smsResponse.smsMessageId -and
        $smsResponse.smsMessageId -notlike "demo-*" -and
        $smsResponse.smsMessageId -notlike "jury-demo-*" -and
        $smsResponse.smsStatus -eq "sent" -and
        -not $smsResponse.smsError
    )
    
    if ($allChecksPass) {
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✅ ALL CHECKS PASSED!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Lambda is EUM-compliant and working correctly!" -ForegroundColor Green
        Write-Host "Messages SHOULD appear in EUM Dashboard." -ForegroundColor Green
        Write-Host ""
        Write-Host "Next step: Check AWS End User Messaging Dashboard" -ForegroundColor Cyan
        Write-Host "  1. Go to AWS Console → Amazon Pinpoint" -ForegroundColor White
        Write-Host "  2. Click 'SMS and voice' → 'Phone numbers'" -ForegroundColor White
        Write-Host "  3. Find +12173933490" -ForegroundColor White
        Write-Host "  4. Verify message count increased" -ForegroundColor White
    } else {
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "  ❌ CHECKS FAILED!" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "Lambda is NOT EUM-compliant!" -ForegroundColor Red
        Write-Host ""
        Write-Host "DIAGNOSIS:" -ForegroundColor Yellow
        
        if ($smsResponse.eumCompliant -ne $true) {
            Write-Host "  ❌ Lambda is still using legacy SNS API" -ForegroundColor Red
            Write-Host "  → The EUM-compliant code was NOT deployed" -ForegroundColor Red
        }
        
        if ($smsResponse.originatorNumber -ne "+12173933490") {
            Write-Host "  ❌ Wrong originator number" -ForegroundColor Red
            Write-Host "  → Lambda is not using your registered 10DLC number" -ForegroundColor Red
        }
        
        if ($smsResponse.campaign -ne "AllSensesAI-SafetyAlerts") {
            Write-Host "  ❌ Wrong or missing campaign" -ForegroundColor Red
            Write-Host "  → Lambda is not using your registered campaign" -ForegroundColor Red
        }
        
        Write-Host ""
        Write-Host "ACTION REQUIRED:" -ForegroundColor Yellow
        Write-Host "  1. Deploy allsenseai-eum-compliant.py to Lambda" -ForegroundColor White
        Write-Host "  2. Add EUM permissions to Lambda IAM role" -ForegroundColor White
        Write-Host "  3. Wait 30 seconds for deployment" -ForegroundColor White
        Write-Host "  4. Run this script again" -ForegroundColor White
    }
    
} catch {
    Write-Host "❌ SMS test failed!" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Verification Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
