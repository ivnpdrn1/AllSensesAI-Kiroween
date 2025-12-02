package com.allsenses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AllSenses AI Guardian - AWS AI Agent Application
 * 
 * This application demonstrates a working AI Agent on AWS that meets the three required conditions:
 * 1. Large Language Model (LLM) hosted out of AWS Bedrock
 * 2. Uses AWS services (Bedrock, DynamoDB, Lambda, SNS, API Gateway)
 * 3. Meets AWS-defined AI agent qualification:
 *    - Uses reasoning LLMs for decision-making
 *    - Demonstrates autonomous capabilities
 *    - Integrates APIs, databases, and external tools
 */
@SpringBootApplication
@EnableAsync
public class AllSensesAiGuardianApplication {

    public static void main(String[] args) {
        SpringApplication.run(AllSensesAiGuardianApplication.class, args);
    }
}