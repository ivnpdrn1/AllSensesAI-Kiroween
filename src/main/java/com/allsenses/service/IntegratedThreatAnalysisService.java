package com.allsenses.service;

import com.allsenses.model.ThreatAssessment;
import com.allsenses.repository.ThreatAssessmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integrated Threat Analysis Service for AllSenses AI Guardian
 * 
 * This service combines AWS Bedrock LLM reasoning with DynamoDB persistence
 * to provide complete threat analysis capabilities. It demonstrates all three
 * AWS AI Agent qualification conditions:
 * 
 * 1. LLM Integration: Uses AWS Bedrock for reasoning
 * 2. AWS Services: Integrates Bedrock + DynamoDB
 * 3. AI Agent Qualification: Autonomous threat analysis with database integration
 */
@Service
public class IntegratedThreatAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(IntegratedThreatAnalysisService.class);

    @Autowired
    private ThreatAssessmentReasoningService threatAssessmentReasoningService;

    @Autowired
    private ThreatAssessmentRepository threatAssessmentRepository;

    @Autowired
    private BedrockLlmService bedrockLlmService;

    /**
     * Perform complete threat analysis with LLM reasoning and database persistence
     * 
     * @param sensorData Sensor data context for analysis
     * @return Persisted threat assessment with LLM reasoning
     */
    public ThreatAssessment performCompleteThreatAnalysis(SensorDataInput sensorData) {
        logger.info("Starting complete threat analysis for user: {}", sensorData.getUserId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Convert input to reasoning service format
            ThreatAssessmentReasoningService.SensorDataContext reasoningContext = 
                convertToReasoningContext(sensorData);
            
            // Step 2: Perform LLM-powered threat assessment
            ThreatAssessmentReasoningService.ThreatAssessmentResult reasoningResult = 
                threatAssessmentReasoningService.performThreatAssessment(reasoningContext);
            
            if (!reasoningResult.isSuccess()) {
                logger.error("LLM threat assessment failed for user: {}", sensorData.getUserId());
                return createFailedAssessment(sensorData, "LLM reasoning failed", startTime);
            }
            
            // Step 3: Convert reasoning result to DynamoDB entity
            ThreatAssessment assessment = convertToThreatAssessmentEntity(reasoningResult, sensorData, startTime);
            
            // Step 4: Persist to DynamoDB
            ThreatAssessment savedAssessment = threatAssessmentRepository.save(assessment);
            
            logger.info("Complete threat analysis completed. Assessment ID: {}, Threat Level: {}, Confidence: {}", 
                       savedAssessment.getAssessmentId(), 
                       savedAssessment.getThreatLevel(), 
                       savedAssessment.getConfidenceScore());
            
            return savedAssessment;
            
        } catch (Exception e) {
            logger.error("Error during complete threat analysis", e);
            return createFailedAssessment(sensorData, "Analysis error: " + e.getMessage(), startTime);
        }
    }

    /**
     * Retrieve threat assessment by ID
     */
    public ThreatAssessment getThreatAssessment(String assessmentId) {
        return threatAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Threat assessment not found: " + assessmentId));
    }

    /**
     * Get recent threat assessments for a user
     */
    public java.util.List<ThreatAssessment> getRecentAssessmentsForUser(String userId) {
        return threatAssessmentRepository.findByUserId(userId);
    }

    /**
     * Get all high-threat assessments
     */
    public java.util.List<ThreatAssessment> getHighThreatAssessments() {
        return threatAssessmentRepository.findHighThreatAssessments();
    }

    /**
     * Update assessment status
     */
    public ThreatAssessment updateAssessmentStatus(String assessmentId, String newStatus) {
        return threatAssessmentRepository.updateStatus(assessmentId, newStatus);
    }

    /**
     * Test complete threat analysis with sample data
     */
    public ThreatAssessment testCompleteThreatAnalysis() {
        SensorDataInput testData = SensorDataInput.builder()
                .userId("test-user-integrated")
                .location("Test Location - Integrated Analysis")
                .audioData("Elevated voice levels, possible distress sounds detected")
                .motionData("Rapid movement patterns suggesting struggle or distress")
                .environmentalData("Normal temperature and humidity levels")
                .biometricData("Elevated heart rate: 120 BPM, stress indicators present")
                .additionalContext("User in unfamiliar location at night, GPS shows isolated area")
                .build();
        
        return performCompleteThreatAnalysis(testData);
    }

    /**
     * Get threat analysis statistics
     */
    public Map<String, Object> getThreatAnalysisStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalAssessments = threatAssessmentRepository.countAll();
            java.util.List<ThreatAssessment> highThreatAssessments = threatAssessmentRepository.findHighThreatAssessments();
            java.util.List<ThreatAssessment> recentAssessments = threatAssessmentRepository.findRecentAssessments();
            
            stats.put("total_assessments", totalAssessments);
            stats.put("high_threat_assessments", highThreatAssessments.size());
            stats.put("recent_assessments_24h", recentAssessments.size());
            
            // Calculate threat level distribution
            Map<String, Long> threatLevelDistribution = new HashMap<>();
            recentAssessments.forEach(assessment -> {
                String level = assessment.getThreatLevel();
                threatLevelDistribution.put(level, threatLevelDistribution.getOrDefault(level, 0L) + 1);
            });
            stats.put("threat_level_distribution", threatLevelDistribution);
            
            // Calculate average confidence score
            double avgConfidence = recentAssessments.stream()
                    .filter(a -> a.getConfidenceScore() != null)
                    .mapToDouble(ThreatAssessment::getConfidenceScore)
                    .average()
                    .orElse(0.0);
            stats.put("average_confidence_score", avgConfidence);
            
            stats.put("status", "SUCCESS");
            
        } catch (Exception e) {
            stats.put("status", "ERROR");
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    /**
     * Convert sensor data input to reasoning context
     */
    private ThreatAssessmentReasoningService.SensorDataContext convertToReasoningContext(SensorDataInput input) {
        return ThreatAssessmentReasoningService.SensorDataContext.builder()
                .userId(input.getUserId())
                .location(input.getLocation())
                .audioData(input.getAudioData())
                .motionData(input.getMotionData())
                .environmentalData(input.getEnvironmentalData())
                .biometricData(input.getBiometricData())
                .additionalContext(input.getAdditionalContext())
                .build();
    }

    /**
     * Convert reasoning result to DynamoDB entity
     */
    private ThreatAssessment convertToThreatAssessmentEntity(
            ThreatAssessmentReasoningService.ThreatAssessmentResult reasoningResult,
            SensorDataInput sensorData,
            long startTime) {
        
        ThreatAssessment assessment = new ThreatAssessment();
        
        // Basic information
        assessment.setAssessmentId(reasoningResult.getAssessmentId());
        assessment.setUserId(reasoningResult.getUserId());
        assessment.setThreatLevel(reasoningResult.getThreatLevel().toString());
        assessment.setConfidenceScore(reasoningResult.getConfidenceScore());
        assessment.setStatus("COMPLETED");
        
        // LLM reasoning data
        assessment.setLlmModelUsed(reasoningResult.getLlmModelUsed());
        assessment.setLlmReasoning(reasoningResult.getLlmReasoning());
        assessment.setRecommendedAction(reasoningResult.getRecommendedAction());
        assessment.setLlmTokensUsed(reasoningResult.getLlmTokensUsed());
        assessment.setRawLlmResponse(reasoningResult.getRawLlmResponse());
        
        // Processing metrics
        long processingTime = System.currentTimeMillis() - startTime;
        assessment.setProcessingDurationMs(processingTime);
        assessment.setAiModelVersion("bedrock-claude-3-sonnet-v1.0");
        
        // Sensor data
        Map<String, String> sensorDataMap = new HashMap<>();
        if (sensorData.getAudioData() != null) sensorDataMap.put("audio", sensorData.getAudioData());
        if (sensorData.getMotionData() != null) sensorDataMap.put("motion", sensorData.getMotionData());
        if (sensorData.getEnvironmentalData() != null) sensorDataMap.put("environmental", sensorData.getEnvironmentalData());
        if (sensorData.getBiometricData() != null) sensorDataMap.put("biometric", sensorData.getBiometricData());
        if (sensorData.getAdditionalContext() != null) sensorDataMap.put("context", sensorData.getAdditionalContext());
        assessment.setSensorData(sensorDataMap);
        
        // Location data (simplified for MVP)
        if (sensorData.getLocation() != null) {
            ThreatAssessment.LocationData location = new ThreatAssessment.LocationData();
            location.setAddress(sensorData.getLocation());
            // For MVP, we'll use mock coordinates
            location.setLatitude(40.7128); // NYC coordinates as example
            location.setLongitude(-74.0060);
            location.setAccuracyMeters(10.0);
            assessment.setLocation(location);
        }
        
        // Timestamps
        assessment.setTimestamp(Instant.now());
        
        return assessment;
    }

    /**
     * Create failed assessment entity
     */
    private ThreatAssessment createFailedAssessment(SensorDataInput sensorData, String errorMessage, long startTime) {
        ThreatAssessment assessment = new ThreatAssessment();
        
        assessment.setAssessmentId("FAILED-" + UUID.randomUUID().toString());
        assessment.setUserId(sensorData.getUserId());
        assessment.setThreatLevel("NONE");
        assessment.setConfidenceScore(0.0);
        assessment.setStatus("FAILED");
        assessment.setLlmReasoning("Assessment failed: " + errorMessage);
        assessment.setRecommendedAction("Manual review required");
        assessment.setProcessingDurationMs(System.currentTimeMillis() - startTime);
        assessment.setTimestamp(Instant.now());
        
        // Save failed assessment for debugging
        try {
            return threatAssessmentRepository.save(assessment);
        } catch (Exception e) {
            logger.error("Failed to save failed assessment", e);
            return assessment;
        }
    }

    /**
     * Sensor data input class for threat analysis
     */
    public static class SensorDataInput {
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
            private SensorDataInput input = new SensorDataInput();

            public Builder userId(String userId) { input.userId = userId; return this; }
            public Builder location(String location) { input.location = location; return this; }
            public Builder audioData(String audioData) { input.audioData = audioData; return this; }
            public Builder motionData(String motionData) { input.motionData = motionData; return this; }
            public Builder environmentalData(String environmentalData) { input.environmentalData = environmentalData; return this; }
            public Builder biometricData(String biometricData) { input.biometricData = biometricData; return this; }
            public Builder additionalContext(String additionalContext) { input.additionalContext = additionalContext; return this; }

            public SensorDataInput build() { return input; }
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
}