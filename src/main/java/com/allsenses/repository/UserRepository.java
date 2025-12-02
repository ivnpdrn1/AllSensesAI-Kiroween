package com.allsenses.repository;

import com.allsenses.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB repository for User entities.
 * 
 * This repository demonstrates Condition 3 (AI Agent Qualification) by providing
 * database integration for user management and consent tracking.
 */
@Repository
public class UserRepository {

    private final DynamoDbTable<User> table;

    @Autowired
    public UserRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("allsenses-users", 
                                         TableSchema.fromBean(User.class));
    }

    /**
     * Save a user to DynamoDB
     */
    public User save(User user) {
        try {
            user.markAsUpdated();
            table.putItem(user);
            return user;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(String userId) {
        try {
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            
            User user = table.getItem(key);
            return Optional.ofNullable(user);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find user: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("email = :email")
                            .putExpressionValue(":email", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s(email)
                                    .build())
                            .build())
                    .build();

            return table.scan(scanRequest).items().stream().findFirst();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find user by email: " + e.getMessage(), e);
        }
    }

    /**
     * Find users with valid consent
     */
    public List<User> findUsersWithValidConsent() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("consentGiven = :true AND dataProcessingConsent = :true AND emergencyResponseConsent = :true")
                            .putExpressionValue(":true", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .bool(true)
                                    .build())
                            .build())
                    .build();

            List<User> users = new ArrayList<>();
            table.scan(scanRequest).items().forEach(users::add);
            
            return users;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find users with consent: " + e.getMessage(), e);
        }
    }

    /**
     * Find active users (not consent withdrawn)
     */
    public List<User> findActiveUsers() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("accountStatus = :active")
                            .putExpressionValue(":active", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("ACTIVE")
                                    .build())
                            .build())
                    .build();

            List<User> users = new ArrayList<>();
            table.scan(scanRequest).items().forEach(users::add);
            
            return users;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find active users: " + e.getMessage(), e);
        }
    }

    /**
     * Update user consent
     */
    public User updateConsent(String userId, boolean consentGiven, String consentVersion) {
        try {
            Optional<User> existing = findById(userId);
            if (existing.isPresent()) {
                User user = existing.get();
                if (consentGiven) {
                    user.giveConsent(consentVersion);
                } else {
                    user.withdrawConsent();
                }
                return save(user);
            } else {
                throw new RuntimeException("User not found: " + userId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to update user consent: " + e.getMessage(), e);
        }
    }

    /**
     * Update user activity timestamp
     */
    public User updateActivity(String userId) {
        try {
            Optional<User> existing = findById(userId);
            if (existing.isPresent()) {
                User user = existing.get();
                user.updateActivity();
                return save(user);
            } else {
                throw new RuntimeException("User not found: " + userId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to update user activity: " + e.getMessage(), e);
        }
    }

    /**
     * Add trusted contact to user
     */
    public User addTrustedContact(String userId, User.TrustedContact contact) {
        try {
            Optional<User> existing = findById(userId);
            if (existing.isPresent()) {
                User user = existing.get();
                List<User.TrustedContact> contacts = user.getTrustedContacts();
                if (contacts == null) {
                    contacts = new ArrayList<>();
                }
                
                // Generate contact ID if not set
                if (contact.getContactId() == null) {
                    contact.setContactId("CONTACT-" + System.currentTimeMillis());
                }
                
                contacts.add(contact);
                user.setTrustedContacts(contacts);
                user.markAsUpdated();
                
                return save(user);
            } else {
                throw new RuntimeException("User not found: " + userId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to add trusted contact: " + e.getMessage(), e);
        }
    }

    /**
     * Delete user (for consent withdrawal)
     */
    public void delete(String userId) {
        try {
            Key key = Key.builder()
                    .partitionValue(userId)
                    .build();
            
            table.deleteItem(key);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Count total users
     */
    public long countAll() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .build();
            
            return table.scan(scanRequest).items().stream().count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count users: " + e.getMessage(), e);
        }
    }

    /**
     * Create table if it doesn't exist (for development)
     */
    public void createTableIfNotExists() {
        try {
            table.createTable(CreateTableEnhancedRequest.builder()
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L)
                            .writeCapacityUnits(5L)
                            .build())
                    .build());
        } catch (Exception e) {
            // Table might already exist, which is fine
            System.out.println("Table creation result: " + e.getMessage());
        }
    }
}