package com.allsenses.repository;

import com.allsenses.model.ThreatAssessment;
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
 * DynamoDB repository for ThreatAssessment entities.
 * 
 * This repository demonstrates Condition 3 (AI Agent Qualification) by providing
 * database integration for storing and retrieving LLM-powered threat assessments.
 */
@Repository
public class ThreatAssessmentRepository {

    private final DynamoDbTable<ThreatAssessment> table;

    @Autowired
    public ThreatAssessmentRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("allsenses-threat-assessments", 
                                         TableSchema.fromBean(ThreatAssessment.class));
    }

    /**
     * Save a threat assessment to DynamoDB
     */
    public ThreatAssessment save(ThreatAssessment assessment) {
        try {
            assessment.markAsUpdated();
            table.putItem(assessment);
            return assessment;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to save threat assessment: " + e.getMessage(), e);
        }
    }

    /**
     * Find threat assessment by ID
     */
    public Optional<ThreatAssessment> findById(String assessmentId) {
        try {
            Key key = Key.builder()
                    .partitionValue(assessmentId)
                    .build();
            
            ThreatAssessment assessment = table.getItem(key);
            return Optional.ofNullable(assessment);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find threat assessment: " + e.getMessage(), e);
        }
    }

    /**
     * Find all threat assessments for a user
     */
    public List<ThreatAssessment> findByUserId(String userId) {
        try {
            // Use scan with filter for this MVP (in production, consider GSI)
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

            List<ThreatAssessment> assessments = new ArrayList<>();
            table.scan(scanRequest).items().forEach(assessments::add);
            
            return assessments;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find assessments for user: " + e.getMessage(), e);
        }
    }

    /**
     * Find recent threat assessments (last 24 hours)
     */
    public List<ThreatAssessment> findRecentAssessments() {
        try {
            Instant yesterday = Instant.now().minus(24, ChronoUnit.HOURS);
            
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("#timestamp > :yesterday")
                            .putExpressionName("#timestamp", "timestamp")
                            .putExpressionValue(":yesterday", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s(yesterday.toString())
                                    .build())
                            .build())
                    .build();

            List<ThreatAssessment> assessments = new ArrayList<>();
            table.scan(scanRequest).items().forEach(assessments::add);
            
            return assessments;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find recent assessments: " + e.getMessage(), e);
        }
    }

    /**
     * Find high-threat assessments
     */
    public List<ThreatAssessment> findHighThreatAssessments() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(
                        software.amazon.awssdk.enhanced.dynamodb.Expression.builder()
                            .expression("threatLevel IN (:high, :critical)")
                            .putExpressionValue(":high", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("HIGH")
                                    .build())
                            .putExpressionValue(":critical", 
                                software.amazon.awssdk.enhanced.dynamodb.AttributeValue.builder()
                                    .s("CRITICAL")
                                    .build())
                            .build())
                    .build();

            List<ThreatAssessment> assessments = new ArrayList<>();
            table.scan(scanRequest).items().forEach(assessments::add);
            
            return assessments;
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to find high threat assessments: " + e.getMessage(), e);
        }
    }

    /**
     * Update threat assessment status
     */
    public ThreatAssessment updateStatus(String assessmentId, String newStatus) {
        try {
            Optional<ThreatAssessment> existing = findById(assessmentId);
            if (existing.isPresent()) {
                ThreatAssessment assessment = existing.get();
                assessment.setStatus(newStatus);
                assessment.markAsUpdated();
                return save(assessment);
            } else {
                throw new RuntimeException("Threat assessment not found: " + assessmentId);
            }
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to update assessment status: " + e.getMessage(), e);
        }
    }

    /**
     * Delete threat assessment
     */
    public void delete(String assessmentId) {
        try {
            Key key = Key.builder()
                    .partitionValue(assessmentId)
                    .build();
            
            table.deleteItem(key);
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to delete threat assessment: " + e.getMessage(), e);
        }
    }

    /**
     * Count total assessments
     */
    public long countAll() {
        try {
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .build();
            
            return table.scan(scanRequest).items().stream().count();
        } catch (DynamoDbException e) {
            throw new RuntimeException("Failed to count assessments: " + e.getMessage(), e);
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