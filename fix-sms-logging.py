# Enhanced SMS function with detailed error logging
def send_sms_with_eum(phone_number, message, event_id=None):
    """
    Send SMS with full EUM compliance and international support - ENHANCED LOGGING
    """
    try:
        # Validate phone number
        validated_phone, error = validate_phone_number(phone_number)
        if error:
            logger.error(f"❌ Phone validation failed: {error}")
            return {
                'status': 'failed',
                'error': error,
                'phone': phone_number,
                'realSms': False
            }
        
        # Check demo mode
        if DEMO_MODE:
            logger.info(f"DEMO_MODE enabled - simulating SMS to {validated_phone}")
            return {
                'status': 'sent',
                'messageId': f'demo-{uuid.uuid4().hex[:12]}',
                'phone': validated_phone,
                'realSms': False,
                'demoMode': True,
                'note': 'SMS simulated (DEMO_MODE=true)'
            }
        
        # Add tracking URL if event_id provided
        if event_id and TRACKING_URL_BASE:
            tracking_url = f"{TRACKING_URL_BASE}?incident={event_id}"
            message = f"{message}\n\nTrack: {tracking_url}"
        
        # Prepare EUM-compliant SMS attributes
        message_attributes = {
            'AWS.SNS.SMS.SMSType': {
                'DataType': 'String',
                'StringValue': 'Transactional'  # High priority for emergency
            }
        }
        
        # Add SenderID if configured (for supported countries)
        if SMS_ORIGINATOR:
            message_attributes['AWS.SNS.SMS.SenderID'] = {
                'DataType': 'String',
                'StringValue': SMS_ORIGINATOR
            }
        
        # Send SMS via SNS - WITH DETAILED LOGGING
        logger.info(f"📱 Attempting SNS publish to {validated_phone}")
        logger.info(f"   Originator: {SMS_ORIGINATOR}")
        logger.info(f"   Message length: {len(message)} chars")
        logger.info(f"   Attributes: {json.dumps(message_attributes)}")
        
        try:
            response = sns.publish(
                PhoneNumber=validated_phone,
                Message=message,
                MessageAttributes=message_attributes
            )
            
            message_id = response['MessageId']
            logger.info(f"✅ SNS PUBLISH SUCCESS!")
            logger.info(f"   MessageId: {message_id}")
            logger.info(f"   Phone: {validated_phone}")
            logger.info(f"   Response: {json.dumps(response, default=str)}")
            
            return {
                'status': 'sent',
                'messageId': message_id,
                'phone': validated_phone,
                'realSms': True,
                'smsMethod': 'SNS',
                'originator': SMS_ORIGINATOR,
                'campaignId': SMS_CAMPAIGN_ID,
                'timestamp': datetime.now(timezone.utc).isoformat()
            }
            
        except Exception as sns_error:
            # Log SNS-specific error
            logger.error(f"❌ SNS PUBLISH FAILED!")
            logger.error(f"   Error Type: {type(sns_error).__name__}")
            logger.error(f"   Error Message: {str(sns_error)}")
            logger.error(f"   Phone: {validated_phone}")
            logger.error(f"   Full exception:", exc_info=True)
            
            return {
                'status': 'failed',
                'error': f'SNS Error: {str(sns_error)}',
                'errorType': type(sns_error).__name__,
                'phone': validated_phone,
                'realSms': False,
                'note': 'SNS publish call failed - check IAM permissions and SNS configuration'
            }
        
    except Exception as e:
        error_msg = str(e)
        logger.error(f"❌ SMS function error for {phone_number}: {error_msg}", exc_info=True)
        
        return {
            'status': 'failed',
            'error': error_msg,
            'phone': phone_number,
            'realSms': False,
            'note': 'SMS delivery failed - check CloudWatch logs for details'
        }
