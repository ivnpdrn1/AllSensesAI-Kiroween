# Test what error AWS returns
Write-Host "Testing AWS IAM access..."
Write-Host ""

Write-Host "Attempting to check if role exists..."
aws iam get-role --role-name KiroDeployRole 2>&1 | Out-String | Write-Host
Write-Host ""
Write-Host "Exit code: $LASTEXITCODE"
