package com.allsenses.controller;

import com.allsenses.model.User;
import com.allsenses.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * User Management API Controller for AllSenses AI Guardian
 * 
 * This controller provides user management endpoints with DynamoDB integration,
 * demonstrating database integration capabilities for AI agent qualification.
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserManagementApiController {

    @Autowired
    private UserRepository userRepository;

    /**
     * Create new user with consent
     */
    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @RequestBody UserRegistrationRequest request) {
        
        try {
            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(UserRegistrationResponse.builder()
                                .success(false)
                                .errorMessage("User with email already exists")
                                .build());
            }
            
            // Create new user
            User user = new User();
            user.setUserId(generateUserId());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            
            // Set consent if provided
            if (request.isConsentGiven()) {
                user.giveConsent(request.getConsentVersion());
            }
            
            // Save user
            User savedUser = userRepository.save(user);
            
            UserRegistrationResponse response = UserRegistrationResponse.builder()
                    .userId(savedUser.getUserId())
                    .user(savedUser)
                    .success(true)
                    .consentStatus(savedUser.hasValidConsent() ? "VALID" : "PENDING")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(UserRegistrationResponse.builder()
                            .success(false)
                            .errorMessage("User registration failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user consent
     */
    @PutMapping("/{userId}/consent")
    public ResponseEntity<ConsentUpdateResponse> updateUserConsent(
            @PathVariable String userId,
            @RequestBody ConsentUpdateRequest request) {
        
        try {
            User updatedUser = userRepository.updateConsent(
                userId, request.isConsentGiven(), request.getConsentVersion());
            
            ConsentUpdateResponse response = ConsentUpdateResponse.builder()
                    .userId(userId)
                    .consentStatus(updatedUser.hasValidConsent() ? "VALID" : "WITHDRAWN")
                    .consentTimestamp(updatedUser.getConsentTimestamp())
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ConsentUpdateResponse.builder()
                            .userId(userId)
                            .success(false)
                            .errorMessage("Consent update failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Add trusted contact
     */
    @PostMapping("/{userId}/trusted-contacts")
    public ResponseEntity<TrustedContactResponse> addTrustedContact(
            @PathVariable String userId,
            @RequestBody TrustedContactRequest request) {
        
        try {
            User.TrustedContact contact = new User.TrustedContact();
            contact.setContactId(generateContactId());
            contact.setName(request.getName());
            contact.setPhoneNumber(request.getPhoneNumber());
            contact.setEmail(request.getEmail());
            contact.setRelationship(request.getRelationship());
            contact.setPreferredContactMethod(request.getPreferredContactMethod());
            contact.setIsPrimary(request.isPrimary());
            
            User updatedUser = userRepository.addTrustedContact(userId, contact);
            
            TrustedContactResponse response = TrustedContactResponse.builder()
                    .contactId(contact.getContactId())
                    .userId(userId)
                    .contact(contact)
                    .totalContacts(updatedUser.getTrustedContacts() != null ? 
                        updatedUser.getTrustedContacts().size() : 0)
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(TrustedContactResponse.builder()
                            .userId(userId)
                            .success(false)
                            .errorMessage("Failed to add trusted contact: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get users with valid consent
     */
    @GetMapping("/with-consent")
    public ResponseEntity<List<User>> getUsersWithValidConsent() {
        List<User> users = userRepository.findUsersWithValidConsent();
        return ResponseEntity.ok(users);
    }

    /**
     * Get active users
     */
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        List<User> users = userRepository.findActiveUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update user activity
     */
    @PutMapping("/{userId}/activity")
    public ResponseEntity<User> updateUserActivity(@PathVariable String userId) {
        try {
            User updatedUser = userRepository.updateActivity(userId);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get user management statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserManagementStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalUsers = userRepository.countAll();
            List<User> usersWithConsent = userRepository.findUsersWithValidConsent();
            List<User> activeUsers = userRepository.findActiveUsers();
            
            stats.put("total_users", totalUsers);
            stats.put("users_with_consent", usersWithConsent.size());
            stats.put("active_users", activeUsers.size());
            stats.put("consent_rate", totalUsers > 0 ? (double) usersWithConsent.size() / totalUsers : 0.0);
            stats.put("database_integration", "DynamoDB");
            stats.put("status", "SUCCESS");
            
        } catch (Exception e) {
            stats.put("status", "ERROR");
            stats.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(stats);
    }

    // Utility methods
    private String generateUserId() {
        return "USER-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private String generateContactId() {
        return "CONTACT-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }    
// Request/Response DTOs
    public static class UserRegistrationRequest {
        private String email;
        private String phoneNumber;
        private String firstName;
        private String lastName;
        private boolean consentGiven = false;
        private String consentVersion = "1.0";

        // Getters and Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public boolean isConsentGiven() { return consentGiven; }
        public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }
        public String getConsentVersion() { return consentVersion; }
        public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    }

    public static class UserRegistrationResponse {
        private String userId;
        private User user;
        private boolean success;
        private String consentStatus;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private UserRegistrationResponse response = new UserRegistrationResponse();
            public Builder userId(String userId) { response.userId = userId; return this; }
            public Builder user(User user) { response.user = user; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder consentStatus(String consentStatus) { response.consentStatus = consentStatus; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public UserRegistrationResponse build() { return response; }
        }

        // Getters
        public String getUserId() { return userId; }
        public User getUser() { return user; }
        public boolean isSuccess() { return success; }
        public String getConsentStatus() { return consentStatus; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ConsentUpdateRequest {
        private boolean consentGiven;
        private String consentVersion = "1.0";

        // Getters and Setters
        public boolean isConsentGiven() { return consentGiven; }
        public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }
        public String getConsentVersion() { return consentVersion; }
        public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    }

    public static class ConsentUpdateResponse {
        private String userId;
        private String consentStatus;
        private Instant consentTimestamp;
        private boolean success;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ConsentUpdateResponse response = new ConsentUpdateResponse();
            public Builder userId(String userId) { response.userId = userId; return this; }
            public Builder consentStatus(String consentStatus) { response.consentStatus = consentStatus; return this; }
            public Builder consentTimestamp(Instant consentTimestamp) { response.consentTimestamp = consentTimestamp; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public ConsentUpdateResponse build() { return response; }
        }

        // Getters
        public String getUserId() { return userId; }
        public String getConsentStatus() { return consentStatus; }
        public Instant getConsentTimestamp() { return consentTimestamp; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class TrustedContactRequest {
        private String name;
        private String phoneNumber;
        private String email;
        private String relationship;
        private String preferredContactMethod = "SMS";
        private boolean isPrimary = false;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }
        public String getPreferredContactMethod() { return preferredContactMethod; }
        public void setPreferredContactMethod(String preferredContactMethod) { this.preferredContactMethod = preferredContactMethod; }
        public boolean isPrimary() { return isPrimary; }
        public void setPrimary(boolean primary) { isPrimary = primary; }
    }

    public static class TrustedContactResponse {
        private String contactId;
        private String userId;
        private User.TrustedContact contact;
        private int totalContacts;
        private boolean success;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private TrustedContactResponse response = new TrustedContactResponse();
            public Builder contactId(String contactId) { response.contactId = contactId; return this; }
            public Builder userId(String userId) { response.userId = userId; return this; }
            public Builder contact(User.TrustedContact contact) { response.contact = contact; return this; }
            public Builder totalContacts(int totalContacts) { response.totalContacts = totalContacts; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public TrustedContactResponse build() { return response; }
        }

        // Getters
        public String getContactId() { return contactId; }
        public String getUserId() { return userId; }
        public User.TrustedContact getContact() { return contact; }
        public int getTotalContacts() { return totalContacts; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}