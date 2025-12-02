package com.allsenses.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsRequest;
import software.amazon.awssdk.services.bedrock.model.ListFoundationModelsResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Integration Service for AllSenses AI Guardian
 * 
 * Provides methods to test and verify AWS service connectivity.
 * This service demonstrates the AI agent's integration with AWS services.
 */
@Service
public class AwsIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(AwsIntegrationService.class);

    @Autowired
    private BedrockClient bedrockClient;

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Value("${aws.region}")
    private String awsRegion;

    /**
     * Test AWS Bedrock connectivity and list available foundation models
     */
    public Map<String, Object> testBedrockConnectivity() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Testing AWS Bedrock connectivity...");
            
            ListFoundationModelsRequest request = ListFoundationModelsRequest.builder().build();
            ListFoundationModelsResponse response = bedrockClient.listFoundationModels(request);
            
            result.put("status", "SUCCESS");
            result.put("region", awsRegion);
            result.put("models_count", response.modelSummaries().size());
            result.put("available_models", response.modelSummaries().stream()
                    .map(model -> model.modelId())
                    .limit(5) // Show first 5 models
                    .toList());
            
            logger.info("Bedrock connectivity test successful. Found {} models", 
                       response.modelSummaries().size());
            
        } catch (Exception e) {
            logger.error("Bedrock connectivity test failed", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Test AWS DynamoDB connectivity and list tables
     */
    public Map<String, Object> testDynamoDbConnectivity() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("Testing AWS DynamoDB connectivity...");
            
            ListTablesRequest request = ListTablesRequest.builder().build();
            ListTablesResponse response = dynamoDbClient.listTables(request);
            
            result.put("status", "SUCCESS");
            result.put("region", awsRegion);
            result.put("tables_count", response.tableNames().size());
            result.put("tables", response.tableNames());
            
            logger.info("DynamoDB connectivity test successful. Found {} tables", 
                       response.tableNames().size());
            
        } catch (Exception e) {
            logger.error("DynamoDB connectivity test failed", e);
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Comprehensive AWS services health check
     */
    public Map<String, Object> performHealthCheck() {
        Map<String, Object> healthCheck = new HashMap<>();
        
        healthCheck.put("bedrock", testBedrockConnectivity());
        healthCheck.put("dynamodb", testDynamoDbConnectivity());
        healthCheck.put("timestamp", System.currentTimeMillis());
        
        return healthCheck;
    }
}