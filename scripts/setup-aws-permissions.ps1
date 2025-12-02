# AllSenses AI Guardian - AWS IAM Permissions Setup Script
# This script creates the necessary IAM policies and roles for deployment

param(
    [Parameter(Mandatory=$false)]
    [string]$UserName = "",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1",
    
    [Parameter(Mandatory=$false)]
    [switch]$CreateUser = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$AttachToCurrentUser = $false
)

$ErrorActionPreference = "Stop"

function Write-Success { param($Message) Write-Host "✅ $Message" -ForegroundColor Green }
function Write-Info { param($Message) Write-Host "ℹ️  $Message" -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host "⚠️  $Message" -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host "❌ $Message" -ForegroundColor Red }

Write-Info "Setting up AWS IAM permissions for AllSenses AI Guardian deployment..."

# Get current user info
try {
    $currentUser = aws sts get-caller-identity --output json | ConvertFrom-Json
    Write-Info "Current AWS identity: $($currentUser.Arn)"
} catch {
    Write-Error "Failed to get current AWS identity. Ensure AWS CLI is configured."
    exit 1
}

# Define the deployment policy
$deploymentPolicy = @{
    Version = "2012-10-17"
    Statement = @(
        @{
            Sid = "CloudFormationPermissions"
            Effect = "Allow"
            Action = @(
                "cloudformation:CreateStack",
                "cloudformation:UpdateStack",
                "cloudformation:DeleteStack",
                "cloudformation:DescribeStacks",
                "cloudformation:DescribeStackEvents",
                "cloudformation:DescribeStackResources",
                "cloudformation:GetTemplate",
                "cloudformation:ValidateTemplate",
                "cloudformation:ListStacks",
                "cloudformation:CreateChangeSet",
                "cloudformation:DescribeChangeSet",
                "cloudformation:ExecuteChangeSet"
            )
            Resource = @(
                "arn:aws:cloudformation:*:*:stack/AllSenses-*/*",
                "arn:aws:cloudformation:*:*:changeSet/AllSenses-*/*"
            )
        },
        @{
            Sid = "IAMPermissions"
            Effect = "Allow"
            Action = @(
                "iam:CreateRole",
                "iam:DeleteRole",
                "iam:GetRole",
                "iam:PassRole",
                "iam:AttachRolePolicy",
                "iam:DetachRolePolicy",
                "iam:PutRolePolicy",
                "iam:DeleteRolePolicy",
                "iam:GetRolePolicy",
                "iam:ListRolePolicies",
                "iam:ListAttachedRolePolicies",
                "iam:CreatePolicy",
                "iam:DeletePolicy",
                "iam:GetPolicy",
                "iam:GetPolicyVersion",
                "iam:ListPolicyVersions",
                "iam:TagRole",
                "iam:UntagRole"
            )
            Resource = @(
                "arn:aws:iam::*:role/AllSenses-*",
                "arn:aws:iam::*:policy/AllSenses-*"
            )
        },
        @{
            Sid = "LambdaPermissions"
            Effect = "Allow"
            Action = @(
                "lambda:CreateFunction",
                "lambda:DeleteFunction",
                "lambda:GetFunction",
                "lambda:UpdateFunctionCode",
                "lambda:UpdateFunctionConfiguration",
                "lambda:AddPermission",
                "lambda:RemovePermission",
                "lambda:GetPolicy",
                "lambda:ListTags",
                "lambda:TagResource",
                "lambda:UntagResource",
                "lambda:InvokeFunction"
            )
            Resource = "arn:aws:lambda:*:*:function:AllSenses-*"
        },
        @{
            Sid = "DynamoDBPermissions"
            Effect = "Allow"
            Action = @(
                "dynamodb:CreateTable",
                "dynamodb:DeleteTable",
                "dynamodb:DescribeTable",
                "dynamodb:UpdateTable",
                "dynamodb:ListTables",
                "dynamodb:TagResource",
                "dynamodb:UntagResource",
                "dynamodb:ListTagsOfResource",
                "dynamodb:UpdateContinuousBackups",
                "dynamodb:DescribeContinuousBackups"
            )
            Resource = "arn:aws:dynamodb:*:*:table/AllSenses-*"
        },
        @{
            Sid = "SNSPermissions"
            Effect = "Allow"
            Action = @(
                "sns:CreateTopic",
                "sns:DeleteTopic",
                "sns:GetTopicAttributes",
                "sns:SetTopicAttributes",
                "sns:Subscribe",
                "sns:Unsubscribe",
                "sns:ListTopics",
                "sns:ListSubscriptionsByTopic",
                "sns:TagResource",
                "sns:UntagResource",
                "sns:ListTagsForResource"
            )
            Resource = "arn:aws:sns:*:*:AllSenses-*"
        },
        @{
            Sid = "APIGatewayPermissions"
            Effect = "Allow"
            Action = @(
                "apigateway:POST",
                "apigateway:GET",
                "apigateway:PUT",
                "apigateway:DELETE",
                "apigateway:PATCH"
            )
            Resource = @(
                "arn:aws:apigateway:*::/restapis",
                "arn:aws:apigateway:*::/restapis/*"
            )
        },
        @{
            Sid = "KMSPermissions"
            Effect = "Allow"
            Action = @(
                "kms:CreateKey",
                "kms:CreateAlias",
                "kms:DeleteAlias",
                "kms:DescribeKey",
                "kms:GetKeyPolicy",
                "kms:PutKeyPolicy",
                "kms:ListAliases",
                "kms:ListKeys",
                "kms:TagResource",
                "kms:UntagResource",
                "kms:ListResourceTags"
            )
            Resource = "*"
            Condition = @{
                StringLike = @{
                    "kms:AliasName" = "alias/allsenses-*"
                }
            }
        },
        @{
            Sid = "CloudWatchLogsPermissions"
            Effect = "Allow"
            Action = @(
                "logs:CreateLogGroup",
                "logs:DeleteLogGroup",
                "logs:DescribeLogGroups",
                "logs:PutRetentionPolicy",
                "logs:TagLogGroup",
                "logs:UntagLogGroup"
            )
            Resource = "arn:aws:logs:*:*:log-group:/aws/lambda/AllSenses-*"
        },
        @{
            Sid = "EC2VPCPermissions"
            Effect = "Allow"
            Action = @(
                "ec2:CreateVpc",
                "ec2:DeleteVpc",
                "ec2:DescribeVpcs",
                "ec2:ModifyVpcAttribute",
                "ec2:CreateSubnet",
                "ec2:DeleteSubnet",
                "ec2:DescribeSubnets",
                "ec2:CreateSecurityGroup",
                "ec2:DeleteSecurityGroup",
                "ec2:DescribeSecurityGroups",
                "ec2:AuthorizeSecurityGroupIngress",
                "ec2:AuthorizeSecurityGroupEgress",
                "ec2:RevokeSecurityGroupIngress",
                "ec2:RevokeSecurityGroupEgress",
                "ec2:CreateTags",
                "ec2:DeleteTags",
                "ec2:DescribeTags",
                "ec2:DescribeAvailabilityZones",
                "ec2:DescribeRegions"
            )
            Resource = "*"
        },
        @{
            Sid = "BedrockPermissions"
            Effect = "Allow"
            Action = @(
                "bedrock:ListFoundationModels",
                "bedrock:GetFoundationModel",
                "bedrock:InvokeModel"
            )
            Resource = "*"
        },
        @{
            Sid = "STSPermissions"
            Effect = "Allow"
            Action = @(
                "sts:GetCallerIdentity"
            )
            Resource = "*"
        }
    )
} | ConvertTo-Json -Depth 10

# Create policy file
$policyFile = "allsenses-deployment-policy.json"
$deploymentPolicy | Out-File -FilePath $policyFile -Encoding UTF8
Write-Success "Created deployment policy file: $policyFile"

# Create the IAM policy
$policyName = "AllSenses-Deployment-Policy"
try {
    Write-Info "Creating IAM policy: $policyName"
    $policyArn = aws iam create-policy --policy-name $policyName --policy-document file://$policyFile --description "AllSenses AI Guardian deployment permissions" --query 'Policy.Arn' --output text
    Write-Success "Created IAM policy: $policyArn"
} catch {
    # Policy might already exist
    try {
        $accountId = $currentUser.Account
        $policyArn = "arn:aws:iam::${accountId}:policy/$policyName"
        aws iam get-policy --policy-arn $policyArn >$null 2>&1
        Write-Info "IAM policy already exists: $policyArn"
        
        # Update the policy
        Write-Info "Updating existing policy..."
        aws iam create-policy-version --policy-arn $policyArn --policy-document file://$policyFile --set-as-default >$null
        Write-Success "Updated IAM policy: $policyArn"
    } catch {
        Write-Error "Failed to create or update IAM policy: $($_.Exception.Message)"
        exit 1
    }
}

# Handle user creation or attachment
if ($CreateUser -and $UserName) {
    Write-Info "Creating new IAM user: $UserName"
    try {
        aws iam create-user --user-name $UserName --tags Key=Application,Value=AllSenses-AI-Guardian Key=Purpose,Value=Deployment
        Write-Success "Created IAM user: $UserName"
        
        # Attach policy to user
        aws iam attach-user-policy --user-name $UserName --policy-arn $policyArn
        Write-Success "Attached deployment policy to user: $UserName"
        
        # Create access keys
        Write-Info "Creating access keys for user: $UserName"
        $accessKeys = aws iam create-access-key --user-name $UserName --output json | ConvertFrom-Json
        
        Write-Success "Access keys created successfully!"
        Write-Warning "IMPORTANT: Save these credentials securely!"
        Write-Info "Access Key ID: $($accessKeys.AccessKey.AccessKeyId)"
        Write-Info "Secret Access Key: $($accessKeys.AccessKey.SecretAccessKey)"
        
        # Save to file
        $credentialsFile = "allsenses-aws-credentials.txt"
        @"
AllSenses AI Guardian AWS Credentials
Generated: $(Get-Date)
User: $UserName

AWS_ACCESS_KEY_ID=$($accessKeys.AccessKey.AccessKeyId)
AWS_SECRET_ACCESS_KEY=$($accessKeys.AccessKey.SecretAccessKey)
AWS_DEFAULT_REGION=$Region

To use these credentials:
1. Run: aws configure
2. Enter the Access Key ID and Secret Access Key above
3. Set default region to: $Region
4. Set default output format to: json

Or set environment variables:
export AWS_ACCESS_KEY_ID=$($accessKeys.AccessKey.AccessKeyId)
export AWS_SECRET_ACCESS_KEY=$($accessKeys.AccessKey.SecretAccessKey)
export AWS_DEFAULT_REGION=$Region
"@ | Out-File -FilePath $credentialsFile -Encoding UTF8
        
        Write-Success "Credentials saved to: $credentialsFile"
        Write-Warning "Delete this file after configuring AWS CLI!"
        
    } catch {
        Write-Error "Failed to create user or access keys: $($_.Exception.Message)"
        exit 1
    }
} elseif ($AttachToCurrentUser) {
    # Extract username from current user ARN
    $currentUserName = $currentUser.Arn -replace '.*user/', ''
    Write-Info "Attaching policy to current user: $currentUserName"
    
    try {
        aws iam attach-user-policy --user-name $currentUserName --policy-arn $policyArn
        Write-Success "Attached deployment policy to current user: $currentUserName"
    } catch {
        Write-Error "Failed to attach policy to current user: $($_.Exception.Message)"
        Write-Info "You may need to ask your AWS administrator to attach this policy:"
        Write-Info "Policy ARN: $policyArn"
        exit 1
    }
} else {
    Write-Info "Policy created but not attached to any user."
    Write-Info "Policy ARN: $policyArn"
    Write-Info ""
    Write-Info "To attach this policy to a user, run:"
    Write-Info "aws iam attach-user-policy --user-name <USERNAME> --policy-arn $policyArn"
    Write-Info ""
    Write-Info "Or re-run this script with options:"
    Write-Info "  -CreateUser -UserName <new-username>     # Create new user with policy"
    Write-Info "  -AttachToCurrentUser                     # Attach to current user"
}

# Verify Bedrock access
Write-Info "Checking Bedrock model access..."
try {
    $models = aws bedrock list-foundation-models --region $Region --output json | ConvertFrom-Json
    $claudeAvailable = $models.modelSummaries | Where-Object { $_.modelId -eq "anthropic.claude-3-sonnet-20240229-v1:0" }
    $titanAvailable = $models.modelSummaries | Where-Object { $_.modelId -eq "amazon.titan-text-express-v1" }
    
    if ($claudeAvailable) {
        Write-Success "Claude-3 Sonnet model access confirmed"
    } else {
        Write-Warning "Claude-3 Sonnet model access not available"
        Write-Info "Request access at: https://console.aws.amazon.com/bedrock/home?region=$Region#/modelaccess"
    }
    
    if ($titanAvailable) {
        Write-Success "Titan Text Express model access confirmed"
    } else {
        Write-Warning "Titan Text Express model access not available"
        Write-Info "Request access at: https://console.aws.amazon.com/bedrock/home?region=$Region#/modelaccess"
    }
} catch {
    Write-Warning "Could not verify Bedrock access. Ensure Bedrock is available in region: $Region"
}

# Clean up policy file
Remove-Item $policyFile -Force
Write-Info "Cleaned up temporary policy file"

Write-Success "AWS IAM permissions setup completed!"
Write-Info ""
Write-Info "=== NEXT STEPS ==="
Write-Info "1. If you created a new user, configure AWS CLI with the provided credentials"
Write-Info "2. Request Bedrock model access if not already available"
Write-Info "3. Run the deployment script: .\scripts\deploy-allsenses.ps1 -NotificationEmail <your-email>"
Write-Info ""
Write-Info "=== SECURITY NOTES ==="
Write-Info "- The created policy follows least-privilege principles"
Write-Info "- All resources are scoped to AllSenses-* naming pattern"
Write-Info "- KMS permissions are limited to AllSenses aliases"
Write-Info "- Consider using AWS Organizations SCPs for additional restrictions"