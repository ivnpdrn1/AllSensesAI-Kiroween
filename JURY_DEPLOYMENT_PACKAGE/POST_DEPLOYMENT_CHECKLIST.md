# ✅ Post-Deployment Checklist

## What You MUST See If Everything Is OK

### 1. Verification Script Output

**Run**: `.\verify-eum-deployment.ps1`

**Expected Output**:
```
========================================
  EUM Deployment Verification
========================================

TEST 1: Lambda Connectivity
Lambda Response Received:
  Originator: +12173933490
  Campaign: AllSensesAI-SafetyAlerts
  Message Type: TRANSACTIONAL
  Region: us-east-1
  ✅ Correct originator number!
  ✅ Correct campaign!

========================================

TEST 2: SMS Sending with Full Response Capture
SMS Response Received:

CRITICAL VERIFICATION CHECKS:

✅ CHECK 1: eumCompliant = true
✅ CHECK 2: originatorNumber = +12173933490
✅ CHECK 3: campaign = AllSensesAI-SafetyAlerts
✅ CHECK 4: MessageId present = us-east-1-abc123...
   ✅ Real MessageId (not demo)
✅ CHECK 5: smsStatus = sent
✅ CHECK 6: No SMS errors

========================================
  ✅ ALL CHECKS PASSED!
========================================

Lambda is EUM-compliant and working correctly!
Messages SHOULD appear in EUM Dashboard.
```

---

### 2. SMS on Your Phone

**Phone**: +19543483664

**Expected SMS**:
```
AllSensesAI Test Message via AWS End User Messaging. Time: 14:23:45
```

**From**: +12173933490

**Timing**: Received within 5 seconds of script execution

---

### 3. EUM Dashboard Changes

**Location**: AWS Console → Pinpoint → SMS and voice → Phone numbers → +12173933490

**Expected Changes**:
- ✅ **Message count increased** by 1
- ✅ **Last activity timestamp** updated to current time
- ✅ **Status**: Active
- ✅ **Analytics tab** shows new message

---

### 4. CloudWatch Log Lines

**Location**: AWS Console → CloudWatch → Logs → `/aws/lambda/allsenseai-complete` → Latest stream

**Expected Log Lines**:
```
START RequestId: abc-123-def-456
AllSenseAI received: {"action":"TEST_SMS","phoneNumber":"+19543483664",...}
Processing action: TEST_SMS
Direct EUM SMS test to +19543483664
=== AWS END USER MESSAGING SMS SEND ===
Destination: +19543483664
Originator: +12173933490
Campaign: AllSensesAI-SafetyAlerts
Message Type: TRANSACTIONAL
Message Length: 75 characters
Region: us-east-1
AWS End User Messaging client initialized successfully
Using Originator: +12173933490
Using Campaign: AllSensesAI-SafetyAlerts
=== EUM SMS SENT SUCCESSFULLY ===
MessageId: us-east-1-abc123def456...
Full Response: {"MessageId":"us-east-1-..."}
This message WILL appear in AWS End User Messaging Dashboard
END RequestId: abc-123-def-456
```

**Key phrases to look for**:
- ✅ "AWS End User Messaging client initialized successfully"
- ✅ "Using Originator: +12173933490"
- ✅ "Using Campaign: AllSensesAI-SafetyAlerts"
- ✅ "=== EUM SMS SENT SUCCESSFULLY ==="
- ✅ "This message WILL appear in AWS End User Messaging Dashboard"

---

## 🚨 What To Do If Things Are NOT Correct

### Problem 1: Verification Script Shows "Lambda connectivity failed"

**Symptom**:
```
❌ Lambda connectivity failed!
Error: The remote server returned an error: (500) Internal Server Error.
```

**What to check**:
1. **Lambda deployment status**
   - Go to: Lambda console → `allsenseai-complete`
   - Check: "Last modified" timestamp is recent
   - Action: If old, redeploy code

2. **Lambda code correctness**
   - Go to: Lambda console → Code tab
   - Check: First line is `import json`
   - Check: Line 15 has `ORIGINATOR_NUMBER = "+12173933490"`
   - Action: If wrong, paste code again from `lambda/allsenseai-eum-compliant.py`

3. **CloudWatch logs for errors**
   - Go to: CloudWatch → Logs → `/aws/lambda/allsenseai-complete`
   - Look for: Error messages in latest stream
   - Common errors:
     - "Failed to initialize EUM client" → IAM permissions missing
     - "NoCredentialsError" → IAM role not attached
     - "AccessDeniedException" → IAM policy not added

---

### Problem 2: CHECK 1 Fails (eumCompliant = false)

**Symptom**:
```
❌ CHECK 1: eumCompliant = false or missing
   This means Lambda is NOT using EUM API!
```

**What to check**:
1. **Lambda code deployment**
   - Go to: Lambda console → Code tab
   - Search for: `pinpoint-sms-voice-v2`
   - Action: If not found, code wasn't deployed correctly
   - Fix: Redeploy `lambda/allsenseai-eum-compliant.py`

2. **Wait and retry**
   - Action: Wait 30 seconds
   - Run: `.\verify-eum-deployment.ps1` again
   - Reason: Lambda may still be deploying

---

### Problem 3: CHECK 2 or CHECK 3 Fails (Wrong originator/campaign)

**Symptom**:
```
❌ CHECK 2: originatorNumber = +15551234567
   Expected: +12173933490
```

**What to check**:
1. **Lambda code constants**
   - Go to: Lambda console → Code tab
   - Find line 15: `ORIGINATOR_NUMBER = "+12173933490"`
   - Find line 16: `CONFIGURATION_SET = "AllSensesAI-SafetyAlerts"`
   - Action: If wrong, fix and click "Deploy"

---

### Problem 4: CHECK 6 Fails (SMS Error present)

**Symptom**:
```
❌ CHECK 6: SMS Error present!
   Error: AccessDeniedException: User is not authorized to perform: sms-voice:SendTextMessage
```

**What to check**:
1. **IAM policy attached**
   - Go to: Lambda → Configuration → Permissions
   - Click: Role name
   - Check: Policy named `EUM-SMS-Permissions` exists
   - Action: If missing, add inline policy from `iam/eum-iam-policy.json`

2. **IAM policy content**
   - Go to: IAM role → Policies → `EUM-SMS-Permissions`
   - Check: Contains `sms-voice:SendTextMessage`
   - Action: If wrong, delete and recreate from `iam/eum-iam-policy.json`

---

### Problem 5: No SMS Received on Phone

**Symptom**: Verification script passes, but no SMS on phone +19543483664

**What to check**:
1. **Phone number verification**
   - Go to: Pinpoint → SMS and voice → Phone numbers
   - Check: +19543483664 is in verified numbers list
   - Action: If not, verify phone number in AWS sandbox

2. **Carrier delays**
   - Action: Wait 1-2 minutes
   - Reason: Some carriers have delays

3. **Spam filtering**
   - Action: Check spam/junk folder on phone
   - Action: Check blocked numbers list

---

### Problem 6: EUM Dashboard Shows No Messages

**Symptom**: Script passes, SMS received, but EUM Dashboard shows no activity

**What to check**:
1. **Correct phone number**
   - Go to: Pinpoint → SMS and voice → Phone numbers
   - Check: Looking at +12173933490 (not a different number)

2. **Refresh dashboard**
   - Action: Click refresh button
   - Action: Wait 30 seconds and refresh again

3. **Analytics tab**
   - Go to: Phone number → Analytics tab
   - Check: Shows recent activity
   - Action: If empty, check CloudWatch logs for actual MessageId

---

### Problem 7: CloudWatch Logs Show SNS Instead of EUM

**Symptom**: Logs show "SNS publish" or "sns.publish()" instead of "EUM SMS SENT"

**What to check**:
1. **Wrong Lambda code deployed**
   - Go to: Lambda console → Code tab
   - Search for: `sns.publish`
   - Action: If found, wrong code is deployed
   - Fix: Deploy `lambda/allsenseai-eum-compliant.py`

2. **Old log stream**
   - Go to: CloudWatch → Logs
   - Check: Looking at latest log stream (most recent timestamp)
   - Action: Click latest stream

---

## 📋 Quick Troubleshooting Decision Tree

```
Verification script fails?
├─ HTTP 500 error?
│  ├─ Check Lambda deployment status
│  ├─ Check CloudWatch logs for errors
│  └─ Verify IAM permissions added
│
├─ CHECK 1 fails (eumCompliant)?
│  ├─ Verify Lambda code deployed correctly
│  └─ Wait 30 seconds and retry
│
├─ CHECK 2/3 fails (originator/campaign)?
│  └─ Check Lambda code constants (lines 15-16)
│
└─ CHECK 6 fails (SMS Error)?
   └─ Verify IAM policy added correctly

SMS not received?
├─ Check phone number verified in AWS
├─ Wait 1-2 minutes for carrier
└─ Check spam folder

EUM Dashboard empty?
├─ Verify correct phone number (+12173933490)
├─ Refresh dashboard
└─ Check Analytics tab
```

---

## ✅ Final Verification

**Everything is OK when ALL of these are true**:
- ✅ Verification script: "ALL CHECKS PASSED"
- ✅ Phone: SMS received from +12173933490
- ✅ EUM Dashboard: Message count increased
- ✅ CloudWatch: "EUM SMS SENT SUCCESSFULLY" in logs

**If any ONE is false, follow troubleshooting steps above.**
