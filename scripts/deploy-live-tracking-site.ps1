# Deploy Live Tracking Site to S3 + CloudFront
# AllSenses AI Guardian - Real-Time Location Tracking

param(
    [string]$StackName = "AllSensesAI-LiveTracking",
    [string]$Region = "us-east-1",
    [switch]$SkipInfrastructure,
    [switch]$InvalidateCache
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AllSensesAI Live Tracking - Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Deploy Infrastructure (if not skipped)
if (-not $SkipInfrastructure) {
    Write-Host "Step 1: Deploying CloudFormation stack..." -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "NOTE: Custom domain configuration requires:" -ForegroundColor Yellow
    Write-Host "  1. Route 53 Hosted Zone for allsensesai.com" -ForegroundColor White
    Write-Host "  2. ACM Certificate for *.allsensesai.com in us-east-1" -ForegroundColor White
    Write-Host ""
    Write-Host "Deploying without custom domain for now..." -ForegroundColor Yellow
    Write-Host "You can update the stack later with domain parameters." -ForegroundColor White
    Write-Host ""
    
    try {
        aws cloudformation deploy `
            --template-file infrastructure/live-tracking-hosting.yaml `
            --stack-name $StackName `
            --region $Region `
            --no-fail-on-empty-changeset `
            --tags Project=AllSensesAI Component=LiveTracking
        
        Write-Host "✓ CloudFormation stack deployed successfully" -ForegroundColor Green
    } catch {
        Write-Host "✗ CloudFormation deployment failed: $_" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
}

# Step 2: Get Stack Outputs
Write-Host "Step 2: Retrieving stack outputs..." -ForegroundColor Yellow

try {
    $stackInfo = aws cloudformation describe-stacks `
        --stack-name $StackName `
        --region $Region `
        --query 'Stacks[0].Outputs' | ConvertFrom-Json
    
    $bucketName = ($stackInfo | Where-Object { $_.OutputKey -eq 'BucketName' }).OutputValue
    $distributionId = ($stackInfo | Where-Object { $_.OutputKey -eq 'DistributionId' }).OutputValue
    $trackingURL = ($stackInfo | Where-Object { $_.OutputKey -eq 'TrackingURL' }).OutputValue
    
    Write-Host "✓ Bucket: $bucketName" -ForegroundColor Green
    Write-Host "✓ Distribution: $distributionId" -ForegroundColor Green
    Write-Host "✓ URL: $trackingURL" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "✗ Failed to retrieve stack outputs: $_" -ForegroundColor Red
    exit 1
}

# Step 3: Upload Tracking Pages
Write-Host "Step 3: Uploading tracking pages to S3..." -ForegroundColor Yellow

$files = @(
    @{
        Source = "frontend/live-tracking.html"
        Key = "live-tracking.html"
        ContentType = "text/html"
    },
    @{
        Source = "frontend/live-tracking-no-api-key.html"
        Key = "live-tracking-no-api-key.html"
        ContentType = "text/html"
    },
    @{
        Source = "frontend/live-tracking.html"
        Key = "index.html"
        ContentType = "text/html"
    }
)

foreach ($file in $files) {
    try {
        Write-Host "  Uploading $($file.Key)..." -ForegroundColor Gray
        
        aws s3 cp $file.Source "s3://$bucketName/$($file.Key)" `
            --content-type $file.ContentType `
            --cache-control "public, max-age=300" `
            --metadata-directive REPLACE `
            --region $Region
        
        Write-Host "  ✓ $($file.Key) uploaded" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed to upload $($file.Key): $_" -ForegroundColor Red
    }
}

Write-Host ""

# Step 4: Invalidate CloudFront Cache
if ($InvalidateCache) {
    Write-Host "Step 4: Invalidating CloudFront cache..." -ForegroundColor Yellow
    
    try {
        $invalidationId = aws cloudfront create-invalidation `
            --distribution-id $distributionId `
            --paths "/*" `
            --query 'Invalidation.Id' `
            --output text
        
        Write-Host "✓ Cache invalidation created: $invalidationId" -ForegroundColor Green
        Write-Host "  Note: Invalidation may take 5-10 minutes to complete" -ForegroundColor Gray
    } catch {
        Write-Host "✗ Failed to invalidate cache: $_" -ForegroundColor Red
    }
    
    Write-Host ""
}

# Step 5: Test Deployment
Write-Host "Step 5: Testing deployment..." -ForegroundColor Yellow

try {
    $testUrl = "$trackingURL/live-tracking.html?incident=TEST-123"
    Write-Host "  Test URL: $testUrl" -ForegroundColor Gray
    
    $response = Invoke-WebRequest -Uri $testUrl -Method HEAD -UseBasicParsing -ErrorAction Stop
    
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Tracking page is accessible" -ForegroundColor Green
    } else {
        Write-Host "⚠ Unexpected status code: $($response.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Could not test deployment (may need time to propagate): $_" -ForegroundColor Yellow
}

Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Tracking Page URLs:" -ForegroundColor Yellow
Write-Host "  Main (Google Maps): $trackingURL/live-tracking.html?incident=EMG-XXXXX" -ForegroundColor White
Write-Host "  Fallback (Leaflet): $trackingURL/live-tracking-no-api-key.html?incident=EMG-XXXXX" -ForegroundColor White
Write-Host ""
Write-Host "S3 Bucket: $bucketName" -ForegroundColor Cyan
Write-Host "CloudFront Distribution: $distributionId" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Update Lambda TRACKING_BASE_URL to: $trackingURL" -ForegroundColor White
Write-Host "2. Add Google Maps API key to live-tracking.html" -ForegroundColor White
Write-Host "3. Test with real incident ID" -ForegroundColor White
Write-Host "4. (Optional) Configure custom domain track.allsensesai.com" -ForegroundColor White
Write-Host ""
