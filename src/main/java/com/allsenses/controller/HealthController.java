package com.allsenses.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for AllSenses AI Guardian
 * 
 * Provides system health information and AWS service connectivity status.
 * This demonstrates the AI agent's integration with AWS services.
 */
@RestController
@RequestMapping("/api/public")
public class HealthController {

    @Autowired
    private BedrockClient bedrockClient;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private LambdaClient lambdaClient;

    @Autowired
    private SnsClient snsClient;

    /**
     * Basic health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "AllSenses AI Guardian");
        health.put("version", "1.0.0-SNAPSHOT");
        health.put("description", "AWS AI Agent for threat detection and emergency response");
        
        return ResponseEntity.ok(health);
    }

    /**
     * AWS services connectivity check
     */
    @GetMapping("/aws-status")
    public ResponseEntity<Map<String, Object>> awsStatus() {
        Map<String, Object> status = new HashMap<>();
        Map<String, String> services = new HashMap<>();
        
        // Check AWS service clients (basic connectivity)
        services.put("bedrock", bedrockClient != null ? "CONFIGURED" : "NOT_CONFIGURED");
        services.put("dynamodb", dynamoDbClient != null ? "CONFIGURED" : "NOT_CONFIGURED");
        services.put("lambda", lambdaClient != null ? "CONFIGURED" : "NOT_CONFIGURED");
        services.put("sns", snsClient != null ? "CONFIGURED" : "NOT_CONFIGURED");
        
        status.put("aws_services", services);
        status.put("ai_agent_conditions", getAiAgentConditions());
        
        return ResponseEntity.ok(status);
    }

    @Autowired
    private com.allsenses.service.AwsIntegrationService awsIntegrationService;

    @Autowired
    private com.allsenses.service.DatabaseInitializationService databaseInitializationService;

    /**
     * Test AWS services connectivity
     */
    @GetMapping("/aws-test")
    public ResponseEntity<Map<String, Object>> testAwsServices() {
        Map<String, Object> testResults = awsIntegrationService.performHealthCheck();
        return ResponseEntity.ok(testResults);
    }

    /**
     * Test DynamoDB database connectivity
     */
    @GetMapping("/database-test")
    public ResponseEntity<Map<String, Object>> testDatabaseConnectivity() {
        Map<String, Object> testResults = databaseInitializationService.testDatabaseConnectivity();
        return ResponseEntity.ok(testResults);
    }

    /**
     * Get database status
     */
    @GetMapping("/database-status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> status = databaseInitializationService.getDatabaseStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * AI Agent qualification conditions status
     */
    private Map<String, Object> getAiAgentConditions() {
        Map<String, Object> conditions = new HashMap<>();
        
        // Condition 1: LLM Integration
        Map<String, String> condition1 = new HashMap<>();
        condition1.put("description", "Large Language Model hosted out of AWS Bedrock");
        condition1.put("status", bedrockClient != null ? "CONFIGURED" : "NOT_CONFIGURED");
        condition1.put("implementation", "AWS Bedrock Runtime Client");
        conditions.put("condition_1_llm", condition1);
        
        // Condition 2: AWS Services
        Map<String, String> condition2 = new HashMap<>();
        condition2.put("description", "Uses AWS services (Bedrock, DynamoDB, Lambda, SNS)");
        condition2.put("status", "CONFIGURED");
        condition2.put("services", "Bedrock, DynamoDB, Lambda, SNS");
        conditions.put("condition_2_aws_services", condition2);
        
        // Condition 3: AI Agent Qualification
        Map<String, String> condition3 = new HashMap<>();
        condition3.put("description", "Reasoning LLMs, autonomous capabilities, external integrations");
        condition3.put("status", "IN_DEVELOPMENT");
        condition3.put("features", "LLM reasoning, autonomous decisions, API/DB integration");
        conditions.put("condition_3_ai_agent", condition3);
        
        return conditions;
    }
}