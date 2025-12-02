# 🚀 AllSensesAI EUM Deployment Instructions

## Step-by-Step Deployment Guide

**Target**: AWS Lambda Function `allsenseai-complete`  
**Region**: us-east-1  
**Estimated Time**: 5 minutes

---

## ✅ Pre-Deployment Checklist

Before you begin, ensure you have:
- [ ] AWS Console access
- [ ] Lambda function exists: `allsenseai-complete`
- [ ] Lambda Function URL: `https://53x75wmoi5qtdv2gfc4sn3btzu0rivqx.lambda-url.us-east-1.on.aws/`
- [ ] Test phone number verified: +19543483664

---

## 📋 Deployment Steps

### Step 1: Deploy Lambda Code (2 minutes)

1. **Open AWS Lambda Console**
   - Go to: https://console.aws.amazon.com/lambda/
   - Region: us-east-1
   - Find function: `allsenseai-complete`

2. **Upload New Code**
   - Click "Code" tab
   - Click "Upload from" → ".zip file"
   - Create zip of `lambda/allsenseai-eum-compliant.py`:
     ```powershell
     Compress-Archive -Path "lambda/allsenseai-eum-compliant.py" -DestinationPath "lambda-code.zip" -Force
     ```
   - Upload `lambda-code.zip`
   - Click "Save"

3. **Wait for Deployment**
   - Wait 30 seconds for Lambda to update
   - Status should show "Active"

---

### Step 2: Add IAM Permissions (2 minutes)

1. **Open IAM Role**
   - In Lambda console, click "Configuration" tab
   - Click "Permissions"
   - Click the role name (e.g., `allsenseai-complete-role-...`)

2. **Add EUM Policy**
   - Click "Add permissions" → "Create inline policy"
   - Click "JSON" tab
   - Paste contents from `iam/eum-iam-policy.json`:
     ```json
     {
       "Version": "2012-10-17",
       "Statement": [
         {
           "Sid": "EUMSendSMS",
           "Effect": "Allow",
           "Action": ["sms-voice:SendTextMessage"],
           "Resource": "*"
         },
         {
           "Sid": "EUMDescribeResources",
           "Effect": "Allow",
           "Action": [
             "sms-voice:DescribePhoneNumbers",
             "sms-voice:DescribeConfigurationSets"
           ],
           "Resource": "*"
         }
       ]
     }
     ```
   - Click "Review policy"
   - Name: `EUM-SMS-Permissions`
   - Click "Create policy"

---

### Step 3: Run Verification Script (1 minute)

1. **Open PowerShell**
   - Navigate to `testing/` folder
   - Run verification script:
     ```powershell
     .\verify-eum-deployment.ps1
     ```

2. **Expected Output**
   ```
   ========================================
     EUM Deployment Verification
   ========================================

   TEST 1: Lambda Connectivity
   ✅ Correct originator number!
   ✅ Correct campaign!

   TEST 2: SMS Sending with Full Response Capture
   ✅ CHECK 1: eumCompliant = true
   ✅ CHECK 2: originatorNumber = +12173933490
   ✅ CHECK 3: campaign = AllSensesAI-SafetyAlerts
   ✅ CHECK 4: MessageId present (not demo)
   ✅ CHECK 5: smsStatus = sent
   ✅ CHECK 6: No SMS errors

   ========================================
     ✅ ALL CHECKS PASSED!
   ========================================
   ```

3. **If Checks Fail**
   - Review error messages in script output
   - Check IAM permissions were added correctly
   - Wait 30 seconds and run script again
   - See `documentation/ERROR_HANDLING_AUDIT.md` for troubleshooting

---

### Step 4: Test Frontend (Optional)

1. **Open Frontend**
   - Navigate to your frontend HTML file
   - Open in browser

2. **Trigger Test SMS**
   - Click "Send Test SMS" button
   - Check phone +19543483664 for message

3. **Expected Result**
   - SMS received within 5 seconds
   - Message from +12173933490
   - Contains "AllSensesAI" branding

---

### Step 5: Verify in AWS Dashboard

1. **Open AWS Pinpoint Console**
   - Go to: https://console.aws.amazon.com/pinpoint/
   - Region: us-east-1

2. **Check SMS Activity**
   - Click "SMS and voice" → "Phone numbers"
   - Find number: +12173933490
   - Verify message count increased

3. **View Message Details**
   - Click on phone number
   - View "Analytics" tab
   - Confirm messages appear in EUM dashboard

---

## ✅ Success Criteria

Your deployment is successful when:
- ✅ Verification script shows "ALL CHECKS PASSED"
- ✅ Test SMS received on phone
- ✅ Messages appear in AWS EUM Dashboard
- ✅ No error messages in Lambda logs

---

## 🚨 Troubleshooting

### Problem: Verification Script Fails

**Symptom**: Script shows "Lambda connectivity failed"

**Solution**:
1. Check Lambda function is deployed
2. Wait 30 seconds after deployment
3. Run script again

---

### Problem: SMS Not Received

**Symptom**: Script passes but no SMS received

**Solution**:
1. Check phone number is verified in AWS sandbox
2. Check IAM permissions were added
3. View Lambda logs in CloudWatch
4. See `documentation/ERROR_HANDLING_AUDIT.md`

---

### Problem: Wrong Originator Number

**Symptom**: Script shows "Wrong originator number"

**Solution**:
1. Verify Lambda code was uploaded correctly
2. Check `allsenseai-eum-compliant.py` has correct number
3. Redeploy Lambda code

---

## 📚 Additional Resources

- **Complete Documentation**: See `documentation/` folder
- **Error Handling**: `documentation/ERROR_HANDLING_AUDIT.md`
- **HTTP Behavior**: `documentation/HTTP_STATUS_CODE_ANALYSIS.md`
- **Verification Details**: `documentation/VERIFICATION_SCRIPT_CONFIRMED.md`

---

## 🎯 For Jury Presentation

This deployment demonstrates:
1. ✅ **EUM Compliance**: Proper use of AWS End User Messaging API
2. ✅ **10DLC Registration**: Registered phone number (+12173933490) and campaign
3. ✅ **Error Handling**: Comprehensive error responses with diagnostics
4. ✅ **Automated Testing**: Verification script validates all compliance requirements
5. ✅ **Production Ready**: Complete deployment package with documentation

---

**Deployment Package Version**: 1.0.0  
**Last Updated**: November 25, 2025  
**Status**: ✅ Ready for Deployment
