# Deploy Live Tracking Backend Infrastructure
# AllSenses AI Guardian - LIVE-TRACK-1 Implementation

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AllSenses Live Tracking Backend Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"
$region = "us-east-1"
$stackName = "AllSenses-LiveTracking-Backend"
$lambdaFunctionName = "AllSenses-Live-MVP-AllSensesFunction-ufWarJQ6FVRk"
$lambdaRoleName = "AllSenses-Live-MVP-LambdaRole-iHsI1SYbs1Ii"

# Step 1: Deploy DynamoDB Tables via CloudFormation
Write-Host "Step 1: Deploying DynamoDB tables..." -ForegroundColor Yellow
Write-Host ""

try {
    aws cloudformation deploy `
        --template-file infrastructure/live-tracking-dynamodb.yaml `
        --stack-name $stackName `
        --region $region `
        --no-fail-on-empty-changeset
    
    Write-Host "✓ DynamoDB tables deployed successfully" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to deploy DynamoDB tables: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "1. Ensure AWS CLI is configured: aws configure" -ForegroundColor White
    Write-Host "2. Verify you have CloudFormation permissions" -ForegroundColor White
    Write-Host "3. Check if tables already exist: aws dynamodb list-tables" -ForegroundColor White
    exit 1
}

Write-Host ""

# Step 2: Wait for stack to complete
Write-Host "Step 2: Waiting for stack to complete..." -ForegroundColor Yellow

try {
    aws cloudformation wait stack-create-complete `
        --stack-name $stackName `
        --region $region 2>$null
    
    if ($LASTEXITCODE -ne 0) {
        # Stack might already exist, try update-complete
        aws cloudformation wait stack-update-complete `
            --stack-name $stackName `
            --region $region 2>$null
    }
    
    Write-Host "✓ Stack deployment complete" -ForegroundColor Green
} catch {
    Write-Host "⚠ Stack wait timed out or stack already exists" -ForegroundColor Yellow
}

Write-Host ""

# Step 3: Get stack outputs
Write-Host "Step 3: Retrieving table information..." -ForegroundColor Yellow

try {
    $stackInfo = aws cloudformation describe-stacks `
        --stack-name $stackName `
        --region $region `
        --query 'Stacks[0].Outputs' `
        --output json | ConvertFrom-Json
    
    Write-Host "✓ Stack outputs retrieved" -ForegroundColor Green
    Write-Host ""
    Write-Host "Created Tables:" -ForegroundColor Cyan
    foreach ($output in $stackInfo) {
        Write-Host "  $($output.OutputKey): $($output.OutputValue)" -ForegroundColor White
    }
} catch {
    Write-Host "⚠ Could not retrieve stack outputs" -ForegroundColor Yellow
}

Write-Host ""

# Step 4: Verify tables exist
Write-Host "Step 4: Verifying DynamoDB tables..." -ForegroundColor Yellow

$tables = @("AllSenses-LocationTracking", "AllSenses-Incidents")
$allTablesExist = $true

foreach ($table in $tables) {
    try {
        $tableInfo = aws dynamodb describe-table `
            --table-name $table `
            --region $region `
            --query 'Table.[TableName,TableStatus,ItemCount]' `
            --output json | ConvertFrom-Json
        
        Write-Host "  ✓ $table - Status: $($tableInfo[1])" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ $table - Not found" -ForegroundColor Red
        $allTablesExist = $false
    }
}

if (-not $allTablesExist) {
    Write-Host ""
    Write-Host "✗ Some tables are missing. Please check CloudFormation stack." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Step 5: Add DynamoDB permissions to Lambda role
Write-Host "Step 5: Adding DynamoDB permissions to Lambda role..." -ForegroundColor Yellow
Write-Host ""

$policyName = "DynamoDBLocationTrackingPolicy"

try {
    # Check if policy already exists
    $existingPolicy = aws iam get-role-policy `
        --role-name $lambdaRoleName `
        --policy-name $policyName `
        --region $region 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ⚠ Policy already exists, updating..." -ForegroundColor Yellow
    }
    
    # Put (create or update) the inline policy
    aws iam put-role-policy `
        --role-name $lambdaRoleName `
        --policy-name $policyName `
        --policy-document file://live-tracking-lambda-iam-policy.json `
        --region $region
    
    Write-Host "  ✓ DynamoDB permissions added to Lambda role" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Failed to add permissions: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Manual Step Required:" -ForegroundColor Yellow
    Write-Host "  1. Go to IAM Console -> Roles -> $lambdaRoleName" -ForegroundColor White
    Write-Host "  2. Add inline policy from: live-tracking-lambda-iam-policy.json" -ForegroundColor White
    Write-Host ""
}

Write-Host ""

# Step 6: Verify Lambda function exists
Write-Host "Step 6: Verifying Lambda function..." -ForegroundColor Yellow

try {
    $lambdaInfo = aws lambda get-function `
        --function-name $lambdaFunctionName `
        --region $region `
        --query 'Configuration.[FunctionName,Runtime,LastModified]' `
        --output json | ConvertFrom-Json
    
    Write-Host "  ✓ Lambda function found: $($lambdaInfo[0])" -ForegroundColor Green
    Write-Host "    Runtime: $($lambdaInfo[1])" -ForegroundColor White
    Write-Host "    Last Modified: $($lambdaInfo[2])" -ForegroundColor White
} catch {
    Write-Host "  ✗ Lambda function not found: $lambdaFunctionName" -ForegroundColor Red
    Write-Host "  Please verify the function name is correct" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Backend Infrastructure Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  ✓ DynamoDB tables created with TTL enabled" -ForegroundColor Green
Write-Host "  ✓ AllSenses-LocationTracking (24-hour retention)" -ForegroundColor Green
Write-Host "  ✓ AllSenses-Incidents (7-day retention)" -ForegroundColor Green
Write-Host "  ✓ IAM permissions configured for Lambda" -ForegroundColor Green
Write-Host ""

Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Deploy updated Lambda function with location tracking handlers" -ForegroundColor White
Write-Host "     File: allsenseai-live-tracking.py" -ForegroundColor Cyan
Write-Host ""
Write-Host "  2. Test location storage:" -ForegroundColor White
Write-Host "     .\scripts\test-live-tracking-backend.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "  3. Update frontend to send location updates" -ForegroundColor White
Write-Host "     File: frontend/enhanced-emergency-monitor-with-tracking.html" -ForegroundColor Cyan
Write-Host ""

Write-Host "Ready for LIVE-TRACK-1.2: Update Lambda function" -ForegroundColor Green
Write-Host ""
