package com.allsenses.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Threat Assessment Reasoning Service for AllSenses AI Guardian
 * 
 * This service uses AWS Bedrock LLMs (Claude/Titan) for autonomous threat assessment reasoning.
 * It demonstrates AI agent qualification by using reasoning LLMs for decision-making
 * and autonomous threat detection capabilities.
 */
@Service
public class ThreatAssessmentReasoningService {

    private static final Logger logger = LoggerFactory.getLogger(ThreatAssessmentReasoningService.class);

    @Autowired
    private BedrockLlmService bedrockLlmService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Perform autonomous threat assessment using LLM reasoning
     * 
     * @param sensorData Sensor data context for threat assessment
     * @return Threat assessment result with LLM reasoning
     */
    public ThreatAssessmentResult performThreatAssessment(SensorDataContext sensorData) {
        logger.info("Starting autonomous threat assessment with LLM reasoning");
        
        try {
            // Build context for LLM reasoning
            String reasoningContext = buildThreatAssessmentContext(sensorData);
            
            // Get LLM reasoning response
            BedrockLlmService.LlmReasoningResponse llmResponse = 
                bedrockLlmService.generateAutonomousReasoning(reasoningContext);
            
            if (!llmResponse.isSuccess()) {
                logger.error("LLM reasoning failed for threat assessment");
                return createFailedAssessment(sensorData, "LLM reasoning failed: " + llmResponse.getResponse());
            }
            
            // Parse LLM response for structured threat assessment
            ThreatAssessmentResult result = parseLlmThreatAssessment(llmResponse, sensorData);
            
            logger.info("Autonomous threat assessment completed. Threat Level: {}, Confidence: {}", 
                       result.getThreatLevel(), result.getConfidenceScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during autonomous threat assessment", e);
            return createFailedAssessment(sensorData, "Assessment error: " + e.getMessage());
        }
    }

    /**
     * Build context string for LLM threat assessment reasoning
     */
    private String buildThreatAssessmentContext(SensorDataContext sensorData) {
        StringBuilder context = new StringBuilder();
        context.append("SENSOR DATA ANALYSIS REQUEST\n");
        context.append("Timestamp: ").append(LocalDateTime.now()).append("\n");
        context.append("Location: ").append(sensorData.getLocation()).append("\n");
        context.append("User ID: ").append(sensorData.getUserId()).append("\n\n");
        
        context.append("SENSOR READINGS:\n");
        if (sensorData.getAudioData() != null) {
            context.append("- Audio: ").append(sensorData.getAudioData()).append("\n");
        }
        if (sensorData.getMotionData() != null) {
            context.append("- Motion: ").append(sensorData.getMotionData()).append("\n");
        }
        if (sensorData.getEnvironmentalData() != null) {
            context.append("- Environmental: ").append(sensorData.getEnvironmentalData()).append("\n");
        }
        if (sensorData.getBiometricData() != null) {
            context.append("- Biometric: ").append(sensorData.getBiometricData()).append("\n");
        }
        
        context.append("\nCONTEXT: ").append(sensorData.getAdditionalContext());
        
        return context.toString();
    }

    /**
     * Parse LLM response into structured threat assessment result
     */
    private ThreatAssessmentResult parseLlmThreatAssessment(
            BedrockLlmService.LlmReasoningResponse llmResponse, 
            SensorDataContext sensorData) {
        
        String response = llmResponse.getResponse();
        
        // Extract threat level using pattern matching
        ThreatLevel threatLevel = extractThreatLevel(response);
        
        // Extract confidence score
        double confidenceScore = extractConfidenceScore(response);
        
        // Extract recommended action
        String recommendedAction = extractRecommendedAction(response);
        
        // Extract reasoning
        String reasoning = extractReasoning(response);
        
        return ThreatAssessmentResult.builder()
                .assessmentId(generateAssessmentId())
                .userId(sensorData.getUserId())
                .threatLevel(threatLevel)
                .confidenceScore(confidenceScore)
                .recommendedAction(recommendedAction)
                .llmReasoning(reasoning)
                .llmModelUsed(llmResponse.getModelId())
                .llmTokensUsed(llmResponse.getTokensUsed())
                .rawLlmResponse(response)
                .sensorDataContext(sensorData)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * Extract threat level from LLM response
     */
    private ThreatLevel extractThreatLevel(String response) {
        String upperResponse = response.toUpperCase();
        
        if (upperResponse.contains("CRITICAL") || upperResponse.contains("IMMEDIATE DANGER")) {
            return ThreatLevel.CRITICAL;
        } else if (upperResponse.contains("HIGH") || upperResponse.contains("URGENT")) {
            return ThreatLevel.HIGH;
        } else if (upperResponse.contains("MEDIUM") || upperResponse.contains("MODERATE")) {
            return ThreatLevel.MEDIUM;
        } else if (upperResponse.contains("LOW") || upperResponse.contains("MINOR")) {
            return ThreatLevel.LOW;
        } else {
            return ThreatLevel.NONE;
        }
    }

    /**
     * Extract confidence score from LLM response
     */
    private double extractConfidenceScore(String response) {
        // Look for confidence patterns like "0.8", "80%", "8/10"
        Pattern confidencePattern = Pattern.compile("(?:confidence|certainty).*?([0-9]\\.[0-9]+|[0-9]+%|[0-9]+/10)", 
                                                   Pattern.CASE_INSENSITIVE);
        Matcher matcher = confidencePattern.matcher(response);
        
        if (matcher.find()) {
            String confidenceStr = matcher.group(1);
            
            if (confidenceStr.contains("%")) {
                return Double.parseDouble(confidenceStr.replace("%", "")) / 100.0;
            } else if (confidenceStr.contains("/")) {
                String[] parts = confidenceStr.split("/");
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } else {
                double value = Double.parseDouble(confidenceStr);
                return value > 1.0 ? value / 100.0 : value; // Handle percentage as decimal
            }
        }
        
        // Default confidence based on threat level keywords
        String upperResponse = response.toUpperCase();
        if (upperResponse.contains("CERTAIN") || upperResponse.contains("DEFINITE")) {
            return 0.9;
        } else if (upperResponse.contains("LIKELY") || upperResponse.contains("PROBABLE")) {
            return 0.7;
        } else if (upperResponse.contains("POSSIBLE") || upperResponse.contains("MAYBE")) {
            return 0.5;
        } else {
            return 0.6; // Default moderate confidence
        }
    }

    /**
     * Extract recommended action from LLM response
     */
    private String extractRecommendedAction(String response) {
        Pattern actionPattern = Pattern.compile("(?:recommended action|action|recommendation):\\s*([^\\n]+)", 
                                              Pattern.CASE_INSENSITIVE);
        Matcher matcher = actionPattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Extract action based on threat level keywords
        String upperResponse = response.toUpperCase();
        if (upperResponse.contains("EMERGENCY") || upperResponse.contains("911")) {
            return "Contact emergency services immediately";
        } else if (upperResponse.contains("ALERT") || upperResponse.contains("NOTIFY")) {
            return "Alert trusted contacts and monitor situation";
        } else if (upperResponse.contains("MONITOR") || upperResponse.contains("WATCH")) {
            return "Continue monitoring for escalation";
        } else {
            return "No immediate action required";
        }
    }

    /**
     * Extract reasoning explanation from LLM response
     */
    private String extractReasoning(String response) {
        Pattern reasoningPattern = Pattern.compile("(?:reasoning|explanation|analysis):\\s*([^\\n]+(?:\\n[^\\n]+)*)", 
                                                 Pattern.CASE_INSENSITIVE);
        Matcher matcher = reasoningPattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // Return first paragraph as reasoning if no specific section found
        String[] paragraphs = response.split("\\n\\n");
        return paragraphs.length > 0 ? paragraphs[0].trim() : response.substring(0, Math.min(200, response.length()));
    }

    /**
     * Create failed assessment result
     */
    private ThreatAssessmentResult createFailedAssessment(SensorDataContext sensorData, String errorMessage) {
        return ThreatAssessmentResult.builder()
                .assessmentId(generateAssessmentId())
                .userId(sensorData.getUserId())
                .threatLevel(ThreatLevel.NONE)
                .confidenceScore(0.0)
                .recommendedAction("Assessment failed - manual review required")
                .llmReasoning("LLM assessment failed: " + errorMessage)
                .sensorDataContext(sensorData)
                .timestamp(LocalDateTime.now())
                .success(false)
                .build();
    }

    /**
     * Generate unique assessment ID
     */
    private String generateAssessmentId() {
        return "ASSESS-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * Test threat assessment reasoning with sample data
     */
    public ThreatAssessmentResult testThreatAssessmentReasoning() {
        SensorDataContext testData = SensorDataContext.builder()
                .userId("test-user-123")
                .location("Test Location")
                .audioData("Elevated voice levels detected, possible distress sounds")
                .motionData("Rapid movement patterns, possible struggle")
                .environmentalData("Normal temperature and humidity")
                .additionalContext("User in unfamiliar location at night")
                .build();
        
        return performThreatAssessment(testData);
    }

    /**
     * Threat Level enumeration
     */
    public enum ThreatLevel {
        NONE, LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Sensor Data Context for threat assessment
     */
    public static class SensorDataContext {
        private String userId;
        private String location;
        private String audioData;
        private String motionData;
        private String environmentalData;
        private String biometricData;
        private String additionalContext;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SensorDataContext context = new SensorDataContext();

            public Builder userId(String userId) { context.userId = userId; return this; }
            public Builder location(String location) { context.location = location; return this; }
            public Builder audioData(String audioData) { context.audioData = audioData; return this; }
            public Builder motionData(String motionData) { context.motionData = motionData; return this; }
            public Builder environmentalData(String environmentalData) { context.environmentalData = environmentalData; return this; }
            public Builder biometricData(String biometricData) { context.biometricData = biometricData; return this; }
            public Builder additionalContext(String additionalContext) { context.additionalContext = additionalContext; return this; }

            public SensorDataContext build() { return context; }
        }

        // Getters
        public String getUserId() { return userId; }
        public String getLocation() { return location; }
        public String getAudioData() { return audioData; }
        public String getMotionData() { return motionData; }
        public String getEnvironmentalData() { return environmentalData; }
        public String getBiometricData() { return biometricData; }
        public String getAdditionalContext() { return additionalContext; }
    }

    /**
     * Threat Assessment Result with LLM reasoning
     */
    public static class ThreatAssessmentResult {
        private String assessmentId;
        private String userId;
        private ThreatLevel threatLevel;
        private double confidenceScore;
        private String recommendedAction;
        private String llmReasoning;
        private String llmModelUsed;
        private int llmTokensUsed;
        private String rawLlmResponse;
        private SensorDataContext sensorDataContext;
        private LocalDateTime timestamp;
        private boolean success;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ThreatAssessmentResult result = new ThreatAssessmentResult();

            public Builder assessmentId(String assessmentId) { result.assessmentId = assessmentId; return this; }
            public Builder userId(String userId) { result.userId = userId; return this; }
            public Builder threatLevel(ThreatLevel threatLevel) { result.threatLevel = threatLevel; return this; }
            public Builder confidenceScore(double confidenceScore) { result.confidenceScore = confidenceScore; return this; }
            public Builder recommendedAction(String recommendedAction) { result.recommendedAction = recommendedAction; return this; }
            public Builder llmReasoning(String llmReasoning) { result.llmReasoning = llmReasoning; return this; }
            public Builder llmModelUsed(String llmModelUsed) { result.llmModelUsed = llmModelUsed; return this; }
            public Builder llmTokensUsed(int llmTokensUsed) { result.llmTokensUsed = llmTokensUsed; return this; }
            public Builder rawLlmResponse(String rawLlmResponse) { result.rawLlmResponse = rawLlmResponse; return this; }
            public Builder sensorDataContext(SensorDataContext sensorDataContext) { result.sensorDataContext = sensorDataContext; return this; }
            public Builder timestamp(LocalDateTime timestamp) { result.timestamp = timestamp; return this; }
            public Builder success(boolean success) { result.success = success; return this; }

            public ThreatAssessmentResult build() { return result; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getUserId() { return userId; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getRecommendedAction() { return recommendedAction; }
        public String getLlmReasoning() { return llmReasoning; }
        public String getLlmModelUsed() { return llmModelUsed; }
        public int getLlmTokensUsed() { return llmTokensUsed; }
        public String getRawLlmResponse() { return rawLlmResponse; }
        public SensorDataContext getSensorDataContext() { return sensorDataContext; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
    }
}