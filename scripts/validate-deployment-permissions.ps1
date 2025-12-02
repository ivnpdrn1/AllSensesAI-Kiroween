# AllSenses AI Guardian - Deployment Permissions Validator
# This script validates that the provided IAM user has all required permissions

param(
  [Parameter(Mandatory=$true)] [string] $AccessKeyId,
  [Parameter(Mandatory=$true)] [string] $SecretAccessKey,
  [string] $Region = "us-east-1"
)

$env:AWS_ACCESS_KEY_ID     = $AccessKeyId
$env:AWS_SECRET_ACCESS_KEY = $SecretAccessKey
$env:AWS_DEFAULT_REGION    = $Region

function Test-Service($name, $cmd) {
  try {
    iex $cmd | Out-Null
    Write-Host "[OK] $name" -ForegroundColor Green
  } catch {
    Write-Host "[WARN] ${name}: $($_.Exception.Message)" -ForegroundColor Yellow
  }
}

try {
  Write-Host "Validating credentials and permissions..."
  iex 'aws sts get-caller-identity' | Out-Null
  Write-Host "[OK] STS identity verified" -ForegroundColor Green

  Test-Service "CloudFormation" 'aws cloudformation list-stacks --max-items 1'
  Test-Service "Lambda"        'aws lambda list-functions --max-items 1'
  Test-Service "DynamoDB"      'aws dynamodb list-tables --max-items 1'
  Test-Service "SNS"           'aws sns list-topics --max-items 1'
  Test-Service "API Gateway"   'aws apigateway get-rest-apis --limit 1'
  Test-Service "KMS"           'aws kms list-keys --limit 1'
  Test-Service "Bedrock"       'aws bedrock list-foundation-models --by-provider "amazon" --max-results 1'

} finally {
  Remove-Item Env:AWS_ACCESS_KEY_ID,Env:AWS_SECRET_ACCESS_KEY,Env:AWS_SESSION_TOKEN,Env:AWS_DEFAULT_REGION -ErrorAction SilentlyContinue
}
