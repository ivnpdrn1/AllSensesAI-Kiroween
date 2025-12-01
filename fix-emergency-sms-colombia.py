# The issue: Emergency SMS passes event_id, test SMS doesn't
# Even though we skip tracking URL for LATAM, something else might be different

# SOLUTION: Make emergency SMS call identical to test SMS call
# Remove event_id from emergency SMS dispatch

# In dispatch_emergency_sms_universal(), change:
# OLD:
#   sms_result = send_sms_with_eum(contact_phone, sms_text, event_id)
#
# NEW:
#   sms_result = send_sms_with_eum(contact_phone, sms_text)  # No event_id for Colombia

# This makes emergency SMS identical to test SMS which works
