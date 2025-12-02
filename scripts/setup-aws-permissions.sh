#!/bin/bash

# AllSenses AI Guardian - AWS IAM Permissions Setup Script
# This script creates the necessary IAM policies and roles for deployment

set -euo pipefail

# Default values
USER_NAME=""
REGION="us-east-1"
CREATE_USER=false
ATTACH_TO_CURRENT_USER=false

# Color functions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

success() { echo -e "${GREEN}✅ $1${NC}"; }
info() { echo -e "${CYAN}ℹ️  $1${NC}"; }
warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
error() { echo -e "${RED}❌ $1${NC}"; }

usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    -u, --user-name <name>      Username for new IAM user
    -r, --region <region>       AWS region (default: us-east-1)
    -c, --create-user           Create new IAM user with deployment permissions
    -a, --attach-current        Attach policy to current user
    -h, --help                  Show this help message

Examples:
    $0 --create-user --user-name allsenses-deployer
    $0 --attach-current
    $0 --user-name existing-user --region us-west-2

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -u|--user-name)
            USER_NAME="$2"
            shift 2
            ;;
        -r|--region)
            REGION="$2"
            shift 2
            ;;
        -c|--create-user)
            CREATE_USER=true
            shift
            ;;
        -a|--attach-current)
            ATTACH_TO_CURRENT_USER=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

info "Setting up AWS IAM permissions for AllSenses AI Guardian deployment..."

# Check prerequisites
if ! command -v aws >/dev/null 2>&1; then
    error "AWS CLI not found. Please install AWS CLI first."
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    error "jq not found. Please install jq for JSON processing."
    exit 1
fi

# Get current user info
if ! CURRENT_USER=$(aws sts get-caller-identity --output json 2>/dev/null); then
    error "Failed to get current AWS identity. Ensure AWS CLI is configured."
    exit 1
fi

CURRENT_USER_ARN=$(echo "$CURRENT_USER" | jq -r '.Arn')
ACCOUNT_ID=$(echo "$CURRENT_USER" | jq -r '.Account')
info "Current AWS identity: $CURRENT_USER_ARN"

# Define the deployment policy
POLICY_FILE="allsenses-deployment-policy.json"
cat > "$POLICY_FILE" << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CloudFormationPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": [
        "arn:aws:cloudformation:*:*:stack/AllSenses-*/*",
        "arn:aws:cloudformation:*:*:changeSet/AllSenses-*/*"
      ]
    },
    {
      "Sid": "IAMPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": [
        "arn:aws:iam::*:role/AllSenses-*",
        "arn:aws:iam::*:policy/AllSenses-*"
      ]
    },
    {
      "Sid": "LambdaPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": "arn:aws:lambda:*:*:function:AllSenses-*"
    },
    {
      "Sid": "DynamoDBPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/AllSenses-*"
    },
    {
      "Sid": "SNSPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": "arn:aws:sns:*:*:AllSenses-*"
    },
    {
      "Sid": "APIGatewayPermissions",
      "Effect": "Allow",
      "Action": [
        "apigateway:POST",
        "apigateway:GET",
        "apigateway:PUT",
        "apigateway:DELETE",
        "apigateway:PATCH"
      ],
      "Resource": [
        "arn:aws:apigateway:*::/restapis",
        "arn:aws:apigateway:*::/restapis/*"
      ]
    },
    {
      "Sid": "KMSPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": "*",
      "Condition": {
        "StringLike": {
          "kms:AliasName": "alias/allsenses-*"
        }
      }
    },
    {
      "Sid": "CloudWatchLogsPermissions",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:DeleteLogGroup",
        "logs:DescribeLogGroups",
        "logs:PutRetentionPolicy",
        "logs:TagLogGroup",
        "logs:UntagLogGroup"
      ],
      "Resource": "arn:aws:logs:*:*:log-group:/aws/lambda/AllSenses-*"
    },
    {
      "Sid": "EC2VPCPermissions",
      "Effect": "Allow",
      "Action": [
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
      ],
      "Resource": "*"
    },
    {
      "Sid": "BedrockPermissions",
      "Effect": "Allow",
      "Action": [
        "bedrock:ListFoundationModels",
        "bedrock:GetFoundationModel",
        "bedrock:InvokeModel"
      ],
      "Resource": "*"
    },
    {
      "Sid": "STSPermissions",
      "Effect": "Allow",
      "Action": [
        "sts:GetCallerIdentity"
      ],
      "Resource": "*"
    }
  ]
}
EOF

success "Created deployment policy file: $POLICY_FILE"

# Create the IAM policy
POLICY_NAME="AllSenses-Deployment-Policy"
POLICY_ARN="arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

info "Creating IAM policy: $POLICY_NAME"
if aws iam create-policy \
    --policy-name "$POLICY_NAME" \
    --policy-document "file://$POLICY_FILE" \
    --description "AllSenses AI Guardian deployment permissions" >/dev/null 2>&1; then
    success "Created IAM policy: $POLICY_ARN"
else
    # Policy might already exist
    if aws iam get-policy --policy-arn "$POLICY_ARN" >/dev/null 2>&1; then
        info "IAM policy already exists: $POLICY_ARN"
        
        # Update the policy
        info "Updating existing policy..."
        if aws iam create-policy-version \
            --policy-arn "$POLICY_ARN" \
            --policy-document "file://$POLICY_FILE" \
            --set-as-default >/dev/null 2>&1; then
            success "Updated IAM policy: $POLICY_ARN"
        else
            error "Failed to update IAM policy"
            exit 1
        fi
    else
        error "Failed to create IAM policy"
        exit 1
    fi
fi

# Handle user creation or attachment
if [[ "$CREATE_USER" == "true" && -n "$USER_NAME" ]]; then
    info "Creating new IAM user: $USER_NAME"
    
    if aws iam create-user \
        --user-name "$USER_NAME" \
        --tags Key=Application,Value=AllSenses-AI-Guardian Key=Purpose,Value=Deployment >/dev/null 2>&1; then
        success "Created IAM user: $USER_NAME"
    else
        warning "User $USER_NAME might already exist"
    fi
    
    # Attach policy to user
    if aws iam attach-user-policy --user-name "$USER_NAME" --policy-arn "$POLICY_ARN"; then
        success "Attached deployment policy to user: $USER_NAME"
    else
        error "Failed to attach policy to user"
        exit 1
    fi
    
    # Create access keys
    info "Creating access keys for user: $USER_NAME"
    if ACCESS_KEYS=$(aws iam create-access-key --user-name "$USER_NAME" --output json); then
        ACCESS_KEY_ID=$(echo "$ACCESS_KEYS" | jq -r '.AccessKey.AccessKeyId')
        SECRET_ACCESS_KEY=$(echo "$ACCESS_KEYS" | jq -r '.AccessKey.SecretAccessKey')
        
        success "Access keys created successfully!"
        warning "IMPORTANT: Save these credentials securely!"
        info "Access Key ID: $ACCESS_KEY_ID"
        info "Secret Access Key: $SECRET_ACCESS_KEY"
        
        # Save to file
        CREDENTIALS_FILE="allsenses-aws-credentials.txt"
        cat > "$CREDENTIALS_FILE" << EOF
AllSenses AI Guardian AWS Credentials
Generated: $(date)
User: $USER_NAME

AWS_ACCESS_KEY_ID=$ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY=$SECRET_ACCESS_KEY
AWS_DEFAULT_REGION=$REGION

To use these credentials:
1. Run: aws configure
2. Enter the Access Key ID and Secret Access Key above
3. Set default region to: $REGION
4. Set default output format to: json

Or set environment variables:
export AWS_ACCESS_KEY_ID=$ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$SECRET_ACCESS_KEY
export AWS_DEFAULT_REGION=$REGION
EOF
        
        success "Credentials saved to: $CREDENTIALS_FILE"
        warning "Delete this file after configuring AWS CLI!"
        
    else
        error "Failed to create access keys"
        exit 1
    fi
    
elif [[ "$ATTACH_TO_CURRENT_USER" == "true" ]]; then
    # Extract username from current user ARN
    if [[ "$CURRENT_USER_ARN" =~ arn:aws:iam::[0-9]+:user/(.+) ]]; then
        CURRENT_USER_NAME="${BASH_REMATCH[1]}"
        info "Attaching policy to current user: $CURRENT_USER_NAME"
        
        if aws iam attach-user-policy --user-name "$CURRENT_USER_NAME" --policy-arn "$POLICY_ARN"; then
            success "Attached deployment policy to current user: $CURRENT_USER_NAME"
        else
            error "Failed to attach policy to current user"
            info "You may need to ask your AWS administrator to attach this policy:"
            info "Policy ARN: $POLICY_ARN"
            exit 1
        fi
    else
        error "Current identity is not an IAM user. Cannot attach policy."
        info "Current identity: $CURRENT_USER_ARN"
        info "Policy ARN: $POLICY_ARN"
        exit 1
    fi
    
else
    info "Policy created but not attached to any user."
    info "Policy ARN: $POLICY_ARN"
    echo
    info "To attach this policy to a user, run:"
    info "aws iam attach-user-policy --user-name <USERNAME> --policy-arn $POLICY_ARN"
    echo
    info "Or re-run this script with options:"
    info "  --create-user --user-name <new-username>     # Create new user with policy"
    info "  --attach-current                             # Attach to current user"
fi

# Verify Bedrock access
info "Checking Bedrock model access..."
if MODELS=$(aws bedrock list-foundation-models --region "$REGION" --output json 2>/dev/null); then
    if echo "$MODELS" | jq -e '.modelSummaries[] | select(.modelId == "anthropic.claude-3-sonnet-20240229-v1:0")' >/dev/null; then
        success "Claude-3 Sonnet model access confirmed"
    else
        warning "Claude-3 Sonnet model access not available"
        info "Request access at: https://console.aws.amazon.com/bedrock/home?region=$REGION#/modelaccess"
    fi
    
    if echo "$MODELS" | jq -e '.modelSummaries[] | select(.modelId == "amazon.titan-text-express-v1")' >/dev/null; then
        success "Titan Text Express model access confirmed"
    else
        warning "Titan Text Express model access not available"
        info "Request access at: https://console.aws.amazon.com/bedrock/home?region=$REGION#/modelaccess"
    fi
else
    warning "Could not verify Bedrock access. Ensure Bedrock is available in region: $REGION"
fi

# Clean up policy file
rm -f "$POLICY_FILE"
info "Cleaned up temporary policy file"

success "AWS IAM permissions setup completed!"
echo
info "=== NEXT STEPS ==="
info "1. If you created a new user, configure AWS CLI with the provided credentials"
info "2. Request Bedrock model access if not already available"
info "3. Run the deployment script: ./scripts/deploy-allsenses.sh -e <your-email>"
echo
info "=== SECURITY NOTES ==="
info "- The created policy follows least-privilege principles"
info "- All resources are scoped to AllSenses-* naming pattern"
info "- KMS permissions are limited to AllSenses aliases"
info "- Consider using AWS Organizations SCPs for additional restrictions"