package com.allsenses.repository;

import com.allsenses.model.EmergencyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DynamoDB repository for EmergencyEvent entities.
 * 
 * This repository demonstrates Condition 3 (AI Agent Qualification) by providing
 * database integration for storing and retrieving autonomous emergency response events.
 */
@Repository
public class EmergencyEventRepository {

    private final DynamoDbTable<EmergencyEvent> table;

    @Autowired
    public EmergencyEventRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("allsenses-emergency-events", 
                                         TableSchema.fromBean(EmergencyEvent.class));
    }

    /**
     * Save an emergency event to DynamoDB
     */
    public EmergencyEvent save(EmergencyEvent event) {
        try {
            event.markAsUpdated();
            table.putItem(event);
            return event;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save emergency event: " + e.getMessage(), e);
        }
    }

    /**
     * Find emergency event by ID
     */
    public Optional<EmergencyEvent> findById(String eventId) {
        try {
            Key key = Key.builder()
                    .partitionValue(eventId)
                    .build();
            
            EmergencyEvent event = table.getItem(key);
            return Optional.ofNullable(event);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find emergency event: " + e.getMessage(), e);
        }
    }

    /**
     * Find all emergency events for a user
     */
    public List<EmergencyEvent> findByUserId(String userId) {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("userId = :userId")
                            .putExpressionValue(":userId", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s(userId)
                                    .build())
                            .build())
                    .build();

            List<EmergencyEvent> events = new ArrayList<>();
            table.scan(scanRequest).items().forEach(events::add);
            
            return events;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find events for user: " + e.getMessage(), e);
        }
    }

    /**
     * Find active emergency events
     */
    public List<EmergencyEvent> findActiveEvents() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("eventStatus IN (:initiated, :inProgress, :contacted)")
                            .putExpressionValue(":initiated", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("INITIATED")
                                    .build())
                            .putExpressionValue(":inProgress", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("IN_PROGRESS")
                                    .build())
                            .putExpressionValue(":contacted", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("SERVICES_CONTACTED")
                                    .build())
                            .build())
                    .build();

            List<EmergencyEvent> events = new ArrayList<>();
            table.scan(scanRequest).items().forEach(events::add);
            
            return events;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find active events: " + e.getMessage(), e);
        }
    }

    /**
     * Find recent emergency events (last 7 days)
     */
    public List<EmergencyEvent> findRecentEvents() {
        try {
            Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("eventTimestamp > :weekAgo")
                            .putExpressionValue(":weekAgo", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s(weekAgo.toString())
                                    .build())
                            .build())
                    .build();

            List<EmergencyEvent> events = new ArrayList<>();
            table.scan(scanRequest).items().forEach(events::add);
            
            return events;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find recent events: " + e.getMessage(), e);
        }
    }

    /**
     * Find critical priority events
     */
    public List<EmergencyEvent> findCriticalEvents() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("priorityLevel = :critical")
                            .putExpressionValue(":critical", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("CRITICAL")
                                    .build())
                            .build())
                    .build();

            List<EmergencyEvent> events = new ArrayList<>();
            table.scan(scanRequest).items().forEach(events::add);
            
            return events;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find critical events: " + e.getMessage(), e);
        }
    }

    /**
     * Update emergency event status
     */
    public EmergencyEvent updateStatus(String eventId, String newStatus) {
        try {
            Optional<EmergencyEvent> existing = findById(eventId);
            if (existing.isPresent()) {
                EmergencyEvent event = existing.get();
                event.setEventStatus(newStatus);
                event.markAsUpdated();
                return save(event);
            } else {
                throw new RuntimeException("Emergency event not found: " + eventId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to update event status: " + e.getMessage(), e);
        }
    }

    /**
     * Mark event as resolved
     */
    public EmergencyEvent resolveEvent(String eventId, String resolutionNotes) {
        try {
            Optional<EmergencyEvent> existing = findById(eventId);
            if (existing.isPresent()) {
                EmergencyEvent event = existing.get();
                event.markAsResolved(resolutionNotes);
                return save(event);
            } else {
                throw new RuntimeException("Emergency event not found: " + eventId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to resolve event: " + e.getMessage(), e);
        }
    }

    /**
     * Mark event as false alarm
     */
    public EmergencyEvent markAsFalseAlarm(String eventId, String notes) {
        try {
            Optional<EmergencyEvent> existing = findById(eventId);
            if (existing.isPresent()) {
                EmergencyEvent event = existing.get();
                event.markAsFalseAlarm(notes);
                return save(event);
            } else {
                throw new RuntimeException("Emergency event not found: " + eventId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to mark as false alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Delete emergency event
     */
    public void delete(String eventId) {
        try {
            Key key = Key.builder()
                    .partitionValue(eventId)
                    .build();
            
            table.deleteItem(key);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete emergency event: " + e.getMessage(), e);
        }
    }

    /**
     * Count total events
     */
    public long countAll() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .build();
            
            return table.scan(scanRequest).items().stream().count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count events: " + e.getMessage(), e);
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