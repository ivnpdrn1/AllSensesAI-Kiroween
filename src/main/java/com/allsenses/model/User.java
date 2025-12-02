package com.allsenses.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * DynamoDB entity representing a user with consent management and privacy preferences.
 * Stores user profile, consent status, and trusted contacts for emergency response.
 * 
 * This entity demonstrates privacy compliance and consent management
 * as required for the AllSenses AI Guardian system.
 */
@DynamoDbBean
public class User {

    private String userId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private Boolean consentGiven;
    private Instant consentTimestamp;
    private String consentVersion;
    private Boolean dataProcessingConsent;
    private Boolean emergencyResponseConsent;
    private Boolean locationTrackingConsent;
    private List<TrustedContact> trustedContacts;
    private Map<String, String> privacyPreferences;
    private String accountStatus;
    private Instant lastActiveTimestamp;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastActiveTimestamp = Instant.now();
        this.consentGiven = false;
        this.dataProcessingConsent = false;
        this.emergencyResponseConsent = false;
        this.locationTrackingConsent = false;
        this.accountStatus = "ACTIVE";
    }

    // Partition key
    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @DynamoDbAttribute("firstName")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @DynamoDbAttribute("lastName")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @DynamoDbAttribute("consentGiven")
    public Boolean getConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(Boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    @DynamoDbAttribute("consentTimestamp")
    public Instant getConsentTimestamp() {
        return consentTimestamp;
    }

    public void setConsentTimestamp(Instant consentTimestamp) {
        this.consentTimestamp = consentTimestamp;
    }

    @DynamoDbAttribute("consentVersion")
    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    @DynamoDbAttribute("dataProcessingConsent")
    public Boolean getDataProcessingConsent() {
        return dataProcessingConsent;
    }

    public void setDataProcessingConsent(Boolean dataProcessingConsent) {
        this.dataProcessingConsent = dataProcessingConsent;
    }

    @DynamoDbAttribute("emergencyResponseConsent")
    public Boolean getEmergencyResponseConsent() {
        return emergencyResponseConsent;
    }

    public void setEmergencyResponseConsent(Boolean emergencyResponseConsent) {
        this.emergencyResponseConsent = emergencyResponseConsent;
    }

    @DynamoDbAttribute("locationTrackingConsent")
    public Boolean getLocationTrackingConsent() {
        return locationTrackingConsent;
    }

    public void setLocationTrackingConsent(Boolean locationTrackingConsent) {
        this.locationTrackingConsent = locationTrackingConsent;
    }

    @DynamoDbAttribute("trustedContacts")
    public List<TrustedContact> getTrustedContacts() {
        return trustedContacts;
    }

    public void setTrustedContacts(List<TrustedContact> trustedContacts) {
        this.trustedContacts = trustedContacts;
    }

    @DynamoDbAttribute("privacyPreferences")
    public Map<String, String> getPrivacyPreferences() {
        return privacyPreferences;
    }

    public void setPrivacyPreferences(Map<String, String> privacyPreferences) {
        this.privacyPreferences = privacyPreferences;
    }

    @DynamoDbAttribute("accountStatus")
    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    @DynamoDbAttribute("lastActiveTimestamp")
    public Instant getLastActiveTimestamp() {
        return lastActiveTimestamp;
    }

    public void setLastActiveTimestamp(Instant lastActiveTimestamp) {
        this.lastActiveTimestamp = lastActiveTimestamp;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods for consent management
    public boolean hasValidConsent() {
        return Boolean.TRUE.equals(consentGiven) && 
               Boolean.TRUE.equals(dataProcessingConsent) &&
               Boolean.TRUE.equals(emergencyResponseConsent) &&
               consentTimestamp != null;
    }

    public boolean canProcessEmergencyData() {
        return hasValidConsent() && Boolean.TRUE.equals(emergencyResponseConsent);
    }

    public boolean canTrackLocation() {
        return hasValidConsent() && Boolean.TRUE.equals(locationTrackingConsent);
    }

    public void giveConsent(String version) {
        this.consentGiven = true;
        this.consentTimestamp = Instant.now();
        this.consentVersion = version;
        this.dataProcessingConsent = true;
        this.emergencyResponseConsent = true;
        this.locationTrackingConsent = true;
        this.updatedAt = Instant.now();
    }

    public void withdrawConsent() {
        this.consentGiven = false;
        this.dataProcessingConsent = false;
        this.emergencyResponseConsent = false;
        this.locationTrackingConsent = false;
        this.accountStatus = "CONSENT_WITHDRAWN";
        this.updatedAt = Instant.now();
    }

    public void updateActivity() {
        this.lastActiveTimestamp = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return "Unknown User";
        }
    }

    public LocalDateTime getConsentTimestampAsLocalDateTime() {
        return consentTimestamp != null ? LocalDateTime.ofInstant(consentTimestamp, ZoneOffset.UTC) : null;
    }

    public void setConsentTimestampFromLocalDateTime(LocalDateTime localDateTime) {
        this.consentTimestamp = localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : null;
    }

    // Update timestamp on modification
    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", consentGiven=" + consentGiven +
                ", consentTimestamp=" + consentTimestamp +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }

    /**
     * Trusted contact for emergency notifications
     */
    @DynamoDbBean
    public static class TrustedContact {
        private String contactId;
        private String name;
        private String phoneNumber;
        private String email;
        private String relationship;
        private String preferredContactMethod;
        private Boolean isPrimary;
        private Instant addedTimestamp;

        public TrustedContact() {
            this.addedTimestamp = Instant.now();
            this.isPrimary = false;
        }

        @DynamoDbAttribute("contactId")
        public String getContactId() {
            return contactId;
        }

        public void setContactId(String contactId) {
            this.contactId = contactId;
        }

        @DynamoDbAttribute("name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @DynamoDbAttribute("phoneNumber")
        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @DynamoDbAttribute("email")
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @DynamoDbAttribute("relationship")
        public String getRelationship() {
            return relationship;
        }

        public void setRelationship(String relationship) {
            this.relationship = relationship;
        }

        @DynamoDbAttribute("preferredContactMethod")
        public String getPreferredContactMethod() {
            return preferredContactMethod;
        }

        public void setPreferredContactMethod(String preferredContactMethod) {
            this.preferredContactMethod = preferredContactMethod;
        }

        @DynamoDbAttribute("isPrimary")
        public Boolean getIsPrimary() {
            return isPrimary;
        }

        public void setIsPrimary(Boolean isPrimary) {
            this.isPrimary = isPrimary;
        }

        @DynamoDbAttribute("addedTimestamp")
        public Instant getAddedTimestamp() {
            return addedTimestamp;
        }

        public void setAddedTimestamp(Instant addedTimestamp) {
            this.addedTimestamp = addedTimestamp;
        }

        public boolean canReceiveSms() {
            return phoneNumber != null && !phoneNumber.isEmpty() &&
                   ("SMS".equals(preferredContactMethod) || "BOTH".equals(preferredContactMethod));
        }

        public boolean canReceiveEmail() {
            return email != null && !email.isEmpty() &&
                   ("EMAIL".equals(preferredContactMethod) || "BOTH".equals(preferredContactMethod));
        }

        @Override
        public String toString() {
            return "TrustedContact{" +
                    "contactId='" + contactId + '\'' +
                    ", name='" + name + '\'' +
                    ", relationship='" + relationship + '\'' +
                    ", preferredContactMethod='" + preferredContactMethod + '\'' +
                    ", isPrimary=" + isPrimary +
                    '}';
        }
    }
}