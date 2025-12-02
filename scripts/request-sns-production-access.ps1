# Request AWS SNS Production Access
# This script helps you move out of SNS sandbox mode

Write-Host "🚀 AWS SNS Production Access Request" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green

Write-Host "`n📋 Steps to move out of SNS Sandbox Mode:" -ForegroundColor Yellow

Write-Host "`n1. Open AWS Support Center:"
Write-Host "   https://console.aws.amazon.com/support/home" -ForegroundColor Cyan

Write-Host "`n2. Create a new case with these details:"
Write-Host "   - Service: Amazon Simple Notification Service (SNS)"
Write-Host "   - Category: Service Limit Increase"
Write-Host "   - Severity: General guidance"

Write-Host "`n3. Request Details:"
Write-Host "   - Subject: 'Request to move SNS out of sandbox mode'"
Write-Host "   - Description: 'Please move my AWS account out of SNS sandbox mode for emergency notification system'"
Write-Host "   - Use case: 'AllSensesAI emergency response system for safety notifications'"

Write-Host "`n4. Additional Information to Include:"
Write-Host "   - Expected monthly SMS volume: 1000-5000 messages"
Write-Host "   - Message type: Emergency notifications and safety alerts"
Write-Host "   - Target regions: US, Canada (adjust as needed)"
Write-Host "   - Compliance: Emergency services integration"

Write-Host "`n5. Check current SNS limits:"
aws sns get-sms-attributes --region us-east-1

Write-Host "`n6. Verify sandbox status:"
aws sns get-sms-sandbox-account-status --region us-east-1

Write-Host "`n⏱️  Typical approval time: 24-48 hours" -ForegroundColor Green
Write-Host "📧 You'll receive email confirmation when approved" -ForegroundColor Green

Write-Host "`n🔧 Alternative: Use verified phone numbers for testing" -ForegroundColor Yellow
Write-Host "Run: aws sns create-sms-sandbox-phone-number --phone-number +1234567890" -ForegroundColor Cyan