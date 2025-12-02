package com.allsenses.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reasoning-Based Threat Level Classification Service for AllSenses AI Guardian
 * 
 * This service uses AWS Bedrock LLMs to perform sophisticated threat level classification
 * based on multi-factor reasoning. It demonstrates autonomous AI agent capabilities by
 * using LLM reasoning to classify threats across multiple dimensions and contexts.
 */
@Service
public class ReasoningBasedThreatClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(ReasoningBasedThreatClassificationService.class);

    @Autowired
    private BedrockLlmService bedrockLlmService;

    /**
     * Perform reasoning-based threat classification using LLM analysis
     * 
     * @param classificationInput Input data for threat classification
     * @return Comprehensive threat classification result
     */
    public ThreatClassificationResult performReasoningBasedClassification(
            ThreatClassificationInput classificationInput) {
        
        logger.info("Starting reasoning-based threat classification for assessment: {}", 
                   classificationInput.getAssessmentId());
        
        try {
            // Step 1: Multi-dimensional threat analysis
            MultiDimensionalAnalysis multiAnalysis = performMultiDimensionalAnalysis(classificationInput);
            
            // Step 2: Contextual threat evaluation
            ContextualEvaluation contextualEval = performContextualEvaluation(classificationInput);
            
            // Step 3: Temporal threat assessment
            TemporalAssessment temporalAssess = performTemporalAssessment(classificationInput);
            
            // Step 4: Integrated threat classification
            IntegratedClassification finalClassification = performIntegratedClassification(
                multiAnalysis, contextualEval, temporalAssess, classificationInput);
            
            // Step 5: Validation and confidence assessment
            ClassificationValidation validation = validateClassification(finalClassification, classificationInput);
            
            ThreatClassificationResult result = ThreatClassificationResult.builder()
                    .assessmentId(classificationInput.getAssessmentId())
                    .finalThreatLevel(finalClassification.threatLevel)
                    .finalConfidenceScore(finalClassification.confidenceScore)
                    .multiDimensionalAnalysis(multiAnalysis)
                    .contextualEvaluation(contextualEval)
                    .temporalAssessment(temporalAssess)
                    .integratedClassification(finalClassification)
                    .classificationValidation(validation)
                    .classificationReasoning(finalClassification.reasoning)
                    .success(true)
                    .build();
            
            logger.info("Reasoning-based classification completed. Final Level: {}, Confidence: {}", 
                       result.getFinalThreatLevel(), result.getFinalConfidenceScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during reasoning-based threat classification", e);
            return createFailedClassification(classificationInput, "Classification error: " + e.getMessage());
        }
    }

    /**
     * Perform multi-dimensional threat analysis
     */
    private MultiDimensionalAnalysis performMultiDimensionalAnalysis(ThreatClassificationInput input) {
        String prompt = buildMultiDimensionalAnalysisPrompt(input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultMultiDimensionalAnalysis();
        }
        
        return parseMultiDimensionalAnalysis(llmResponse.getResponse());
    }

    /**
     * Perform contextual threat evaluation
     */
    private ContextualEvaluation performContextualEvaluation(ThreatClassificationInput input) {
        String prompt = buildContextualEvaluationPrompt(input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultContextualEvaluation();
        }
        
        return parseContextualEvaluation(llmResponse.getResponse());
    }

    /**
     * Perform temporal threat assessment
     */
    private TemporalAssessment performTemporalAssessment(ThreatClassificationInput input) {
        String prompt = buildTemporalAssessmentPrompt(input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultTemporalAssessment();
        }
        
        return parseTemporalAssessment(llmResponse.getResponse());
    }

    /**
     * Perform integrated threat classification
     */
    private IntegratedClassification performIntegratedClassification(
            MultiDimensionalAnalysis multiAnalysis,
            ContextualEvaluation contextualEval,
            TemporalAssessment temporalAssess,
            ThreatClassificationInput input) {
        
        String prompt = buildIntegratedClassificationPrompt(
            multiAnalysis, contextualEval, temporalAssess, input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultIntegratedClassification();
        }
        
        return parseIntegratedClassification(llmResponse.getResponse());
    }

    /**
     * Validate the final classification
     */
    private ClassificationValidation validateClassification(
            IntegratedClassification classification, 
            ThreatClassificationInput input) {
        
        String prompt = buildValidationPrompt(classification, input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultValidation();
        }
        
        return parseValidationResult(llmResponse.getResponse());
    }

    /**
     * Build multi-dimensional analysis prompt
     */
    private String buildMultiDimensionalAnalysisPrompt(ThreatClassificationInput input) {
        return String.format("""
            Perform multi-dimensional threat analysis for the following data:
            
            SENSOR DATA:
            - Audio: %s
            - Motion: %s
            - Environmental: %s
            - Biometric: %s
            
            CONTEXT:
            - Location: %s
            - Time Context: %s
            - User Profile: %s
            
            Analyze across these dimensions:
            1. PHYSICAL_THREAT: Physical danger indicators
            2. BEHAVIORAL_ANOMALY: Unusual behavior patterns
            3. ENVIRONMENTAL_RISK: Environmental danger factors
            4. MEDICAL_EMERGENCY: Health-related emergency indicators
            5. SECURITY_THREAT: Security or safety concerns
            
            For each dimension, provide:
            DIMENSION_NAME: [NONE/LOW/MEDIUM/HIGH/CRITICAL]
            EVIDENCE: [Supporting evidence]
            CONFIDENCE: [0.0-1.0]
            
            Format your response with clear dimension labels.
            """, 
            input.getAudioData(), input.getMotionData(), input.getEnvironmentalData(),
            input.getBiometricData(), input.getLocation(), input.getTimeContext(), input.getUserProfile());
    }

    /**
     * Build contextual evaluation prompt
     */
    private String buildContextualEvaluationPrompt(ThreatClassificationInput input) {
        return String.format("""
            Evaluate threat context for comprehensive assessment:
            
            LOCATION CONTEXT: %s
            TIME CONTEXT: %s
            USER CONTEXT: %s
            HISTORICAL CONTEXT: %s
            
            Evaluate these contextual factors:
            1. LOCATION_RISK: Is this a high-risk location?
            2. TIME_RISK: Is this a high-risk time period?
            3. USER_VULNERABILITY: Is the user in a vulnerable state?
            4. SITUATIONAL_FACTORS: What situational factors increase/decrease risk?
            5. ESCALATION_POTENTIAL: How likely is the situation to escalate?
            
            Provide:
            CONTEXT_RISK_LEVEL: [NONE/LOW/MEDIUM/HIGH/CRITICAL]
            RISK_FACTORS: [List key risk factors]
            PROTECTIVE_FACTORS: [List factors that reduce risk]
            CONTEXT_CONFIDENCE: [0.0-1.0]
            """, 
            input.getLocation(), input.getTimeContext(), input.getUserProfile(), input.getHistoricalContext());
    }

    /**
     * Build temporal assessment prompt
     */
    private String buildTemporalAssessmentPrompt(ThreatClassificationInput input) {
        return String.format("""
            Analyze temporal aspects of the threat:
            
            CURRENT DATA: %s
            HISTORICAL PATTERNS: %s
            TREND ANALYSIS: %s
            
            Assess:
            1. IMMEDIACY: How immediate is the threat?
            2. PERSISTENCE: Is this a persistent or transient threat?
            3. ESCALATION_RATE: How quickly might this escalate?
            4. INTERVENTION_WINDOW: How much time for intervention?
            5. HISTORICAL_CORRELATION: Does this match known patterns?
            
            Provide:
            TEMPORAL_URGENCY: [NONE/LOW/MEDIUM/HIGH/CRITICAL]
            TIME_TO_ESCALATION: [IMMEDIATE/MINUTES/HOURS/DAYS]
            INTERVENTION_PRIORITY: [LOW/MEDIUM/HIGH/CRITICAL]
            TEMPORAL_CONFIDENCE: [0.0-1.0]
            """, 
            input.getCurrentSensorData(), input.getHistoricalContext(), input.getTrendData());
    }

    /**
     * Build integrated classification prompt
     */
    private String buildIntegratedClassificationPrompt(
            MultiDimensionalAnalysis multiAnalysis,
            ContextualEvaluation contextualEval,
            TemporalAssessment temporalAssess,
            ThreatClassificationInput input) {
        
        return String.format("""
            Integrate all analyses to determine final threat classification:
            
            MULTI-DIMENSIONAL ANALYSIS:
            - Physical Threat: %s
            - Behavioral Anomaly: %s
            - Environmental Risk: %s
            - Medical Emergency: %s
            - Security Threat: %s
            
            CONTEXTUAL EVALUATION:
            - Context Risk Level: %s
            - Risk Factors: %s
            - Protective Factors: %s
            
            TEMPORAL ASSESSMENT:
            - Temporal Urgency: %s
            - Time to Escalation: %s
            - Intervention Priority: %s
            
            Based on this comprehensive analysis, determine:
            FINAL_THREAT_LEVEL: [NONE/LOW/MEDIUM/HIGH/CRITICAL]
            FINAL_CONFIDENCE: [0.0-1.0]
            PRIMARY_THREAT_TYPE: [PHYSICAL/MEDICAL/SECURITY/BEHAVIORAL/ENVIRONMENTAL]
            SECONDARY_FACTORS: [List secondary contributing factors]
            RECOMMENDED_RESPONSE: [Specific response recommendation]
            CLASSIFICATION_REASONING: [Detailed reasoning for the classification]
            """, 
            multiAnalysis.physicalThreat, multiAnalysis.behavioralAnomaly, 
            multiAnalysis.environmentalRisk, multiAnalysis.medicalEmergency, multiAnalysis.securityThreat,
            contextualEval.contextRiskLevel, contextualEval.riskFactors, contextualEval.protectiveFactors,
            temporalAssess.temporalUrgency, temporalAssess.timeToEscalation, temporalAssess.interventionPriority);
    }

    /**
     * Build validation prompt
     */
    private String buildValidationPrompt(IntegratedClassification classification, ThreatClassificationInput input) {
        return String.format("""
            Validate the threat classification result:
            
            CLASSIFICATION RESULT:
            - Threat Level: %s
            - Confidence: %.2f
            - Primary Type: %s
            - Reasoning: %s
            
            VALIDATION CHECKS:
            1. Is the threat level consistent with the evidence?
            2. Is the confidence score appropriate?
            3. Are there any contradictions in the analysis?
            4. Does the classification match expected patterns?
            5. Are there any missing considerations?
            
            Provide:
            VALIDATION_RESULT: [VALID/INVALID/NEEDS_REVIEW]
            VALIDATION_ISSUES: [List any issues found]
            CONFIDENCE_IN_VALIDATION: [0.0-1.0]
            RECOMMENDED_ADJUSTMENTS: [Any recommended changes]
            """, 
            classification.threatLevel, classification.confidenceScore, 
            classification.primaryThreatType, classification.reasoning);
    }

    /**
     * Test reasoning-based classification with sample data
     */
    public ThreatClassificationResult testReasoningBasedClassification() {
        ThreatClassificationInput testInput = ThreatClassificationInput.builder()
                .assessmentId("TEST-CLASSIFICATION-001")
                .audioData("Elevated voice levels, distress sounds, background noise suggesting struggle")
                .motionData("Rapid, erratic movement patterns, possible physical altercation")
                .environmentalData("Isolated location, poor lighting, no witnesses nearby")
                .biometricData("Heart rate 140 BPM, stress indicators elevated")
                .location("Parking garage, downtown area, known for incidents")
                .timeContext("Late evening, 11:30 PM, weekend")
                .userProfile("Young adult, no previous incidents, traveling alone")
                .historicalContext("3 similar incidents in this area in past month")
                .currentSensorData("All sensors showing anomalous readings")
                .trendData("Escalating pattern over last 5 minutes")
                .build();
        
        return performReasoningBasedClassification(testInput);
    }

    // Parsing and default creation methods (simplified for MVP)
    private MultiDimensionalAnalysis parseMultiDimensionalAnalysis(String response) {
        MultiDimensionalAnalysis analysis = new MultiDimensionalAnalysis();
        analysis.physicalThreat = extractThreatLevel(response, "PHYSICAL_THREAT:", "MEDIUM");
        analysis.behavioralAnomaly = extractThreatLevel(response, "BEHAVIORAL_ANOMALY:", "MEDIUM");
        analysis.environmentalRisk = extractThreatLevel(response, "ENVIRONMENTAL_RISK:", "MEDIUM");
        analysis.medicalEmergency = extractThreatLevel(response, "MEDICAL_EMERGENCY:", "LOW");
        analysis.securityThreat = extractThreatLevel(response, "SECURITY_THREAT:", "MEDIUM");
        return analysis;
    }

    private ContextualEvaluation parseContextualEvaluation(String response) {
        ContextualEvaluation eval = new ContextualEvaluation();
        eval.contextRiskLevel = extractThreatLevel(response, "CONTEXT_RISK_LEVEL:", "MEDIUM");
        eval.riskFactors = extractSection(response, "RISK_FACTORS:", "Multiple risk factors identified");
        eval.protectiveFactors = extractSection(response, "PROTECTIVE_FACTORS:", "Limited protective factors");
        return eval;
    }

    private TemporalAssessment parseTemporalAssessment(String response) {
        TemporalAssessment assessment = new TemporalAssessment();
        assessment.temporalUrgency = extractThreatLevel(response, "TEMPORAL_URGENCY:", "HIGH");
        assessment.timeToEscalation = extractSection(response, "TIME_TO_ESCALATION:", "MINUTES");
        assessment.interventionPriority = extractThreatLevel(response, "INTERVENTION_PRIORITY:", "HIGH");
        return assessment;
    }

    private IntegratedClassification parseIntegratedClassification(String response) {
        IntegratedClassification classification = new IntegratedClassification();
        classification.threatLevel = extractThreatLevel(response, "FINAL_THREAT_LEVEL:", "HIGH");
        classification.confidenceScore = extractConfidenceScore(response, "FINAL_CONFIDENCE:", 0.75);
        classification.primaryThreatType = extractSection(response, "PRIMARY_THREAT_TYPE:", "PHYSICAL");
        classification.reasoning = extractSection(response, "CLASSIFICATION_REASONING:", "Comprehensive analysis indicates high threat level");
        return classification;
    }

    private ClassificationValidation parseValidationResult(String response) {
        ClassificationValidation validation = new ClassificationValidation();
        validation.validationResult = extractSection(response, "VALIDATION_RESULT:", "VALID");
        validation.validationIssues = extractSection(response, "VALIDATION_ISSUES:", "No significant issues");
        validation.confidenceInValidation = extractConfidenceScore(response, "CONFIDENCE_IN_VALIDATION:", 0.8);
        return validation;
    }

    // Utility methods
    private String extractThreatLevel(String response, String pattern, String defaultValue) {
        return extractSection(response, pattern, defaultValue);
    }

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

    private double extractConfidenceScore(String response, String pattern, double defaultValue) {
        try {
            String scoreStr = extractSection(response, pattern, String.valueOf(defaultValue));
            return Double.parseDouble(scoreStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Default creation methods
    private MultiDimensionalAnalysis createDefaultMultiDimensionalAnalysis() {
        MultiDimensionalAnalysis analysis = new MultiDimensionalAnalysis();
        analysis.physicalThreat = "MEDIUM";
        analysis.behavioralAnomaly = "MEDIUM";
        analysis.environmentalRisk = "MEDIUM";
        analysis.medicalEmergency = "LOW";
        analysis.securityThreat = "MEDIUM";
        return analysis;
    }

    private ContextualEvaluation createDefaultContextualEvaluation() {
        ContextualEvaluation eval = new ContextualEvaluation();
        eval.contextRiskLevel = "MEDIUM";
        eval.riskFactors = "Default risk assessment";
        eval.protectiveFactors = "Standard protective factors";
        return eval;
    }

    private TemporalAssessment createDefaultTemporalAssessment() {
        TemporalAssessment assessment = new TemporalAssessment();
        assessment.temporalUrgency = "MEDIUM";
        assessment.timeToEscalation = "MINUTES";
        assessment.interventionPriority = "MEDIUM";
        return assessment;
    }

    private IntegratedClassification createDefaultIntegratedClassification() {
        IntegratedClassification classification = new IntegratedClassification();
        classification.threatLevel = "MEDIUM";
        classification.confidenceScore = 0.6;
        classification.primaryThreatType = "GENERAL";
        classification.reasoning = "Default classification due to analysis failure";
        return classification;
    }

    private ClassificationValidation createDefaultValidation() {
        ClassificationValidation validation = new ClassificationValidation();
        validation.validationResult = "NEEDS_REVIEW";
        validation.validationIssues = "Unable to validate due to analysis failure";
        validation.confidenceInValidation = 0.3;
        return validation;
    }

    private ThreatClassificationResult createFailedClassification(ThreatClassificationInput input, String errorMessage) {
        return ThreatClassificationResult.builder()
                .assessmentId(input.getAssessmentId())
                .finalThreatLevel("NONE")
                .finalConfidenceScore(0.0)
                .classificationReasoning("Classification failed: " + errorMessage)
                .success(false)
                .build();
    }

    // Data classes (simplified for MVP)
    public static class ThreatClassificationInput {
        private String assessmentId;
        private String audioData;
        private String motionData;
        private String environmentalData;
        private String biometricData;
        private String location;
        private String timeContext;
        private String userProfile;
        private String historicalContext;
        private String currentSensorData;
        private String trendData;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ThreatClassificationInput input = new ThreatClassificationInput();
            public Builder assessmentId(String assessmentId) { input.assessmentId = assessmentId; return this; }
            public Builder audioData(String audioData) { input.audioData = audioData; return this; }
            public Builder motionData(String motionData) { input.motionData = motionData; return this; }
            public Builder environmentalData(String environmentalData) { input.environmentalData = environmentalData; return this; }
            public Builder biometricData(String biometricData) { input.biometricData = biometricData; return this; }
            public Builder location(String location) { input.location = location; return this; }
            public Builder timeContext(String timeContext) { input.timeContext = timeContext; return this; }
            public Builder userProfile(String userProfile) { input.userProfile = userProfile; return this; }
            public Builder historicalContext(String historicalContext) { input.historicalContext = historicalContext; return this; }
            public Builder currentSensorData(String currentSensorData) { input.currentSensorData = currentSensorData; return this; }
            public Builder trendData(String trendData) { input.trendData = trendData; return this; }
            public ThreatClassificationInput build() { return input; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getAudioData() { return audioData; }
        public String getMotionData() { return motionData; }
        public String getEnvironmentalData() { return environmentalData; }
        public String getBiometricData() { return biometricData; }
        public String getLocation() { return location; }
        public String getTimeContext() { return timeContext; }
        public String getUserProfile() { return userProfile; }
        public String getHistoricalContext() { return historicalContext; }
        public String getCurrentSensorData() { return currentSensorData; }
        public String getTrendData() { return trendData; }
    }

    public static class MultiDimensionalAnalysis {
        public String physicalThreat;
        public String behavioralAnomaly;
        public String environmentalRisk;
        public String medicalEmergency;
        public String securityThreat;
    }

    public static class ContextualEvaluation {
        public String contextRiskLevel;
        public String riskFactors;
        public String protectiveFactors;
    }

    public static class TemporalAssessment {
        public String temporalUrgency;
        public String timeToEscalation;
        public String interventionPriority;
    }

    public static class IntegratedClassification {
        public String threatLevel;
        public double confidenceScore;
        public String primaryThreatType;
        public String reasoning;
    }

    public static class ClassificationValidation {
        public String validationResult;
        public String validationIssues;
        public double confidenceInValidation;
    }

    public static class ThreatClassificationResult {
        private String assessmentId;
        private String finalThreatLevel;
        private double finalConfidenceScore;
        private MultiDimensionalAnalysis multiDimensionalAnalysis;
        private ContextualEvaluation contextualEvaluation;
        private TemporalAssessment temporalAssessment;
        private IntegratedClassification integratedClassification;
        private ClassificationValidation classificationValidation;
        private String classificationReasoning;
        private boolean success;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ThreatClassificationResult result = new ThreatClassificationResult();
            public Builder assessmentId(String assessmentId) { result.assessmentId = assessmentId; return this; }
            public Builder finalThreatLevel(String finalThreatLevel) { result.finalThreatLevel = finalThreatLevel; return this; }
            public Builder finalConfidenceScore(double finalConfidenceScore) { result.finalConfidenceScore = finalConfidenceScore; return this; }
            public Builder multiDimensionalAnalysis(MultiDimensionalAnalysis multiDimensionalAnalysis) { result.multiDimensionalAnalysis = multiDimensionalAnalysis; return this; }
            public Builder contextualEvaluation(ContextualEvaluation contextualEvaluation) { result.contextualEvaluation = contextualEvaluation; return this; }
            public Builder temporalAssessment(TemporalAssessment temporalAssessment) { result.temporalAssessment = temporalAssessment; return this; }
            public Builder integratedClassification(IntegratedClassification integratedClassification) { result.integratedClassification = integratedClassification; return this; }
            public Builder classificationValidation(ClassificationValidation classificationValidation) { result.classificationValidation = classificationValidation; return this; }
            public Builder classificationReasoning(String classificationReasoning) { result.classificationReasoning = classificationReasoning; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public ThreatClassificationResult build() { return result; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getFinalThreatLevel() { return finalThreatLevel; }
        public double getFinalConfidenceScore() { return finalConfidenceScore; }
        public MultiDimensionalAnalysis getMultiDimensionalAnalysis() { return multiDimensionalAnalysis; }
        public ContextualEvaluation getContextualEvaluation() { return contextualEvaluation; }
        public TemporalAssessment getTemporalAssessment() { return temporalAssessment; }
        public IntegratedClassification getIntegratedClassification() { return integratedClassification; }
        public ClassificationValidation getClassificationValidation() { return classificationValidation; }
        public String getClassificationReasoning() { return classificationReasoning; }
        public boolean isSuccess() { return success; }
    }
}