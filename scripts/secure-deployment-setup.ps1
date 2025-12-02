# AllSenses AI Guardian - Secure Live Deployment Setup
# This script configures secure AWS credentials for KIRO deployment

<#
Purpose:
- Prepare inputs for deployment without storing credentials.
- Creates a local params file with NON-secret values only.
#>

param(
  [Parameter(Mandatory=$true)] [string] $AccessKeyId,
  [Parameter(Mandatory=$true)] [string] $SecretAccessKey,
  [Parameter(Mandatory=$true)] [string] $NotificationEmail,
  [string] $Region = "us-east-1",
  [string] $Environment = "production"
)

Write-Host "Setting temporary AWS credentials (for this session only)..."
$env:AWS_ACCESS_KEY_ID     = $AccessKeyId
$env:AWS_SECRET_ACCESS_KEY = $SecretAccessKey
$env:AWS_DEFAULT_REGION    = $Region

# Create a parameters json used by the deploy step (no secrets saved)
$params = @{
  NotificationEmail = $NotificationEmail
  Environment       = $Environment
  Region            = $Region
}
$paramFile = Join-Path -Path (Resolve-Path ".") -ChildPath "deployment-params.json"
$params | ConvertTo-Json | Set-Content -Path $paramFile -Encoding ascii

Write-Host "Created $paramFile with non-secret parameters."

# Optional: quick sanity checks
try {
  aws sts get-caller-identity | Out-Null
  Write-Host "[OK] AWS identity reachable"
  aws bedrock list-foundation-models --max-results 1 | Out-Null 2>$null
  Write-Host "[OK] Bedrock endpoint reachable (model access still depends on your console setting)"
} catch {
  Write-Warning "AWS CLI sanity check failed: $($_.Exception.Message)"
}

Write-Host "Setup complete. Credentials remain only in memory for this session."
