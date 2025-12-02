package com.allsenses.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrock.BedrockClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

/**
 * AWS Configuration for AllSenses AI Guardian
 * 
 * Configures all AWS service clients for production deployment
 * with proper credential management and regional configuration.
 */
@Configuration
@Profile({"aws", "production"})
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.bedrock.primary-model:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String primaryModel;

    @Value("${aws.bedrock.fallback-model:amazon.titan-text-express-v1}")
    private String fallbackModel;

    /**
     * AWS Bedrock client for LLM inference
     */
    @Bean
    public BedrockClient bedrockClient() {
        return BedrockClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * DynamoDB client for data persistence
     */
    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * SNS client for notifications
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Lambda client for serverless functions
     */
    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * CloudWatch client for monitoring
     */
    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * AWS configuration properties bean
     */
    @Bean
    public AwsProperties awsProperties() {
        AwsProperties properties = new AwsProperties();
        properties.setRegion(awsRegion);
        properties.setPrimaryModel(primaryModel);
        properties.setFallbackModel(fallbackModel);
        return properties;
    }

    /**
     * AWS Properties holder
     */
    public static class AwsProperties {
        private String region;
        private String primaryModel;
        private String fallbackModel;

        // Getters and Setters
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getPrimaryModel() { return primaryModel; }
        public void setPrimaryModel(String primaryModel) { this.primaryModel = primaryModel; }
        public String getFallbackModel() { return fallbackModel; }
        public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
    }
}