package com.allsenses.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Autonomous Confidence Scoring Service for AllSenses AI Guardian
 * 
 * This service uses AWS Bedrock LLMs to perform sophisticated confidence scoring
 * for threat assessments. It demonstrates autonomous AI agent capabilities by
 * using LLM reasoning to evaluate and score the confidence of threat detections.
 */
@Service
public class AutonomousConfidenceScoringService {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousConfidenceScoringService.class);

    @Autowired
    private BedrockLlmService bedrockLlmService;

    /**
     * Generate autonomous confidence score using LLM analysis
     * 
     * @param threatAssessmentData Data from the initial threat assessment
     * @param initialConfidence Initial confidence score from basic analysis
     * @return Enhanced confidence analysis result
     */
    public ConfidenceAnalysisResult generateAutonomousConfidenceScore(
            ThreatAssessmentData threatAssessmentData, 
            double initialConfidence) {
        
        logger.info("Starting autonomous confidence scoring for assessment: {}", 
                   threatAssessmentData.getAssessmentId());
        
        try {
            // Build confidence analysis prompt
            String confidencePrompt = buildConfidenceAnalysisPrompt(threatAssessmentData, initialConfidence);
            
            // Get LLM confidence analysis
            BedrockLlmService.LlmReasoningResponse llmResponse = 
                bedrockLlmService.generateAutonomousReasoning(confidencePrompt);
            
            if (!llmResponse.isSuccess()) {
                logger.error("LLM confidence analysis failed");
                return createFailedConfidenceAnalysis(initialConfidence, "LLM analysis failed");
            }
            
            // Parse LLM response for confidence metrics
            ConfidenceMetrics metrics = parseConfidenceMetrics(llmResponse.getResponse());
            
            // Calculate final confidence score
            double finalConfidence = calculateFinalConfidenceScore(
                initialConfidence, metrics, threatAssessmentData);
            
            // Create confidence analysis result
            ConfidenceAnalysisResult result = ConfidenceAnalysisResult.builder()
                    .assessmentId(threatAssessmentData.getAssessmentId())
                    .initialConfidence(initialConfidence)
                    .finalConfidence(finalConfidence)
                    .confidenceMetrics(metrics)
                    .llmAnalysis(llmResponse.getResponse())
                    .llmModelUsed(llmResponse.getModelId())
                    .llmTokensUsed(llmResponse.getTokensUsed())
                    .confidenceFactors(identifyConfidenceFactors(threatAssessmentData, metrics))
                    .success(true)
                    .build();
            
            logger.info("Autonomous confidence scoring completed. Initial: {}, Final: {}", 
                       initialConfidence, finalConfidence);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during autonomous confidence scoring", e);
            return createFailedConfidenceAnalysis(initialConfidence, "Scoring error: " + e.getMessage());
        }
    }

    /**
     * Validate confidence score against multiple criteria
     */
    public ConfidenceValidationResult validateConfidenceScore(
            double confidenceScore, 
            String threatLevel, 
            ThreatAssessmentData assessmentData) {
        
        logger.info("Validating confidence score: {} for threat level: {}", confidenceScore, threatLevel);
        
        try {
            // Build validation prompt
            String validationPrompt = buildConfidenceValidationPrompt(
                confidenceScore, threatLevel, assessmentData);
            
            // Get LLM validation analysis
            BedrockLlmService.LlmReasoningResponse llmResponse = 
                bedrockLlmService.generateAutonomousReasoning(validationPrompt);
            
            if (!llmResponse.isSuccess()) {
                return ConfidenceValidationResult.builder()
                        .isValid(false)
                        .validationReason("LLM validation failed")
                        .build();
            }
            
            // Parse validation result
            boolean isValid = parseValidationResult(llmResponse.getResponse());
            String validationReason = extractValidationReason(llmResponse.getResponse());
            double adjustedConfidence = extractAdjustedConfidence(llmResponse.getResponse(), confidenceScore);
            
            return ConfidenceValidationResult.builder()
                    .isValid(isValid)
                    .validationReason(validationReason)
                    .adjustedConfidence(adjustedConfidence)
                    .llmValidationAnalysis(llmResponse.getResponse())
                    .build();
            
        } catch (Exception e) {
            logger.error("Error during confidence validation", e);
            return ConfidenceValidationResult.builder()
                    .isValid(false)
                    .validationReason("Validation error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Test autonomous confidence scoring with sample data
     */
    public ConfidenceAnalysisResult testAutonomousConfidenceScoring() {
        ThreatAssessmentData testData = ThreatAssessmentData.builder()
                .assessmentId("TEST-CONFIDENCE-001")
                .userId("test-user-confidence")
                .threatLevel("HIGH")
                .sensorDataQuality("GOOD")
                .dataConsistency("CONSISTENT")
                .environmentalFactors("ISOLATED_LOCATION")
                .historicalContext("NO_PREVIOUS_INCIDENTS")
                .build();
        
        return generateAutonomousConfidenceScore(testData, 0.75);
    }

    /**
     * Build confidence analysis prompt for LLM
     */
    private String buildConfidenceAnalysisPrompt(ThreatAssessmentData data, double initialConfidence) {
        return String.format("""
            You are an AI agent responsible for autonomous confidence scoring in threat detection.
            
            THREAT ASSESSMENT DATA:
            - Assessment ID: %s
            - User ID: %s
            - Threat Level: %s
            - Initial Confidence: %.2f
            - Sensor Data Quality: %s
            - Data Consistency: %s
            - Environmental Factors: %s
            - Historical Context: %s
            
            TASK: Analyze the confidence level of this threat assessment.
            
            Consider these factors:
            1. Data Quality: How reliable is the sensor data?
            2. Consistency: Are multiple data sources consistent?
            3. Environmental Context: Do environmental factors support the assessment?
            4. Historical Patterns: Does this match known threat patterns?
            5. Uncertainty Factors: What could reduce confidence?
            
            Provide your analysis in this format:
            CONFIDENCE_ANALYSIS: [Your detailed analysis]
            DATA_QUALITY_SCORE: [0.0-1.0]
            CONSISTENCY_SCORE: [0.0-1.0]
            CONTEXT_SCORE: [0.0-1.0]
            UNCERTAINTY_FACTORS: [List key uncertainty factors]
            RECOMMENDED_CONFIDENCE: [0.0-1.0]
            REASONING: [Explain your confidence assessment]
            """, 
            data.getAssessmentId(), data.getUserId(), data.getThreatLevel(),
            initialConfidence, data.getSensorDataQuality(), data.getDataConsistency(),
            data.getEnvironmentalFactors(), data.getHistoricalContext());
    }

    /**
     * Build confidence validation prompt
     */
    private String buildConfidenceValidationPrompt(
            double confidenceScore, String threatLevel, ThreatAssessmentData data) {
        return String.format("""
            You are validating a confidence score for threat detection.
            
            ASSESSMENT DETAILS:
            - Confidence Score: %.2f
            - Threat Level: %s
            - Sensor Data Quality: %s
            - Data Consistency: %s
            
            VALIDATION CRITERIA:
            - HIGH/CRITICAL threats should have confidence >= 0.7
            - MEDIUM threats should have confidence >= 0.5
            - LOW threats should have confidence >= 0.3
            - Confidence should match data quality and consistency
            
            TASK: Validate if this confidence score is appropriate.
            
            Respond with:
            VALIDATION_RESULT: [VALID/INVALID]
            VALIDATION_REASON: [Explain why valid or invalid]
            ADJUSTED_CONFIDENCE: [Suggest adjusted score if needed, or same if valid]
            """, 
            confidenceScore, threatLevel, data.getSensorDataQuality(), data.getDataConsistency());
    }

    /**
     * Parse confidence metrics from LLM response
     */
    private ConfidenceMetrics parseConfidenceMetrics(String llmResponse) {
        ConfidenceMetrics metrics = new ConfidenceMetrics();
        
        // Extract scores using pattern matching
        metrics.dataQualityScore = extractScore(llmResponse, "DATA_QUALITY_SCORE:", 0.5);
        metrics.consistencyScore = extractScore(llmResponse, "CONSISTENCY_SCORE:", 0.5);
        metrics.contextScore = extractScore(llmResponse, "CONTEXT_SCORE:", 0.5);
        metrics.recommendedConfidence = extractScore(llmResponse, "RECOMMENDED_CONFIDENCE:", 0.5);
        
        // Extract uncertainty factors
        metrics.uncertaintyFactors = extractSection(llmResponse, "UNCERTAINTY_FACTORS:", "");
        
        return metrics;
    }

    /**
     * Calculate final confidence score based on multiple factors
     */
    private double calculateFinalConfidenceScore(
            double initialConfidence, 
            ConfidenceMetrics metrics, 
            ThreatAssessmentData data) {
        
        // Weighted combination of factors
        double dataQualityWeight = 0.3;
        double consistencyWeight = 0.3;
        double contextWeight = 0.2;
        double llmRecommendationWeight = 0.2;
        
        double weightedScore = 
            (metrics.dataQualityScore * dataQualityWeight) +
            (metrics.consistencyScore * consistencyWeight) +
            (metrics.contextScore * contextWeight) +
            (metrics.recommendedConfidence * llmRecommendationWeight);
        
        // Blend with initial confidence (70% weighted, 30% initial)
        double finalScore = (weightedScore * 0.7) + (initialConfidence * 0.3);
        
        // Ensure score is within valid range
        return Math.max(0.0, Math.min(1.0, finalScore));
    }

    /**
     * Identify key confidence factors
     */
    private Map<String, String> identifyConfidenceFactors(
            ThreatAssessmentData data, ConfidenceMetrics metrics) {
        
        Map<String, String> factors = new HashMap<>();
        
        factors.put("data_quality", String.format("%.2f - %s", 
            metrics.dataQualityScore, data.getSensorDataQuality()));
        factors.put("consistency", String.format("%.2f - %s", 
            metrics.consistencyScore, data.getDataConsistency()));
        factors.put("context", String.format("%.2f - %s", 
            metrics.contextScore, data.getEnvironmentalFactors()));
        factors.put("uncertainty_factors", metrics.uncertaintyFactors);
        
        return factors;
    }

    /**
     * Extract numerical score from LLM response
     */
    private double extractScore(String response, String pattern, double defaultValue) {
        try {
            int startIndex = response.indexOf(pattern);
            if (startIndex == -1) return defaultValue;
            
            startIndex += pattern.length();
            int endIndex = response.indexOf('\n', startIndex);
            if (endIndex == -1) endIndex = response.length();
            
            String scoreStr = response.substring(startIndex, endIndex).trim();
            return Double.parseDouble(scoreStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Extract text section from LLM response
     */
    private String extractSection(String response, String pattern, String defaultValue) {
        try {
            int startIndex = response.indexOf(pattern);
            if (startIndex == -1) return defaultValue;
            
            startIndex += pattern.length();
            int endIndex = response.indexOf('\n', startIndex);
            if (endIndex == -1) endIndex = response.length();
            
            return response.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parse validation result from LLM response
     */
    private boolean parseValidationResult(String response) {
        return response.toUpperCase().contains("VALIDATION_RESULT: VALID");
    }

    /**
     * Extract validation reason from LLM response
     */
    private String extractValidationReason(String response) {
        return extractSection(response, "VALIDATION_REASON:", "No reason provided");
    }

    /**
     * Extract adjusted confidence from LLM response
     */
    private double extractAdjustedConfidence(String response, double originalConfidence) {
        double adjusted = extractScore(response, "ADJUSTED_CONFIDENCE:", originalConfidence);
        return Math.max(0.0, Math.min(1.0, adjusted));
    }

    /**
     * Create failed confidence analysis result
     */
    private ConfidenceAnalysisResult createFailedConfidenceAnalysis(double initialConfidence, String errorMessage) {
        return ConfidenceAnalysisResult.builder()
                .initialConfidence(initialConfidence)
                .finalConfidence(initialConfidence) // Keep original on failure
                .llmAnalysis("Confidence analysis failed: " + errorMessage)
                .success(false)
                .build();
    }

    // Data classes
    public static class ThreatAssessmentData {
        private String assessmentId;
        private String userId;
        private String threatLevel;
        private String sensorDataQuality;
        private String dataConsistency;
        private String environmentalFactors;
        private String historicalContext;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ThreatAssessmentData data = new ThreatAssessmentData();
            public Builder assessmentId(String assessmentId) { data.assessmentId = assessmentId; return this; }
            public Builder userId(String userId) { data.userId = userId; return this; }
            public Builder threatLevel(String threatLevel) { data.threatLevel = threatLevel; return this; }
            public Builder sensorDataQuality(String sensorDataQuality) { data.sensorDataQuality = sensorDataQuality; return this; }
            public Builder dataConsistency(String dataConsistency) { data.dataConsistency = dataConsistency; return this; }
            public Builder environmentalFactors(String environmentalFactors) { data.environmentalFactors = environmentalFactors; return this; }
            public Builder historicalContext(String historicalContext) { data.historicalContext = historicalContext; return this; }
            public ThreatAssessmentData build() { return data; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getUserId() { return userId; }
        public String getThreatLevel() { return threatLevel; }
        public String getSensorDataQuality() { return sensorDataQuality; }
        public String getDataConsistency() { return dataConsistency; }
        public String getEnvironmentalFactors() { return environmentalFactors; }
        public String getHistoricalContext() { return historicalContext; }
    }

    public static class ConfidenceMetrics {
        public double dataQualityScore;
        public double consistencyScore;
        public double contextScore;
        public double recommendedConfidence;
        public String uncertaintyFactors;
    }

    public static class ConfidenceAnalysisResult {
        private String assessmentId;
        private double initialConfidence;
        private double finalConfidence;
        private ConfidenceMetrics confidenceMetrics;
        private String llmAnalysis;
        private String llmModelUsed;
        private int llmTokensUsed;
        private Map<String, String> confidenceFactors;
        private boolean success;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ConfidenceAnalysisResult result = new ConfidenceAnalysisResult();
            public Builder assessmentId(String assessmentId) { result.assessmentId = assessmentId; return this; }
            public Builder initialConfidence(double initialConfidence) { result.initialConfidence = initialConfidence; return this; }
            public Builder finalConfidence(double finalConfidence) { result.finalConfidence = finalConfidence; return this; }
            public Builder confidenceMetrics(ConfidenceMetrics confidenceMetrics) { result.confidenceMetrics = confidenceMetrics; return this; }
            public Builder llmAnalysis(String llmAnalysis) { result.llmAnalysis = llmAnalysis; return this; }
            public Builder llmModelUsed(String llmModelUsed) { result.llmModelUsed = llmModelUsed; return this; }
            public Builder llmTokensUsed(int llmTokensUsed) { result.llmTokensUsed = llmTokensUsed; return this; }
            public Builder confidenceFactors(Map<String, String> confidenceFactors) { result.confidenceFactors = confidenceFactors; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public ConfidenceAnalysisResult build() { return result; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public double getInitialConfidence() { return initialConfidence; }
        public double getFinalConfidence() { return finalConfidence; }
        public ConfidenceMetrics getConfidenceMetrics() { return confidenceMetrics; }
        public String getLlmAnalysis() { return llmAnalysis; }
        public String getLlmModelUsed() { return llmModelUsed; }
        public int getLlmTokensUsed() { return llmTokensUsed; }
        public Map<String, String> getConfidenceFactors() { return confidenceFactors; }
        public boolean isSuccess() { return success; }
    }

    public static class ConfidenceValidationResult {
        private boolean isValid;
        private String validationReason;
        private double adjustedConfidence;
        private String llmValidationAnalysis;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ConfidenceValidationResult result = new ConfidenceValidationResult();
            public Builder isValid(boolean isValid) { result.isValid = isValid; return this; }
            public Builder validationReason(String validationReason) { result.validationReason = validationReason; return this; }
            public Builder adjustedConfidence(double adjustedConfidence) { result.adjustedConfidence = adjustedConfidence; return this; }
            public Builder llmValidationAnalysis(String llmValidationAnalysis) { result.llmValidationAnalysis = llmValidationAnalysis; return this; }
            public ConfidenceValidationResult build() { return result; }
        }

        // Getters
        public boolean isValid() { return isValid; }
        public String getValidationReason() { return validationReason; }
        public double getAdjustedConfidence() { return adjustedConfidence; }
        public String getLlmValidationAnalysis() { return llmValidationAnalysis; }
    }
}