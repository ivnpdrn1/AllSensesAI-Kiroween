# Deploy DynamoDB Tables for Live Location Tracking
# AllSenses AI Guardian - Real-Time Google Maps Integration

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AllSenses Live Tracking - DynamoDB Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$region = "us-east-1"

# Step 1: Create Location Tracking Table
Write-Host "Step 1: Creating AllSenses-LocationTracking table..." -ForegroundColor Yellow

try {
    aws dynamodb create-table `
        --table-name AllSenses-LocationTracking `
        --attribute-definitions `
            AttributeName=incidentId,AttributeType=S `
            AttributeName=timestamp,AttributeType=N `
        --key-schema `
            AttributeName=incidentId,KeyType=HASH `
            AttributeName=timestamp,KeyType=RANGE `
        --billing-mode PAY_PER_REQUEST `
        --region $region `
        --tags Key=Project,Value=AllSensesAI Key=Feature,Value=LiveTracking
    
    Write-Host "✓ AllSenses-LocationTracking table created successfully" -ForegroundColor Green
} catch {
    if ($_.Exception.Message -like "*ResourceInUseException*") {
        Write-Host "⚠ AllSenses-LocationTracking table already exists" -ForegroundColor Yellow
    } else {
        Write-Host "✗ Failed to create LocationTracking table: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Step 2: Enable TTL on Location Tracking Table
Write-Host "Step 2: Enabling TTL on AllSenses-LocationTracking..." -ForegroundColor Yellow

try {
    aws dynamodb update-time-to-live `
        --table-name AllSenses-LocationTracking `
        --time-to-live-specification "Enabled=true,AttributeName=ttl" `
        --region $region
    
    Write-Host "✓ TTL enabled on AllSenses-LocationTracking (24-hour auto-delete)" -ForegroundColor Green
} catch {
    Write-Host "⚠ TTL may already be enabled: $_" -ForegroundColor Yellow
}

Write-Host ""

# Step 3: Create Incidents Table
Write-Host "Step 3: Creating AllSenses-Incidents table..." -ForegroundColor Yellow

try {
    aws dynamodb create-table `
        --table-name AllSenses-Incidents `
        --attribute-definitions `
            AttributeName=incidentId,AttributeType=S `
        --key-schema `
            AttributeName=incidentId,KeyType=HASH `
        --billing-mode PAY_PER_REQUEST `
        --region $region `
        --tags Key=Project,Value=AllSensesAI Key=Feature,Value=LiveTracking
    
    Write-Host "✓ AllSenses-Incidents table created successfully" -ForegroundColor Green
} catch {
    if ($_.Exception.Message -like "*ResourceInUseException*") {
        Write-Host "⚠ AllSenses-Incidents table already exists" -ForegroundColor Yellow
    } else {
        Write-Host "✗ Failed to create Incidents table: $_" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""

# Step 4: Enable TTL on Incidents Table
Write-Host "Step 4: Enabling TTL on AllSenses-Incidents..." -ForegroundColor Yellow

try {
    aws dynamodb update-time-to-live `
        --table-name AllSenses-Incidents `
        --time-to-live-specification "Enabled=true,AttributeName=ttl" `
        --region $region
    
    Write-Host "✓ TTL enabled on AllSenses-Incidents (7-day auto-delete)" -ForegroundColor Green
} catch {
    Write-Host "⚠ TTL may already be enabled: $_" -ForegroundColor Yellow
}

Write-Host ""

# Step 5: Wait for tables to become active
Write-Host "Step 5: Waiting for tables to become active..." -ForegroundColor Yellow

aws dynamodb wait table-exists --table-name AllSenses-LocationTracking --region $region
aws dynamodb wait table-exists --table-name AllSenses-Incidents --region $region

Write-Host "✓ All tables are active and ready" -ForegroundColor Green
Write-Host ""

# Step 6: Verify tables
Write-Host "Step 6: Verifying table configuration..." -ForegroundColor Yellow

$locationTable = aws dynamodb describe-table --table-name AllSenses-LocationTracking --region $region | ConvertFrom-Json
$incidentsTable = aws dynamodb describe-table --table-name AllSenses-Incidents --region $region | ConvertFrom-Json

Write-Host ""
Write-Host "Table Summary:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AllSenses-LocationTracking:" -ForegroundColor White
Write-Host "  Status: $($locationTable.Table.TableStatus)" -ForegroundColor Green
Write-Host "  Partition Key: incidentId (String)" -ForegroundColor White
Write-Host "  Sort Key: timestamp (Number)" -ForegroundColor White
Write-Host "  Billing: PAY_PER_REQUEST" -ForegroundColor White
Write-Host "  TTL: Enabled - 24 hours" -ForegroundColor White
Write-Host ""
Write-Host "AllSenses-Incidents:" -ForegroundColor White
Write-Host "  Status: $($incidentsTable.Table.TableStatus)" -ForegroundColor Green
Write-Host "  Partition Key: incidentId (String)" -ForegroundColor White
Write-Host "  Billing: PAY_PER_REQUEST" -ForegroundColor White
Write-Host "  TTL: Enabled - 7 days" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 7: Update IAM Policy
Write-Host "Step 7: Checking IAM permissions..." -ForegroundColor Yellow

$lambdaRoleName = "AllSenses-Live-MVP-LambdaRole-iHsI1SYbs1Ii"

Write-Host ""
Write-Host "MANUAL STEP REQUIRED:" -ForegroundColor Yellow
Write-Host "Add the following policy to Lambda role: $lambdaRoleName" -ForegroundColor White
Write-Host ""

$policyJson = @'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "DynamoDBLocationTracking",
            "Effect": "Allow",
            "Action": [
                "dynamodb:PutItem",
                "dynamodb:GetItem",
                "dynamodb:Query",
                "dynamodb:Scan",
                "dynamodb:UpdateItem"
            ],
            "Resource": [
                "arn:aws:dynamodb:us-east-1:*:table/AllSenses-LocationTracking",
                "arn:aws:dynamodb:us-east-1:*:table/AllSenses-Incidents"
            ]
        }
    ]
}
'@

Write-Host $policyJson -ForegroundColor Cyan

Write-Host ""
Write-Host "To add this policy:" -ForegroundColor Yellow
Write-Host "1. Go to IAM Console -> Roles -> $lambdaRoleName" -ForegroundColor White
Write-Host "2. Click Add permissions -> Create inline policy" -ForegroundColor White
Write-Host "3. Paste the JSON above" -ForegroundColor White
Write-Host "4. Name it: DynamoDBLocationTrackingPolicy" -ForegroundColor White
Write-Host "5. Click Create policy" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ DynamoDB Tables Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Add IAM policy to Lambda role (see above)" -ForegroundColor White
Write-Host "2. Deploy updated Lambda function with location tracking" -ForegroundColor White
Write-Host "3. Test location storage and retrieval" -ForegroundColor White
Write-Host ""
