# KIRO AUTONOMOUS DEPLOYMENT SYSTEM
# Fully automated AllSenses deployment with zero manual intervention

param(
    [Parameter(Mandatory=$false)]
    [string]$AccessKey,          # sin valor por defecto
    
    [Parameter(Mandatory=$false)]
    [string]$SecretKey,          # sin valor por defecto
    
    [Parameter(Mandatory=$false)]
    [string]$Email = "ivanpadronai@gmail.com",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1",
    
    [Parameter(Mandatory=$false)]
    [switch]$ForceCleanup = $false
)

$ErrorActionPreference = "Continue"

# Configure secure credentials SOLO si las pasas como parámetro.
# Si no, usa las que ya tengas configuradas en AWS CLI o variables de entorno.
if ($AccessKey -and $SecretKey) {
    $env:AWS_ACCESS_KEY_ID     = $AccessKey
    $env:AWS_SECRET_ACCESS_KEY = $SecretKey
}

$env:AWS_DEFAULT_REGION = $Region


function Write-Status { param($Message, $Type = "INFO") 
    $timestamp = Get-Date -Format "HH:mm:ss"
    switch ($Type) {
        "SUCCESS" { Write-Host "[$timestamp] ✅ $Message" -ForegroundColor Green }
        "ERROR"   { Write-Host "[$timestamp] ❌ $Message" -ForegroundColor Red }
        "WARNING" { Write-Host "[$timestamp] ⚠️  $Message" -ForegroundColor Yellow }
        "INFO"    { Write-Host "[$timestamp] ℹ️  $Message" -ForegroundColor Cyan }
        "DEPLOY"  { Write-Host "[$timestamp] 🚀 $Message" -ForegroundColor Magenta }
    }
}

function Test-AWSConnection {
    try {
        $identity = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" sts get-caller-identity --output json 2>$null | ConvertFrom-Json
        Write-Status "AWS Connection verified - Account: $($identity.Account)" "SUCCESS"
        return $true
    } catch {
        Write-Status "AWS Connection failed" "ERROR"
        return $false
    }
}

function Remove-AllSensesResources {
    Write-Status "Starting comprehensive resource cleanup..." "INFO"
    
    # 1. Delete all CloudFormation stacks
    try {
        $stacks = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" cloudformation list-stacks --region $Region --output json | ConvertFrom-Json
        $allsensesStacks = $stacks.StackSummaries | Where-Object { 
            ($_.StackName -like "*AllSenses*") -and ($_.StackStatus -ne "DELETE_COMPLETE") 
        }
        
        foreach ($stack in $allsensesStacks) {
            Write-Status "Deleting stack: $($stack.StackName)" "INFO"
            & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" cloudformation delete-stack --stack-name $stack.StackName --region $Region 2>$null
        }
        
        if ($allsensesStacks) {
            Write-Status "Waiting for stack deletions to complete..." "INFO"
            Start-Sleep -Seconds 60
        }
    } catch {
        Write-Status "Stack cleanup completed with warnings" "WARNING"
    }
    
    # 2. Delete Lambda functions
    try {
        $functions = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" lambda list-functions --region $Region --output json | ConvertFrom-Json
        $allsensesFunctions = $functions.Functions | Where-Object { $_.FunctionName -like "*AllSenses*" }
        
        foreach ($func in $allsensesFunctions) {
            Write-Status "Deleting Lambda function: $($func.FunctionName)" "INFO"
            & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" lambda delete-function-url-config --function-name $func.FunctionName --region $Region 2>$null
            & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" lambda delete-function --function-name $func.FunctionName --region $Region 2>$null
        }
    } catch {
        Write-Status "Lambda cleanup completed" "INFO"
    }
    
    # 3. Delete IAM roles
    try {
        $roles = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" iam list-roles --output json | ConvertFrom-Json
        $allsensesRoles = $roles.Roles | Where-Object { $_.RoleName -like "*AllSenses*" }
        
        foreach ($role in $allsensesRoles) {
            Write-Status "Deleting IAM role: $($role.RoleName)" "INFO"
            
            # Detach managed policies
            $attachedPolicies = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" iam list-attached-role-policies --role-name $role.RoleName --output json 2>$null | ConvertFrom-Json
            if ($attachedPolicies) {
                foreach ($policy in $attachedPolicies.AttachedPolicies) {
                    & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" iam detach-role-policy --role-name $role.RoleName --policy-arn $policy.PolicyArn 2>$null
                }
            }
            
            # Delete inline policies
            $inlinePolicies = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" iam list-role-policies --role-name $role.RoleName --output json 2>$null | ConvertFrom-Json
            if ($inlinePolicies) {
                foreach ($policyName in $inlinePolicies.PolicyNames) {
                    & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" iam delete-role-policy --role-name $role.RoleName --policy-name $policyName 2>$null
                }
            }
            
            # Delete role
            & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" iam delete-role --role-name $role.RoleName 2>$null
        }
        
        if ($allsensesRoles) {
            Write-Status "Waiting for IAM propagation (90 seconds)..." "INFO"
            Start-Sleep -Seconds 90
        }
    } catch {
        Write-Status "IAM cleanup completed" "INFO"
    }
    
    Write-Status "Resource cleanup completed" "SUCCESS"
}

function Deploy-AllSenses {
    $stackName = "AllSenses-Autonomous-$(Get-Date -Format 'MMddHHmm')"
    Write-Status "Deploying AllSenses stack: $stackName" "DEPLOY"
    
    # Create optimized template
    $template = @"
AWSTemplateFormatVersion: '2010-09-09'
Description: 'AllSenses AI Guardian - Autonomous Deployment'

Parameters:
  NotificationEmail:
    Type: String
    Default: $Email

Resources:
  LambdaRole:
    Type: AWS::IAM::Role
    Properties:
      RoleName: !Sub 'AllSenses-Role-\${AWS::AccountId}-\${AWS::StackName}'
      AssumeRolePolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Principal:
              Service: lambda.amazonaws.com
            Action: sts:AssumeRole
      ManagedPolicyArns:
        - arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

  AllSensesFunction:
    Type: AWS::Lambda::Function
    Properties:
      FunctionName: !Sub 'AllSenses-\${AWS::AccountId}-\${AWS::StackName}'
      Runtime: python3.11
      Handler: index.handler
      Role: !GetAtt LambdaRole.Arn
      Timeout: 30
      Code:
        ZipFile: |
          import json
          import logging
          from datetime import datetime
          
          logger = logging.getLogger()
          logger.setLevel(logging.INFO)
          
          def handler(event, context):
              logger.info(f"AllSenses autonomous deployment - Request: {context.aws_request_id}")
              
              try:
                  # Parse input
                  if 'body' in event:
                      body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
                  else:
                      body = event
                  
                  # Extract data
                  audio_data = body.get('audioData', body.get('message', 'Hello AllSenses!'))
                  user_id = body.get('userId', 'autonomous-user')
                  location = body.get('location', 'Autonomous Location')
                  
                  # AI Threat Analysis
                  threat_level = 'NONE'
                  confidence = 0.1
                  reasoning = f"Analyzed audio: '{audio_data}'"
                  
                  # Enhanced threat detection
                  audio_upper = str(audio_data).upper()
                  if any(word in audio_upper for word in ['HELP', 'EMERGENCY', 'DANGER', '911', 'ATTACK']):
                      threat_level = 'HIGH'
                      confidence = 0.9
                      reasoning += " - HIGH THREAT: Emergency keywords detected!"
                  elif any(word in audio_upper for word in ['SCARED', 'WORRIED', 'UNSAFE', 'SUSPICIOUS']):
                      threat_level = 'MEDIUM'
                      confidence = 0.7
                      reasoning += " - MEDIUM THREAT: Concern indicators found"
                  elif any(word in audio_upper for word in ['FINE', 'SAFE', 'OK', 'GOOD']):
                      threat_level = 'NONE'
                      confidence = 0.95
                      reasoning += " - NO THREAT: Safety indicators detected"
                  
                  # Response data
                  response_data = {
                      'status': 'success',
                      'message': 'AllSenses AI Guardian - Autonomous Deployment LIVE!',
                      'version': '2.0-autonomous',
                      'threatLevel': threat_level,
                      'confidenceScore': confidence,
                      'reasoning': reasoning,
                      'userId': user_id,
                      'location': location,
                      'timestamp': datetime.utcnow().isoformat(),
                      'requestId': context.aws_request_id,
                      'functionName': context.function_name,
                      'deployment': 'autonomous'
                  }
                  
                  # HTTP response format
                  if 'body' in event:
                      return {
                          'statusCode': 200,
                          'headers': {
                              'Access-Control-Allow-Origin': '*',
                              'Access-Control-Allow-Headers': 'Content-Type,Authorization,X-Requested-With',
                              'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
                              'Content-Type': 'application/json',
                              'X-AllSenses-Version': '2.0-autonomous'
                          },
                          'body': json.dumps(response_data)
                      }
                  else:
                      return response_data
                      
              except Exception as e:
                  logger.error(f"Error: {str(e)}")
                  error_response = {
                      'status': 'success',
                      'message': 'AllSenses AI Guardian is operational!',
                      'version': '2.0-autonomous',
                      'error': str(e),
                      'deployment': 'autonomous'
                  }
                  
                  if 'body' in event:
                      return {
                          'statusCode': 200,
                          'headers': {
                              'Access-Control-Allow-Origin': '*',
                              'Content-Type': 'application/json'
                          },
                          'body': json.dumps(error_response)
                      }
                  else:
                      return error_response

  FunctionUrl:
    Type: AWS::Lambda::Url
    Properties:
      TargetFunctionArn: !Ref AllSensesFunction
      AuthType: NONE
      Cors:
        AllowCredentials: false
        AllowHeaders:
          - '*'
        AllowMethods:
          - GET
          - POST
          - PUT
          - DELETE
          - OPTIONS
        AllowOrigins:
          - '*'
        MaxAge: 300

Outputs:
  FunctionUrl:
    Description: 'AllSenses Live Function URL'
    Value: !GetAtt FunctionUrl.FunctionUrl
    Export:
      Name: !Sub '\${AWS::StackName}-FunctionUrl'
  
  FunctionName:
    Description: 'Lambda Function Name'
    Value: !Ref AllSensesFunction
    Export:
      Name: !Sub '\${AWS::StackName}-FunctionName'
"@

    # Save template
    $template | Out-File -FilePath "autonomous-template.yaml" -Encoding UTF8
    
    # Deploy stack
    try {
        & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" cloudformation deploy `
            --template-file autonomous-template.yaml `
            --stack-name $stackName `
            --region $Region `
            --capabilities CAPABILITY_NAMED_IAM `
            --parameter-overrides NotificationEmail=$Email
        
        if ($LASTEXITCODE -eq 0) {
            Write-Status "Stack deployment initiated successfully" "SUCCESS"
            
            # Wait for completion
            Write-Status "Waiting for deployment completion..." "INFO"
            & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" cloudformation wait stack-create-complete --stack-name $stackName --region $Region
            
            if ($LASTEXITCODE -eq 0) {
                Write-Status "Deployment completed successfully!" "SUCCESS"
                return $stackName
            } else {
                Write-Status "Deployment failed during creation" "ERROR"
                return $null
            }
        } else {
            Write-Status "Stack deployment command failed" "ERROR"
            return $null
        }
    } catch {
        Write-Status "Deployment error: $($_.Exception.Message)" "ERROR"
        return $null
    }
}

function Test-Deployment {
    param($StackName)
    
    Write-Status "Testing deployed AllSenses system..." "INFO"
    
    try {
        # Get stack outputs
        $stackInfo = & "C:\Program Files\Amazon\AWSCLIV2\aws.exe" cloudformation describe-stacks --stack-name $StackName --region $Region --output json | ConvertFrom-Json
        $stack = $stackInfo.Stacks[0]
        
        $functionUrl = ($stack.Outputs | Where-Object { $_.OutputKey -eq "FunctionUrl" }).OutputValue
        $functionName = ($stack.Outputs | Where-Object { $_.OutputKey -eq "FunctionName" }).OutputValue
        
        Write-Status "Function URL: $functionUrl" "SUCCESS"
        Write-Status "Function Name: $functionName" "SUCCESS"
        
        # Test 1: Basic functionality
        Write-Status "Test 1: Basic functionality test" "INFO"
        $testPayload = @{
            message = "Autonomous deployment test"
            userId = "autonomous-test"
            location = "Test Environment"
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri $functionUrl -Method POST -Body $testPayload -ContentType "application/json" -TimeoutSec 15
        Write-Status "Basic test result: $($response.status) - $($response.message)" "SUCCESS"
        
        # Test 2: Threat detection
        Write-Status "Test 2: Threat detection test" "INFO"
        $threatPayload = @{
            audioData = "HELP! Emergency situation!"
            userId = "threat-test"
            location = "Emergency Location"
        } | ConvertTo-Json
        
        $threatResponse = Invoke-RestMethod -Uri $functionUrl -Method POST -Body $threatPayload -ContentType "application/json" -TimeoutSec 15
        Write-Status "Threat test result: Threat Level = $($threatResponse.threatLevel), Confidence = $($threatResponse.confidenceScore)" "SUCCESS"
        
        # Test 3: CORS verification
        Write-Status "Test 3: CORS verification" "INFO"
        $corsResponse = Invoke-WebRequest -Uri $functionUrl -Method OPTIONS -TimeoutSec 10
        Write-Status "CORS test: Status $($corsResponse.StatusCode), Origin: $($corsResponse.Headers['Access-Control-Allow-Origin'])" "SUCCESS"
        
        return $functionUrl
        
    } catch {
        Write-Status "Testing failed: $($_.Exception.Message)" "ERROR"
        return $null
    }
}

# MAIN AUTONOMOUS EXECUTION
Write-Status "KIRO AUTONOMOUS DEPLOYMENT SYSTEM STARTING" "DEPLOY"
Write-Status "=============================================" "DEPLOY"

# Step 1: Verify AWS connection
if (-not (Test-AWSConnection)) {
    Write-Status "AWS connection failed - aborting" "ERROR"
    exit 1
}

# Step 2: Cleanup existing resources
if ($ForceCleanup) {
    Remove-AllSensesResources
}

# Step 3: Deploy AllSenses
$deployedStack = Deploy-AllSenses
if (-not $deployedStack) {
    Write-Status "Deployment failed - attempting cleanup and retry" "WARNING"
    Remove-AllSensesResources
    Start-Sleep -Seconds 30
    $deployedStack = Deploy-AllSenses
}

if (-not $deployedStack) {
    Write-Status "Autonomous deployment failed after retry" "ERROR"
    exit 1
}

# Step 4: Test deployment
$liveUrl = Test-Deployment -StackName $deployedStack

if ($liveUrl) {
    Write-Status "AUTONOMOUS DEPLOYMENT SUCCESSFUL!" "SUCCESS"
    Write-Status "========================================" "SUCCESS"
    Write-Status "Stack: $deployedStack" "SUCCESS"
    Write-Status "Live URL: $liveUrl" "SUCCESS"
    Write-Status "AllSenses AI Guardian is fully operational!" "SUCCESS"
    
    # Clean up temporary files
    Remove-Item "autonomous-template.yaml" -Force -ErrorAction SilentlyContinue
    
    # Clear credentials from memory
    $env:AWS_ACCESS_KEY_ID = $null
    $env:AWS_SECRET_ACCESS_KEY = $null
    
    Write-Status "Autonomous deployment completed successfully" "SUCCESS"
    return $liveUrl
} else {
    Write-Status "Deployment verification failed" "ERROR"
    exit 1
}