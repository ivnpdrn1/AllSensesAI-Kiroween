package com.allsenses.controller;

import com.allsenses.model.ThreatAssessment;
import com.allsenses.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Threat Detection API Controller for AllSenses AI Guardian
 * 
 * This controller provides specialized threat detection endpoints with
 * AWS Bedrock LLM integration, demonstrating advanced AI agent capabilities
 * for autonomous threat analysis and classification.
 */
@RestController
@RequestMapping("/api/v1/threat-detection")
@CrossOrigin(origins = "*")
public class ThreatDetectionApiController {

    @Autowired
    private IntegratedThreatAnalysisService threatAnalysisService;

    @Autowired
    private AutonomousConfidenceScoringService confidenceScoringService;

    @Autowired
    private ReasoningBasedThreatClassificationService threatClassificationService;

    @Autowired
    private BedrockLlmService bedrockLlmService;

    /**
     * Perform comprehensive threat analysis with LLM reasoning
     */
    @PostMapping("/analyze")
    public ResponseEntity<ThreatAnalysisResponse> analyzeThreat(
            @RequestBody ThreatAnalysisRequest request) {
        
        try {
            // Perform integrated threat analysis
            IntegratedThreatAnalysisService.SensorDataInput sensorInput = 
                IntegratedThreatAnalysisService.SensorDataInput.builder()
                    .userId(request.getUserId())
                    .location(request.getLocation())
                    .audioData(request.getAudioData())
                    .motionData(request.getMotionData())
                    .environmentalData(request.getEnvironmentalData())
                    .biometricData(request.getBiometricData())
                    .additionalContext(request.getAdditionalContext())
                    .build();
            
            ThreatAssessment assessment = threatAnalysisService.performCompleteThreatAnalysis(sensorInput);
            
            // Enhanced confidence scoring if requested
            AutonomousConfidenceScoringService.ConfidenceAnalysisResult confidenceAnalysis = null;
            if (request.isEnhancedConfidenceScoring()) {
                AutonomousConfidenceScoringService.ThreatAssessmentData assessmentData = 
                    AutonomousConfidenceScoringService.ThreatAssessmentData.builder()
                        .assessmentId(assessment.getAssessmentId())
                        .userId(assessment.getUserId())
                        .threatLevel(assessment.getThreatLevel())
                        .sensorDataQuality("GOOD")
                        .dataConsistency("CONSISTENT")
                        .environmentalFactors(request.getLocation())
                        .historicalContext("NO_PREVIOUS_INCIDENTS")
                        .build();
                
                confidenceAnalysis = confidenceScoringService.generateAutonomousConfidenceScore(
                    assessmentData, assessment.getConfidenceScore());
            }
            
            // Advanced threat classification if requested
            ReasoningBasedThreatClassificationService.ThreatClassificationResult classificationResult = null;
            if (request.isAdvancedClassification()) {
                ReasoningBasedThreatClassificationService.ThreatClassificationInput classificationInput = 
                    ReasoningBasedThreatClassificationService.ThreatClassificationInput.builder()
                        .assessmentId(assessment.getAssessmentId())
                        .audioData(request.getAudioData())
                        .motionData(request.getMotionData())
                        .environmentalData(request.getEnvironmentalData())
                        .biometricData(request.getBiometricData())
                        .location(request.getLocation())
                        .timeContext("Current analysis")
                        .userProfile(request.getUserId())
                        .historicalContext("No previous incidents")
                        .currentSensorData("Current sensor readings")
                        .trendData("No trend data available")
                        .build();
                
                classificationResult = threatClassificationService.performReasoningBasedClassification(classificationInput);
            }
            
            ThreatAnalysisResponse response = ThreatAnalysisResponse.builder()
                    .threatAssessment(assessment)
                    .confidenceAnalysis(confidenceAnalysis)
                    .classificationResult(classificationResult)
                    .analysisComplete(true)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ThreatAnalysisResponse.builder()
                            .analysisComplete(false)
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Perform quick threat assessment
     */
    @PostMapping("/quick-assess")
    public ResponseEntity<QuickThreatAssessmentResponse> quickThreatAssessment(
            @RequestBody QuickAssessmentRequest request) {
        
        try {
            IntegratedThreatAnalysisService.SensorDataInput sensorInput = 
                IntegratedThreatAnalysisService.SensorDataInput.builder()
                    .userId(request.getUserId())
                    .location(request.getLocation())
                    .audioData(request.getAudioData())
                    .motionData(request.getMotionData())
                    .environmentalData(request.getEnvironmentalData())
                    .biometricData(request.getBiometricData())
                    .additionalContext(request.getContext())
                    .build();
            
            ThreatAssessment assessment = threatAnalysisService.performCompleteThreatAnalysis(sensorInput);
            
            QuickThreatAssessmentResponse response = QuickThreatAssessmentResponse.builder()
                    .assessmentId(assessment.getAssessmentId())
                    .threatLevel(assessment.getThreatLevel())
                    .confidenceScore(assessment.getConfidenceScore())
                    .recommendedAction(assessment.getRecommendedAction())
                    .requiresEmergencyResponse(assessment.requiresEmergencyResponse())
                    .llmReasoning(assessment.getLlmReasoning())
                    .processingTimeMs(assessment.getProcessingDurationMs())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(QuickThreatAssessmentResponse.builder()
                            .threatLevel("UNKNOWN")
                            .confidenceScore(0.0)
                            .recommendedAction("Assessment failed - manual review required")
                            .requiresEmergencyResponse(false)
                            .errorMessage(e.getMessage())
                            .build());
        }
    }

    /**
     * Validate confidence score using LLM reasoning
     */
    @PostMapping("/validate-confidence")
    public ResponseEntity<AutonomousConfidenceScoringService.ConfidenceValidationResult> validateConfidenceScore(
            @RequestBody ConfidenceValidationRequest request) {
        
        AutonomousConfidenceScoringService.ThreatAssessmentData assessmentData = 
            AutonomousConfidenceScoringService.ThreatAssessmentData.builder()
                .assessmentId(request.getAssessmentId())
                .userId(request.getUserId())
                .threatLevel(request.getThreatLevel())
                .sensorDataQuality(request.getSensorDataQuality())
                .dataConsistency(request.getDataConsistency())
                .environmentalFactors(request.getEnvironmentalFactors())
                .historicalContext(request.getHistoricalContext())
                .build();
        
        AutonomousConfidenceScoringService.ConfidenceValidationResult result = 
            confidenceScoringService.validateConfidenceScore(
                request.getConfidenceScore(), request.getThreatLevel(), assessmentData);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Get threat detection statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getThreatDetectionStatistics() {
        Map<String, Object> statistics = threatAnalysisService.getThreatAnalysisStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Test Bedrock LLM connectivity
     */
    @GetMapping("/test/bedrock-connectivity")
    public ResponseEntity<Map<String, Object>> testBedrockConnectivity() {
        Map<String, Object> testResults = bedrockLlmService.testLlmConnectivity();
        return ResponseEntity.ok(testResults);
    }

    /**
     * Test complete threat detection pipeline
     */
    @PostMapping("/test/complete-pipeline")
    public ResponseEntity<Map<String, Object>> testCompleteThreatDetectionPipeline() {
        Map<String, Object> testResults = new HashMap<>();
        
        try {
            // Test integrated threat analysis
            ThreatAssessment threatTest = threatAnalysisService.testCompleteThreatAnalysis();
            testResults.put("threat_analysis", Map.of(
                "success", threatTest != null,
                "assessment_id", threatTest != null ? threatTest.getAssessmentId() : "null",
                "threat_level", threatTest != null ? threatTest.getThreatLevel() : "null",
                "confidence_score", threatTest != null ? threatTest.getConfidenceScore() : 0.0
            ));
            
            // Test confidence scoring
            AutonomousConfidenceScoringService.ConfidenceAnalysisResult confidenceTest = 
                confidenceScoringService.testAutonomousConfidenceScoring();
            testResults.put("confidence_scoring", Map.of(
                "success", confidenceTest.isSuccess(),
                "initial_confidence", confidenceTest.getInitialConfidence(),
                "final_confidence", confidenceTest.getFinalConfidence()
            ));
            
            // Test threat classification
            ReasoningBasedThreatClassificationService.ThreatClassificationResult classificationTest = 
                threatClassificationService.testReasoningBasedClassification();
            testResults.put("threat_classification", Map.of(
                "success", classificationTest.isSuccess(),
                "final_threat_level", classificationTest.getFinalThreatLevel(),
                "final_confidence", classificationTest.getFinalConfidenceScore()
            ));
            
            // Test Bedrock connectivity
            Map<String, Object> bedrockTest = bedrockLlmService.testLlmConnectivity();
            testResults.put("bedrock_connectivity", bedrockTest);
            
            // Overall pipeline status
            boolean pipelineSuccess = threatTest != null && confidenceTest.isSuccess() && 
                                    classificationTest.isSuccess();
            testResults.put("pipeline_status", pipelineSuccess ? "FULLY_OPERATIONAL" : "PARTIAL_FAILURE");
            testResults.put("bedrock_integration", "ACTIVE");
            
        } catch (Exception e) {
            testResults.put("pipeline_status", "FAILED");
            testResults.put("error_message", e.getMessage());
        }
        
        return ResponseEntity.ok(testResults);
    }

    // Request/Response DTOs
    public static class ThreatAnalysisRequest {
        private String userId;
        private String location;
        private String audioData;
        private String motionData;
        private String environmentalData;
        private String biometricData;
        private String additionalContext;
        private boolean enhancedConfidenceScoring = false;
        private boolean advancedClassification = false;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getAudioData() { return audioData; }
        public void setAudioData(String audioData) { this.audioData = audioData; }
        public String getMotionData() { return motionData; }
        public void setMotionData(String motionData) { this.motionData = motionData; }
        public String getEnvironmentalData() { return environmentalData; }
        public void setEnvironmentalData(String environmentalData) { this.environmentalData = environmentalData; }
        public String getBiometricData() { return biometricData; }
        public void setBiometricData(String biometricData) { this.biometricData = biometricData; }
        public String getAdditionalContext() { return additionalContext; }
        public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
        public boolean isEnhancedConfidenceScoring() { return enhancedConfidenceScoring; }
        public void setEnhancedConfidenceScoring(boolean enhancedConfidenceScoring) { this.enhancedConfidenceScoring = enhancedConfidenceScoring; }
        public boolean isAdvancedClassification() { return advancedClassification; }
        public void setAdvancedClassification(boolean advancedClassification) { this.advancedClassification = advancedClassification; }
    }

    public static class ThreatAnalysisResponse {
        private ThreatAssessment threatAssessment;
        private AutonomousConfidenceScoringService.ConfidenceAnalysisResult confidenceAnalysis;
        private ReasoningBasedThreatClassificationService.ThreatClassificationResult classificationResult;
        private boolean analysisComplete;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ThreatAnalysisResponse response = new ThreatAnalysisResponse();
            public Builder threatAssessment(ThreatAssessment threatAssessment) { response.threatAssessment = threatAssessment; return this; }
            public Builder confidenceAnalysis(AutonomousConfidenceScoringService.ConfidenceAnalysisResult confidenceAnalysis) { response.confidenceAnalysis = confidenceAnalysis; return this; }
            public Builder classificationResult(ReasoningBasedThreatClassificationService.ThreatClassificationResult classificationResult) { response.classificationResult = classificationResult; return this; }
            public Builder analysisComplete(boolean analysisComplete) { response.analysisComplete = analysisComplete; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public ThreatAnalysisResponse build() { return response; }
        }

        // Getters
        public ThreatAssessment getThreatAssessment() { return threatAssessment; }
        public AutonomousConfidenceScoringService.ConfidenceAnalysisResult getConfidenceAnalysis() { return confidenceAnalysis; }
        public ReasoningBasedThreatClassificationService.ThreatClassificationResult getClassificationResult() { return classificationResult; }
        public boolean isAnalysisComplete() { return analysisComplete; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class QuickAssessmentRequest {
        private String userId;
        private String location;
        private String audioData;
        private String motionData;
        private String environmentalData;
        private String biometricData;
        private String context;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getAudioData() { return audioData; }
        public void setAudioData(String audioData) { this.audioData = audioData; }
        public String getMotionData() { return motionData; }
        public void setMotionData(String motionData) { this.motionData = motionData; }
        public String getEnvironmentalData() { return environmentalData; }
        public void setEnvironmentalData(String environmentalData) { this.environmentalData = environmentalData; }
        public String getBiometricData() { return biometricData; }
        public void setBiometricData(String biometricData) { this.biometricData = biometricData; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }

    public static class QuickThreatAssessmentResponse {
        private String assessmentId;
        private String threatLevel;
        private double confidenceScore;
        private String recommendedAction;
        private boolean requiresEmergencyResponse;
        private String llmReasoning;
        private Long processingTimeMs;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private QuickThreatAssessmentResponse response = new QuickThreatAssessmentResponse();
            public Builder assessmentId(String assessmentId) { response.assessmentId = assessmentId; return this; }
            public Builder threatLevel(String threatLevel) { response.threatLevel = threatLevel; return this; }
            public Builder confidenceScore(double confidenceScore) { response.confidenceScore = confidenceScore; return this; }
            public Builder recommendedAction(String recommendedAction) { response.recommendedAction = recommendedAction; return this; }
            public Builder requiresEmergencyResponse(boolean requiresEmergencyResponse) { response.requiresEmergencyResponse = requiresEmergencyResponse; return this; }
            public Builder llmReasoning(String llmReasoning) { response.llmReasoning = llmReasoning; return this; }
            public Builder processingTimeMs(Long processingTimeMs) { response.processingTimeMs = processingTimeMs; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public QuickThreatAssessmentResponse build() { return response; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getThreatLevel() { return threatLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getRecommendedAction() { return recommendedAction; }
        public boolean isRequiresEmergencyResponse() { return requiresEmergencyResponse; }
        public String getLlmReasoning() { return llmReasoning; }
        public Long getProcessingTimeMs() { return processingTimeMs; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ConfidenceValidationRequest {
        private String assessmentId;
        private String userId;
        private String threatLevel;
        private double confidenceScore;
        private String sensorDataQuality;
        private String dataConsistency;
        private String environmentalFactors;
        private String historicalContext;

        // Getters and Setters
        public String getAssessmentId() { return assessmentId; }
        public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getThreatLevel() { return threatLevel; }
        public void setThreatLevel(String threatLevel) { this.threatLevel = threatLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getSensorDataQuality() { return sensorDataQuality; }
        public void setSensorDataQuality(String sensorDataQuality) { this.sensorDataQuality = sensorDataQuality; }
        public String getDataConsistency() { return dataConsistency; }
        public void setDataConsistency(String dataConsistency) { this.dataConsistency = dataConsistency; }
        public String getEnvironmentalFactors() { return environmentalFactors; }
        public void setEnvironmentalFactors(String environmentalFactors) { this.environmentalFactors = environmentalFactors; }
        public String getHistoricalContext() { return historicalContext; }
        public void setHistoricalContext(String historicalContext) { this.historicalContext = historicalContext; }
    }
}