package com.allsenses.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * DynamoDB Configuration for AllSenses AI Guardian
 * 
 * Configures DynamoDB clients for data persistence, demonstrating
 * Condition 2 (AWS Services) and Condition 3 (External Integrations)
 * of the AI agent qualification requirements.
 */
@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    /**
     * DynamoDB client configuration
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClient.Builder builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create());

        // Use local DynamoDB endpoint if specified (for development)
        if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isEmpty()) {
            builder.endpointOverride(URI.create(dynamoDbEndpoint));
        }

        return builder.build();
    }

    /**
     * DynamoDB Enhanced client for object mapping
     */
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}