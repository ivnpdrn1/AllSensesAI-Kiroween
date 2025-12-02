# Invalidate CloudFront Cache for Live Tracking Site
# Use after updating tracking pages

param(
    [string]$StackName = "AllSensesAI-LiveTracking",
    [string]$Region = "us-east-1",
    [string[]]$Paths = @("/*")
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CloudFront Cache Invalidation" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get Distribution ID from stack
Write-Host "Retrieving CloudFront Distribution ID..." -ForegroundColor Yellow

try {
    $stackInfo = aws cloudformation describe-stacks `
        --stack-name $StackName `
        --region $Region `
        --query 'Stacks[0].Outputs' | ConvertFrom-Json
    
    $distributionId = ($stackInfo | Where-Object { $_.OutputKey -eq 'DistributionId' }).OutputValue
    
    if (-not $distributionId) {
        throw "Distribution ID not found in stack outputs"
    }
    
    Write-Host "✓ Distribution ID: $distributionId" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "✗ Failed to retrieve distribution ID: $_" -ForegroundColor Red
    exit 1
}

# Create invalidation
Write-Host "Creating cache invalidation..." -ForegroundColor Yellow
Write-Host "  Paths: $($Paths -join ', ')" -ForegroundColor Gray

try {
    $pathsJson = $Paths | ConvertTo-Json -Compress
    
    $invalidationId = aws cloudfront create-invalidation `
        --distribution-id $distributionId `
        --paths $Paths `
        --query 'Invalidation.Id' `
        --output text
    
    Write-Host "✓ Invalidation created: $invalidationId" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "✗ Failed to create invalidation: $_" -ForegroundColor Red
    exit 1
}

# Check invalidation status
Write-Host "Checking invalidation status..." -ForegroundColor Yellow

try {
    $status = aws cloudfront get-invalidation `
        --distribution-id $distributionId `
        --id $invalidationId `
        --query 'Invalidation.Status' `
        --output text
    
    Write-Host "  Status: $status" -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "⚠ Could not check status: $_" -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Cache Invalidation Initiated" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: Invalidation typically takes 5-10 minutes to complete." -ForegroundColor Yellow
Write-Host "You can check status in the CloudFront console." -ForegroundColor White
Write-Host ""
Write-Host "Distribution ID: $distributionId" -ForegroundColor Cyan
Write-Host "Invalidation ID: $invalidationId" -ForegroundColor Cyan
Write-Host ""
