# AllSensesAI-Kiroween  
### AllSensesAI Guardian – KIROWEEN Hackathon Submission  

---

## 🛡️ Overview  
AllSensesAI Guardian is an AI-driven personal safety system designed to detect emergency situations using multimodal signals—voice, audio distress cues, and geolocation—then trigger automated notifications to trusted contacts through SMS, voice calls, and real-time location tracking.

This version is specifically adapted and improved for the **KIROWEEN Hackathon**, including:  
- Enhanced emergency workflow  
- Refined SMS international support for Latin America  
- A stable and jury-ready CloudFront demo link  
- Cleaner Lambda infrastructure  
- Resilient alerting and fallback channels  
- Latin America readiness (Colombia, Chile, México, Venezuela)

---

## 🧩 Key Features
- **AI distress detection** (keyword + audio cues)  
- **Real-time location streaming** via API Gateway + Lambda  
- **Emergency SMS dispatch** using AWS End User Messaging (10DLC + international SMS)  
- **AI-powered call escalation** using Amazon Chime SDK  
- **Encrypted evidence storage** (audio + logs)  
- **Multi-country SMS routing**  
- **CloudFront web app for demo / testing**  

---

## 🏗️ Architecture

### Core AWS Services Used:
- **Amazon Lambda** – Emergency logic, SMS, geolocation  
- **Amazon Bedrock (Claude)** – Natural language analysis  
- **Amazon SNS / End User Messaging (EUM)** – SMS delivery  
- **Amazon Chime SDK** – Automated voice call fallback  
- **Amazon DynamoDB** – Event logging + geolocation history  
- **Amazon S3** – Encrypted evidence storage

## 🌐 Demo URL (Jury Link)
https://d4om8j6cvwtqd.cloudfront.net/audio


This link displays the jury-facing interface used during evaluation.

---

## 🚨 How to Trigger an Emergency
1. Open the demo interface.  
2. Speak the emergency keyword:  
   **"Check your phone"** or your configured distress phrase.  
3. The system will:  
   - Detect the emergency  
   - Capture the location  
   - Send SMS to the designated international number  
   - Trigger the voice call fallback if SMS fails  
   - Log the event in DynamoDB  

---

## 📡 SMS / International Support  
This system has dedicated configuration for SMS delivery in:  
- **Colombia (+57)**  
- **Chile (+56)**  
- **México (+52)**  
- **Venezuela (+58)**  

Using:  
- **Originator Number:** +1 217-393-3490  
- **Campaign:** AllSensesAI-SafetyAlerts  
- **Type:** TRANSACTIONAL  

---

## 📁 Repository Structure
/lambda-update # Emergency Lambda code
/infrastructure # CloudFormation / SAM
/iam # IAM roles & policies
/scripts # Deployment automation
/JURY_DEPLOYMENT_PACKAGE # Static assets used for the demo
/src # Source modules


---

## ⚙️ Deployment Guide

### 1. Deploy IAM Roles

aws iam create-role ...
aws iam attach-role-policy ...


### 2. Deploy Lambda
aws lambda update-function-code
--function-name AllSensesAI-Live
--zip-file fileb://lambda-deployment.zip


### 3. Deploy API Gateway + CloudFront  
CloudFormation / SAM template included in `/infrastructure`.

### 4. Update Emergency Contact Number  
Lambda ENV variable:

EMERGENCY_CONTACT_PHONE=+573222063010


---

## 🧪 Testing Instructions
1. Open the CloudFront demo link.  
2. Click **Start Emergency Simulation**.  
3. The system will:  
   - Process the event  
   - Send SMS  
   - Log the event  
4. Verify delivery:  
   - SNS/EUM message count increases  
   - Emergency contact receives SMS  
   - Lambda logs show successful flow  

---

## 📜 License  
This project is released under the **MIT License**.

---

## 👤 Author  
**Iván Padrón**  
Creator of AllSensesAI & Nubelai Inc.  
AWS Cloud Engineer & AI Developer  

---

## 🧛 KIROWEEN Edition Notes  
This version includes:  
- Updated demo endpoints  
- Clean emergency pipeline  
- Improved logging  
- New international SMS support  
- Jury-stable CloudFront deployment  
- Revised AI distress detection flow  

