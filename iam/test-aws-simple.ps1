# Simple AWS CLI Test
Write-Host "Testing AWS CLI..." -ForegroundColor Cyan
Write-Host ""

# Test AWS CLI
Write-Host "Running: aws --version" -ForegroundColor Yellow
aws --version
Write-Host ""

Write-Host "Running: aws sts get-caller-identity" -ForegroundColor Yellow
aws sts get-caller-identity
Write-Host ""

if ($LASTEXITCODE -eq 0) {
    Write-Host "SUCCESS: AWS CLI is working!" -ForegroundColor Green
} else {
    Write-Host "FAILED: AWS CLI is not configured" -ForegroundColor Red
    Write-Host "Run: aws configure" -ForegroundColor Yellow
}
