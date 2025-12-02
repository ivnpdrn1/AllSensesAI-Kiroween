package com.allsenses.service;

import com.allsenses.repository.EmergencyEventRepository;
import com.allsenses.repository.ThreatAssessmentRepository;
import com.allsenses.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Database Initialization Service for AllSenses AI Guardian
 * 
 * This service initializes DynamoDB tables and demonstrates
 * Condition 3 (AI Agent Qualification) by setting up database integration
 * for the AI agent's data persistence needs.
 */
@Service
public class DatabaseInitializationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationService.class);

    @Autowired
    private ThreatAssessmentRepository threatAssessmentRepository;

    @Autowired
    private EmergencyEventRepository emergencyEventRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Initialize DynamoDB tables on application startup
     */
    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing DynamoDB tables for AI agent data persistence...");
        
        try {
            // Create tables if they don't exist
            threatAssessmentRepository.createTableIfNotExists();
            emergencyEventRepository.createTableIfNotExists();
            userRepository.createTableIfNotExists();
            
            logger.info("DynamoDB table initialization completed successfully");
            
            // Log table status
            logTableStatus();
            
        } catch (Exception e) {
            logger.error("Failed to initialize DynamoDB tables", e);
            // Don't fail the application startup for table creation issues
        }
    }

    /**
     * Log the status of all tables
     */
    private void logTableStatus() {
        try {
            long threatAssessmentCount = threatAssessmentRepository.countAll();
            long emergencyEventCount = emergencyEventRepository.countAll();
            long userCount = userRepository.countAll();
            
            logger.info("Database Status:");
            logger.info("- Threat Assessments: {} records", threatAssessmentCount);
            logger.info("- Emergency Events: {} records", emergencyEventCount);
            logger.info("- Users: {} records", userCount);
            
        } catch (Exception e) {
            logger.warn("Could not retrieve table counts: {}", e.getMessage());
        }
    }

    /**
     * Get database status for health checks
     */
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("threat_assessments_count", threatAssessmentRepository.countAll());
            status.put("emergency_events_count", emergencyEventRepository.countAll());
            status.put("users_count", userRepository.countAll());
            status.put("database_status", "CONNECTED");
            
        } catch (Exception e) {
            status.put("database_status", "ERROR");
            status.put("error_message", e.getMessage());
        }
        
        return status;
    }

    /**
     * Test database connectivity
     */
    public Map<String, Object> testDatabaseConnectivity() {
        Map<String, Object> testResults = new HashMap<>();
        
        // Test ThreatAssessment table
        try {
            long count = threatAssessmentRepository.countAll();
            testResults.put("threat_assessments_table", Map.of(
                "status", "SUCCESS",
                "record_count", count
            ));
        } catch (Exception e) {
            testResults.put("threat_assessments_table", Map.of(
                "status", "FAILED",
                "error", e.getMessage()
            ));
        }
        
        // Test EmergencyEvent table
        try {
            long count = emergencyEventRepository.countAll();
            testResults.put("emergency_events_table", Map.of(
                "status", "SUCCESS",
                "record_count", count
            ));
        } catch (Exception e) {
            testResults.put("emergency_events_table", Map.of(
                "status", "FAILED",
                "error", e.getMessage()
            ));
        }
        
        // Test User table
        try {
            long count = userRepository.countAll();
            testResults.put("users_table", Map.of(
                "status", "SUCCESS",
                "record_count", count
            ));
        } catch (Exception e) {
            testResults.put("users_table", Map.of(
                "status", "FAILED",
                "error", e.getMessage()
            ));
        }
        
        // Overall status
        boolean allSuccess = testResults.values().stream()
                .allMatch(result -> {
                    if (result instanceof Map) {
                        return "SUCCESS".equals(((Map<?, ?>) result).get("status"));
                    }
                    return false;
                });
        
        testResults.put("overall_status", allSuccess ? "SUCCESS" : "PARTIAL_FAILURE");
        
        return testResults;
    }
}