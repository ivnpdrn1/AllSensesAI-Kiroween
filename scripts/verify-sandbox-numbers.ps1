# Verify Phone Numbers in SNS Sandbox Mode
# Use this while waiting for production access approval

param(
    [string]$PhoneNumber = "",
    [string]$VerificationCode = ""
)

Write-Host "📱 AWS SNS Sandbox Phone Number Verification" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green

if ($PhoneNumber -eq "") {
    Write-Host "`n📋 Available Commands:" -ForegroundColor Yellow
    Write-Host "1. List verified numbers:"
    Write-Host "   .\scripts\verify-sandbox-numbers.ps1" -ForegroundColor Cyan
    
    Write-Host "`n2. Start verification for a new number:"
    Write-Host "   .\scripts\verify-sandbox-numbers.ps1 -PhoneNumber '+1234567890'" -ForegroundColor Cyan
    
    Write-Host "`n3. Complete verification with code:"
    Write-Host "   .\scripts\verify-sandbox-numbers.ps1 -PhoneNumber '+1234567890' -VerificationCode '123456'" -ForegroundColor Cyan
    
    Write-Host "`n📱 Currently verified numbers:" -ForegroundColor Yellow
    aws sns list-sms-sandbox-phone-numbers --region us-east-1
    
    Write-Host "`n🔍 Sandbox status:" -ForegroundColor Yellow
    aws sns get-sms-sandbox-account-status --region us-east-1
    
    exit 0
}

if ($VerificationCode -eq "") {
    # Start verification process
    Write-Host "`n🚀 Starting verification for: $PhoneNumber" -ForegroundColor Yellow
    
    try {
        aws sns create-sms-sandbox-phone-number --phone-number $PhoneNumber --region us-east-1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Verification SMS sent to $PhoneNumber" -ForegroundColor Green
            Write-Host "`n📨 Check your phone for the verification code" -ForegroundColor Cyan
            Write-Host "`n🔄 Complete verification with:" -ForegroundColor Yellow
            Write-Host ".\scripts\verify-sandbox-numbers.ps1 -PhoneNumber '$PhoneNumber' -VerificationCode 'YOUR_CODE'" -ForegroundColor Gray
        } else {
            Write-Host "❌ Failed to send verification SMS" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    # Complete verification process
    Write-Host "`n🔐 Completing verification for: $PhoneNumber" -ForegroundColor Yellow
    Write-Host "Using code: $VerificationCode" -ForegroundColor Gray
    
    try {
        aws sns verify-sms-sandbox-phone-number --phone-number $PhoneNumber --one-time-password $VerificationCode --region us-east-1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Phone number $PhoneNumber verified successfully!" -ForegroundColor Green
            
            Write-Host "`n📱 Updated verified numbers:" -ForegroundColor Yellow
            aws sns list-sms-sandbox-phone-numbers --region us-east-1
            
            Write-Host "`n🧪 You can now test SMS to this number!" -ForegroundColor Green
        } else {
            Write-Host "❌ Verification failed - check the code and try again" -ForegroundColor Red
        }
    } catch {
        Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n💡 Pro Tips:" -ForegroundColor Cyan
Write-Host "• Verification codes expire in 5 minutes" -ForegroundColor White
Write-Host "• You can verify up to 10 numbers in sandbox mode" -ForegroundColor White
Write-Host "• Use international format: +1234567890" -ForegroundColor White
Write-Host "• Request production access for unlimited numbers" -ForegroundColor White