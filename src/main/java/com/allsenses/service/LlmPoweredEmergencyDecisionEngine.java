package com.allsenses.service;

import com.allsenses.model.EmergencyEvent;
import com.allsenses.model.ThreatAssessment;
import com.allsenses.repository.EmergencyEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * LLM-Powered Emergency Decision Engine for AllSenses AI Guardian
 * 
 * This service uses AWS Bedrock LLMs to make autonomous emergency response decisions.
 * It demonstrates all three AWS AI Agent qualification conditions by using
 * reasoning LLMs for autonomous emergency decision-making with database integration.
 */
@Service
public class LlmPoweredEmergencyDecisionEngine {

    private static final Logger logger = LoggerFactory.getLogger(LlmPoweredEmergencyDecisionEngine.class);

    @Autowired
    private BedrockLlmService bedrockLlmService;

    @Autowired
    private EmergencyEventRepository emergencyEventRepository;

    /**
     * Make autonomous emergency response decision using LLM reasoning
     * 
     * @param decisionInput Input data for emergency decision
     * @return Comprehensive emergency decision result
     */
    public EmergencyDecisionResult makeAutonomousEmergencyDecision(EmergencyDecisionInput decisionInput) {
        logger.info("Starting autonomous emergency decision for assessment: {}", decisionInput.getAssessmentId());
        
        try {
            // Step 1: Emergency situation analysis
            EmergencySituationAnalysis situationAnalysis = analyzeEmergencySituation(decisionInput);
            
            // Step 2: Response priority determination
            ResponsePriorityAssessment priorityAssessment = determineResponsePriority(decisionInput, situationAnalysis);
            
            // Step 3: Resource allocation decision
            ResourceAllocationDecision resourceDecision = determineResourceAllocation(decisionInput, priorityAssessment);
            
            // Step 4: Communication strategy
            CommunicationStrategy commStrategy = determineCommunicationStrategy(decisionInput, priorityAssessment);
            
            // Step 5: Integrated emergency decision
            IntegratedEmergencyDecision finalDecision = makeIntegratedEmergencyDecision(
                situationAnalysis, priorityAssessment, resourceDecision, commStrategy, decisionInput);
            
            // Step 6: Create and persist emergency event
            EmergencyEvent emergencyEvent = createEmergencyEvent(finalDecision, decisionInput);
            EmergencyEvent savedEvent = emergencyEventRepository.save(emergencyEvent);
            
            EmergencyDecisionResult result = EmergencyDecisionResult.builder()
                    .decisionId(generateDecisionId())
                    .assessmentId(decisionInput.getAssessmentId())
                    .emergencyEventId(savedEvent.getEventId())
                    .situationAnalysis(situationAnalysis)
                    .priorityAssessment(priorityAssessment)
                    .resourceDecision(resourceDecision)
                    .communicationStrategy(commStrategy)
                    .finalDecision(finalDecision)
                    .emergencyEvent(savedEvent)
                    .decisionTimestamp(Instant.now())
                    .success(true)
                    .build();
            
            logger.info("Autonomous emergency decision completed. Event ID: {}, Priority: {}, Response: {}", 
                       savedEvent.getEventId(), finalDecision.priorityLevel, finalDecision.responseType);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during autonomous emergency decision", e);
            return createFailedEmergencyDecision(decisionInput, "Emergency decision error: " + e.getMessage());
        }
    }

    /**
     * Analyze emergency situation using LLM reasoning
     */
    private EmergencySituationAnalysis analyzeEmergencySituation(EmergencyDecisionInput input) {
        String prompt = buildSituationAnalysisPrompt(input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultSituationAnalysis();
        }
        
        return parseSituationAnalysis(llmResponse.getResponse());
    }

    /**
     * Determine response priority using LLM reasoning
     */
    private ResponsePriorityAssessment determineResponsePriority(
            EmergencyDecisionInput input, EmergencySituationAnalysis situationAnalysis) {
        
        String prompt = buildPriorityAssessmentPrompt(input, situationAnalysis);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultPriorityAssessment();
        }
        
        return parsePriorityAssessment(llmResponse.getResponse());
    }

    /**
     * Determine resource allocation using LLM reasoning
     */
    private ResourceAllocationDecision determineResourceAllocation(
            EmergencyDecisionInput input, ResponsePriorityAssessment priorityAssessment) {
        
        String prompt = buildResourceAllocationPrompt(input, priorityAssessment);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultResourceAllocation();
        }
        
        return parseResourceAllocation(llmResponse.getResponse());
    }

    /**
     * Determine communication strategy using LLM reasoning
     */
    private CommunicationStrategy determineCommunicationStrategy(
            EmergencyDecisionInput input, ResponsePriorityAssessment priorityAssessment) {
        
        String prompt = buildCommunicationStrategyPrompt(input, priorityAssessment);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultCommunicationStrategy();
        }
        
        return parseCommunicationStrategy(llmResponse.getResponse());
    }

    /**
     * Make integrated emergency decision
     */
    private IntegratedEmergencyDecision makeIntegratedEmergencyDecision(
            EmergencySituationAnalysis situationAnalysis,
            ResponsePriorityAssessment priorityAssessment,
            ResourceAllocationDecision resourceDecision,
            CommunicationStrategy commStrategy,
            EmergencyDecisionInput input) {
        
        String prompt = buildIntegratedDecisionPrompt(
            situationAnalysis, priorityAssessment, resourceDecision, commStrategy, input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultIntegratedDecision();
        }
        
        return parseIntegratedDecision(llmResponse.getResponse());
    }

    /**
     * Build situation analysis prompt
     */
    private String buildSituationAnalysisPrompt(EmergencyDecisionInput input) {
        return String.format("""
            Analyze the emergency situation for autonomous response decision:
            
            THREAT ASSESSMENT:
            - Assessment ID: %s
            - Threat Level: %s
            - Confidence Score: %.2f
            - LLM Reasoning: %s
            
            CONTEXT INFORMATION:
            - User ID: %s
            - Location: %s
            - Time Context: %s
            - Environmental Factors: %s
            
            SENSOR DATA:
            - Audio Data: %s
            - Motion Data: %s
            - Biometric Data: %s
            
            Analyze the situation across these dimensions:
            1. IMMEDIATE_DANGER: Is there immediate physical danger?
            2. MEDICAL_EMERGENCY: Are there medical emergency indicators?
            3. SECURITY_THREAT: Is this a security or safety threat?
            4. ENVIRONMENTAL_HAZARD: Are there environmental dangers?
            5. ESCALATION_POTENTIAL: How likely is escalation?
            
            For each dimension, provide:
            DIMENSION_ASSESSMENT: [NONE/LOW/MEDIUM/HIGH/CRITICAL]
            EVIDENCE: [Supporting evidence from data]
            URGENCY_LEVEL: [0-10 scale]
            
            Also provide:
            OVERALL_SITUATION_SEVERITY: [NONE/LOW/MEDIUM/HIGH/CRITICAL]
            PRIMARY_EMERGENCY_TYPE: [MEDICAL/PHYSICAL/SECURITY/ENVIRONMENTAL/UNKNOWN]
            SITUATION_CONFIDENCE: [0.0-1.0]
            """, 
            input.getAssessmentId(), input.getThreatLevel(), input.getConfidenceScore(), input.getLlmReasoning(),
            input.getUserId(), input.getLocation(), input.getTimeContext(), input.getEnvironmentalFactors(),
            input.getAudioData(), input.getMotionData(), input.getBiometricData());
    }

    /**
     * Build priority assessment prompt
     */
    private String buildPriorityAssessmentPrompt(EmergencyDecisionInput input, EmergencySituationAnalysis analysis) {
        return String.format("""
            Determine emergency response priority based on situation analysis:
            
            SITUATION ANALYSIS RESULTS:
            - Overall Severity: %s
            - Primary Emergency Type: %s
            - Immediate Danger: %s
            - Medical Emergency: %s
            - Security Threat: %s
            - Escalation Potential: %s
            
            PRIORITY FACTORS TO CONSIDER:
            1. Life-threatening situations = CRITICAL priority
            2. Immediate physical danger = HIGH priority
            3. Medical emergencies = HIGH priority
            4. Security threats = MEDIUM-HIGH priority
            5. Environmental hazards = MEDIUM priority
            
            RESPONSE TIME REQUIREMENTS:
            - CRITICAL: Immediate response (0-30 seconds)
            - HIGH: Urgent response (30 seconds - 2 minutes)
            - MEDIUM: Priority response (2-5 minutes)
            - LOW: Standard response (5-15 minutes)
            
            Determine:
            RESPONSE_PRIORITY: [CRITICAL/HIGH/MEDIUM/LOW]
            RESPONSE_TIME_TARGET: [seconds]
            JUSTIFICATION: [Explain priority level reasoning]
            AUTONOMOUS_ACTION_REQUIRED: [YES/NO]
            HUMAN_OVERSIGHT_NEEDED: [YES/NO]
            """, 
            analysis.overallSeverity, analysis.primaryEmergencyType, analysis.immediateDanger,
            analysis.medicalEmergency, analysis.securityThreat, analysis.escalationPotential);
    }

    /**
     * Build resource allocation prompt
     */
    private String buildResourceAllocationPrompt(EmergencyDecisionInput input, ResponsePriorityAssessment priority) {
        return String.format("""
            Determine resource allocation for emergency response:
            
            PRIORITY ASSESSMENT:
            - Response Priority: %s
            - Response Time Target: %s seconds
            - Autonomous Action Required: %s
            - Human Oversight Needed: %s
            
            AVAILABLE RESOURCES:
            1. Emergency Services (911/local emergency)
            2. Trusted Contacts (family/friends)
            3. Medical Services
            4. Security Services
            5. Environmental Response Teams
            
            RESOURCE ALLOCATION CRITERIA:
            - CRITICAL: All relevant resources immediately
            - HIGH: Primary resources + backup notification
            - MEDIUM: Primary resources only
            - LOW: Monitoring + trusted contacts
            
            Determine:
            EMERGENCY_SERVICES_CONTACT: [YES/NO]
            TRUSTED_CONTACTS_NOTIFY: [YES/NO]
            MEDICAL_SERVICES_ALERT: [YES/NO]
            SECURITY_SERVICES_ALERT: [YES/NO]
            RESOURCE_PRIORITY_ORDER: [List in order of contact]
            SIMULTANEOUS_CONTACT: [YES/NO]
            """, 
            priority.responsePriority, priority.responseTimeTarget, 
            priority.autonomousActionRequired, priority.humanOversightNeeded);
    }

    /**
     * Build communication strategy prompt
     */
    private String buildCommunicationStrategyPrompt(EmergencyDecisionInput input, ResponsePriorityAssessment priority) {
        return String.format("""
            Determine communication strategy for emergency response:
            
            EMERGENCY CONTEXT:
            - Priority Level: %s
            - Response Time: %s seconds
            - Location: %s
            - User Profile: %s
            
            COMMUNICATION CHANNELS:
            1. Voice Call (immediate, high reliability)
            2. SMS (fast, good reliability)
            3. Push Notification (instant, medium reliability)
            4. Email (delayed, high reliability)
            
            COMMUNICATION STRATEGY FACTORS:
            - CRITICAL: Multi-channel simultaneous
            - HIGH: Primary + backup channel
            - MEDIUM: Primary channel + confirmation
            - LOW: Single channel notification
            
            Determine:
            PRIMARY_COMMUNICATION_METHOD: [VOICE/SMS/PUSH/EMAIL]
            BACKUP_COMMUNICATION_METHOD: [VOICE/SMS/PUSH/EMAIL]
            MESSAGE_URGENCY_LEVEL: [CRITICAL/HIGH/MEDIUM/LOW]
            RETRY_STRATEGY: [IMMEDIATE/DELAYED/NONE]
            ESCALATION_PROTOCOL: [YES/NO]
            CONTEXT_SHARING_LEVEL: [FULL/PARTIAL/MINIMAL]
            """, 
            priority.responsePriority, priority.responseTimeTarget, input.getLocation(), input.getUserId());
    }

    /**
     * Build integrated decision prompt
     */
    private String buildIntegratedDecisionPrompt(
            EmergencySituationAnalysis situationAnalysis,
            ResponsePriorityAssessment priorityAssessment,
            ResourceAllocationDecision resourceDecision,
            CommunicationStrategy commStrategy,
            EmergencyDecisionInput input) {
        
        return String.format("""
            Make final integrated emergency response decision:
            
            SITUATION ANALYSIS:
            - Overall Severity: %s
            - Primary Type: %s
            - Situation Confidence: %s
            
            PRIORITY ASSESSMENT:
            - Response Priority: %s
            - Time Target: %s seconds
            - Autonomous Action: %s
            
            RESOURCE ALLOCATION:
            - Emergency Services: %s
            - Trusted Contacts: %s
            - Medical Services: %s
            
            COMMUNICATION STRATEGY:
            - Primary Method: %s
            - Message Urgency: %s
            - Escalation Protocol: %s
            
            Based on this comprehensive analysis, make the final decision:
            
            FINAL_RESPONSE_TYPE: [IMMEDIATE_EMERGENCY/URGENT_RESPONSE/PRIORITY_ALERT/MONITORING_ALERT/NO_ACTION]
            FINAL_PRIORITY_LEVEL: [CRITICAL/HIGH/MEDIUM/LOW]
            AUTONOMOUS_EXECUTION: [FULL/PARTIAL/SUPERVISED/MANUAL]
            EXPECTED_RESPONSE_TIME: [seconds]
            DECISION_CONFIDENCE: [0.0-1.0]
            DECISION_REASONING: [Comprehensive reasoning for the decision]
            FALLBACK_PLAN: [What to do if primary response fails]
            """, 
            situationAnalysis.overallSeverity, situationAnalysis.primaryEmergencyType, situationAnalysis.situationConfidence,
            priorityAssessment.responsePriority, priorityAssessment.responseTimeTarget, priorityAssessment.autonomousActionRequired,
            resourceDecision.emergencyServicesContact, resourceDecision.trustedContactsNotify, resourceDecision.medicalServicesAlert,
            commStrategy.primaryCommunicationMethod, commStrategy.messageUrgencyLevel, commStrategy.escalationProtocol);
    }

    /**
     * Create emergency event from decision
     */
    private EmergencyEvent createEmergencyEvent(IntegratedEmergencyDecision decision, EmergencyDecisionInput input) {
        EmergencyEvent event = new EmergencyEvent();
        
        event.setEventId(generateEventId());
        event.setUserId(input.getUserId());
        event.setAssessmentId(input.getAssessmentId());
        event.setEventStatus("INITIATED");
        event.setPriorityLevel(decision.priorityLevel);
        event.setAutonomousDecisionId(decision.decisionId);
        event.setLlmReasoningUsed(decision.decisionReasoning);
        
        // Set location if available
        if (input.getLocation() != null) {
            ThreatAssessment.LocationData location = new ThreatAssessment.LocationData();
            location.setAddress(input.getLocation());
            location.setLatitude(40.7128); // Mock coordinates for MVP
            location.setLongitude(-74.0060);
            event.setEventLocation(location);
        }
        
        // Set context data
        Map<String, String> contextData = new HashMap<>();
        contextData.put("threat_level", input.getThreatLevel());
        contextData.put("confidence_score", String.valueOf(input.getConfidenceScore()));
        contextData.put("response_type", decision.responseType);
        contextData.put("autonomous_execution", decision.autonomousExecution);
        event.setContextData(contextData.toString());
        
        return event;
    }

    /**
     * Test LLM-powered emergency decision with sample data
     */
    public EmergencyDecisionResult testLlmPoweredEmergencyDecision() {
        EmergencyDecisionInput testInput = EmergencyDecisionInput.builder()
                .assessmentId("TEST-EMERGENCY-DECISION-001")
                .userId("test-user-emergency")
                .threatLevel("HIGH")
                .confidenceScore(0.85)
                .llmReasoning("High threat detected with multiple indicators of physical danger")
                .location("Downtown parking garage, isolated area")
                .timeContext("Late evening, 11:45 PM")
                .environmentalFactors("Poor lighting, no witnesses, known high-crime area")
                .audioData("Distress sounds, elevated voices, sounds of struggle")
                .motionData("Rapid, erratic movement patterns suggesting physical altercation")
                .biometricData("Heart rate 145 BPM, extreme stress indicators")
                .build();
        
        return makeAutonomousEmergencyDecision(testInput);
    }

    // Parsing methods (simplified for MVP)
    private EmergencySituationAnalysis parseSituationAnalysis(String response) {
        EmergencySituationAnalysis analysis = new EmergencySituationAnalysis();
        analysis.overallSeverity = extractValue(response, "OVERALL_SITUATION_SEVERITY:", "HIGH");
        analysis.primaryEmergencyType = extractValue(response, "PRIMARY_EMERGENCY_TYPE:", "PHYSICAL");
        analysis.immediateDanger = extractValue(response, "IMMEDIATE_DANGER:", "HIGH");
        analysis.medicalEmergency = extractValue(response, "MEDICAL_EMERGENCY:", "MEDIUM");
        analysis.securityThreat = extractValue(response, "SECURITY_THREAT:", "HIGH");
        analysis.escalationPotential = extractValue(response, "ESCALATION_POTENTIAL:", "HIGH");
        analysis.situationConfidence = extractDoubleValue(response, "SITUATION_CONFIDENCE:", 0.8);
        return analysis;
    }

    private ResponsePriorityAssessment parsePriorityAssessment(String response) {
        ResponsePriorityAssessment assessment = new ResponsePriorityAssessment();
        assessment.responsePriority = extractValue(response, "RESPONSE_PRIORITY:", "HIGH");
        assessment.responseTimeTarget = extractIntValue(response, "RESPONSE_TIME_TARGET:", 60);
        assessment.justification = extractValue(response, "JUSTIFICATION:", "High priority emergency response required");
        assessment.autonomousActionRequired = extractBooleanValue(response, "AUTONOMOUS_ACTION_REQUIRED:", true);
        assessment.humanOversightNeeded = extractBooleanValue(response, "HUMAN_OVERSIGHT_NEEDED:", false);
        return assessment;
    }

    private ResourceAllocationDecision parseResourceAllocation(String response) {
        ResourceAllocationDecision decision = new ResourceAllocationDecision();
        decision.emergencyServicesContact = extractBooleanValue(response, "EMERGENCY_SERVICES_CONTACT:", true);
        decision.trustedContactsNotify = extractBooleanValue(response, "TRUSTED_CONTACTS_NOTIFY:", true);
        decision.medicalServicesAlert = extractBooleanValue(response, "MEDICAL_SERVICES_ALERT:", false);
        decision.securityServicesAlert = extractBooleanValue(response, "SECURITY_SERVICES_ALERT:", true);
        decision.simultaneousContact = extractBooleanValue(response, "SIMULTANEOUS_CONTACT:", true);
        return decision;
    }

    private CommunicationStrategy parseCommunicationStrategy(String response) {
        CommunicationStrategy strategy = new CommunicationStrategy();
        strategy.primaryCommunicationMethod = extractValue(response, "PRIMARY_COMMUNICATION_METHOD:", "VOICE");
        strategy.backupCommunicationMethod = extractValue(response, "BACKUP_COMMUNICATION_METHOD:", "SMS");
        strategy.messageUrgencyLevel = extractValue(response, "MESSAGE_URGENCY_LEVEL:", "HIGH");
        strategy.retryStrategy = extractValue(response, "RETRY_STRATEGY:", "IMMEDIATE");
        strategy.escalationProtocol = extractBooleanValue(response, "ESCALATION_PROTOCOL:", true);
        strategy.contextSharingLevel = extractValue(response, "CONTEXT_SHARING_LEVEL:", "FULL");
        return strategy;
    }

    private IntegratedEmergencyDecision parseIntegratedDecision(String response) {
        IntegratedEmergencyDecision decision = new IntegratedEmergencyDecision();
        decision.decisionId = generateDecisionId();
        decision.responseType = extractValue(response, "FINAL_RESPONSE_TYPE:", "IMMEDIATE_EMERGENCY");
        decision.priorityLevel = extractValue(response, "FINAL_PRIORITY_LEVEL:", "HIGH");
        decision.autonomousExecution = extractValue(response, "AUTONOMOUS_EXECUTION:", "FULL");
        decision.expectedResponseTime = extractIntValue(response, "EXPECTED_RESPONSE_TIME:", 60);
        decision.decisionConfidence = extractDoubleValue(response, "DECISION_CONFIDENCE:", 0.85);
        decision.decisionReasoning = extractValue(response, "DECISION_REASONING:", "Comprehensive emergency analysis indicates immediate response required");
        decision.fallbackPlan = extractValue(response, "FALLBACK_PLAN:", "Escalate to human oversight if primary response fails");
        return decision;
    }

    // Utility methods
    private String extractValue(String response, String pattern, String defaultValue) {
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

    private double extractDoubleValue(String response, String pattern, double defaultValue) {
        try {
            String value = extractValue(response, pattern, String.valueOf(defaultValue));
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int extractIntValue(String response, String pattern, int defaultValue) {
        try {
            String value = extractValue(response, pattern, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String response, String pattern, boolean defaultValue) {
        try {
            String value = extractValue(response, pattern, String.valueOf(defaultValue));
            return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String generateDecisionId() {
        return "DECISION-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private String generateEventId() {
        return "EVENT-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    // Default creation methods
    private EmergencySituationAnalysis createDefaultSituationAnalysis() {
        EmergencySituationAnalysis analysis = new EmergencySituationAnalysis();
        analysis.overallSeverity = "MEDIUM";
        analysis.primaryEmergencyType = "UNKNOWN";
        analysis.immediateDanger = "MEDIUM";
        analysis.medicalEmergency = "LOW";
        analysis.securityThreat = "MEDIUM";
        analysis.escalationPotential = "MEDIUM";
        analysis.situationConfidence = 0.6;
        return analysis;
    }

    private ResponsePriorityAssessment createDefaultPriorityAssessment() {
        ResponsePriorityAssessment assessment = new ResponsePriorityAssessment();
        assessment.responsePriority = "MEDIUM";
        assessment.responseTimeTarget = 120;
        assessment.justification = "Default priority assessment";
        assessment.autonomousActionRequired = true;
        assessment.humanOversightNeeded = false;
        return assessment;
    }

    private ResourceAllocationDecision createDefaultResourceAllocation() {
        ResourceAllocationDecision decision = new ResourceAllocationDecision();
        decision.emergencyServicesContact = false;
        decision.trustedContactsNotify = true;
        decision.medicalServicesAlert = false;
        decision.securityServicesAlert = false;
        decision.simultaneousContact = false;
        return decision;
    }

    private CommunicationStrategy createDefaultCommunicationStrategy() {
        CommunicationStrategy strategy = new CommunicationStrategy();
        strategy.primaryCommunicationMethod = "SMS";
        strategy.backupCommunicationMethod = "VOICE";
        strategy.messageUrgencyLevel = "MEDIUM";
        strategy.retryStrategy = "DELAYED";
        strategy.escalationProtocol = false;
        strategy.contextSharingLevel = "PARTIAL";
        return strategy;
    }

    private IntegratedEmergencyDecision createDefaultIntegratedDecision() {
        IntegratedEmergencyDecision decision = new IntegratedEmergencyDecision();
        decision.decisionId = generateDecisionId();
        decision.responseType = "MONITORING_ALERT";
        decision.priorityLevel = "MEDIUM";
        decision.autonomousExecution = "SUPERVISED";
        decision.expectedResponseTime = 300;
        decision.decisionConfidence = 0.5;
        decision.decisionReasoning = "Default emergency decision due to analysis failure";
        decision.fallbackPlan = "Manual review required";
        return decision;
    }

    private EmergencyDecisionResult createFailedEmergencyDecision(EmergencyDecisionInput input, String errorMessage) {
        return EmergencyDecisionResult.builder()
                .decisionId(generateDecisionId())
                .assessmentId(input.getAssessmentId())
                .success(false)
                .errorMessage(errorMessage)
                .decisionTimestamp(Instant.now())
                .build();
    }

    // Data classes (simplified for MVP)
    public static class EmergencyDecisionInput {
        private String assessmentId;
        private String userId;
        private String threatLevel;
        private double confidenceScore;
        private String llmReasoning;
        private String location;
        private String timeContext;
        private String environmentalFactors;
        private String audioData;
        private String motionData;
        private String biometricData;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyDecisionInput input = new EmergencyDecisionInput();
            public Builder assessmentId(String assessmentId) { input.assessmentId = assessmentId; return this; }
            public Builder userId(String userId) { input.userId = userId; return this; }
            public Builder threatLevel(String threatLevel) { input.threatLevel = threatLevel; return this; }
            public Builder confidenceScore(double confidenceScore) { input.confidenceScore = confidenceScore; return this; }
            public Builder llmReasoning(String llmReasoning) { input.llmReasoning = llmReasoning; return this; }
            public Builder location(String location) { input.location = location; return this; }
            public Builder timeContext(String timeContext) { input.timeContext = timeContext; return this; }
            public Builder environmentalFactors(String environmentalFactors) { input.environmentalFactors = environmentalFactors; return this; }
            public Builder audioData(String audioData) { input.audioData = audioData; return this; }
            public Builder motionData(String motionData) { input.motionData = motionData; return this; }
            public Builder biometricData(String biometricData) { input.biometricData = biometricData; return this; }
            public EmergencyDecisionInput build() { return input; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getUserId() { return userId; }
        public String getThreatLevel() { return threatLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getLlmReasoning() { return llmReasoning; }
        public String getLocation() { return location; }
        public String getTimeContext() { return timeContext; }
        public String getEnvironmentalFactors() { return environmentalFactors; }
        public String getAudioData() { return audioData; }
        public String getMotionData() { return motionData; }
        public String getBiometricData() { return biometricData; }
    }

    public static class EmergencySituationAnalysis {
        public String overallSeverity;
        public String primaryEmergencyType;
        public String immediateDanger;
        public String medicalEmergency;
        public String securityThreat;
        public String escalationPotential;
        public double situationConfidence;
    }

    public static class ResponsePriorityAssessment {
        public String responsePriority;
        public int responseTimeTarget;
        public String justification;
        public boolean autonomousActionRequired;
        public boolean humanOversightNeeded;
    }

    public static class ResourceAllocationDecision {
        public boolean emergencyServicesContact;
        public boolean trustedContactsNotify;
        public boolean medicalServicesAlert;
        public boolean securityServicesAlert;
        public boolean simultaneousContact;
    }

    public static class CommunicationStrategy {
        public String primaryCommunicationMethod;
        public String backupCommunicationMethod;
        public String messageUrgencyLevel;
        public String retryStrategy;
        public boolean escalationProtocol;
        public String contextSharingLevel;
    }

    public static class IntegratedEmergencyDecision {
        public String decisionId;
        public String responseType;
        public String priorityLevel;
        public String autonomousExecution;
        public int expectedResponseTime;
        public double decisionConfidence;
        public String decisionReasoning;
        public String fallbackPlan;
    }

    public static class EmergencyDecisionResult {
        private String decisionId;
        private String assessmentId;
        private String emergencyEventId;
        private EmergencySituationAnalysis situationAnalysis;
        private ResponsePriorityAssessment priorityAssessment;
        private ResourceAllocationDecision resourceDecision;
        private CommunicationStrategy communicationStrategy;
        private IntegratedEmergencyDecision finalDecision;
        private EmergencyEvent emergencyEvent;
        private Instant decisionTimestamp;
        private boolean success;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyDecisionResult result = new EmergencyDecisionResult();
            public Builder decisionId(String decisionId) { result.decisionId = decisionId; return this; }
            public Builder assessmentId(String assessmentId) { result.assessmentId = assessmentId; return this; }
            public Builder emergencyEventId(String emergencyEventId) { result.emergencyEventId = emergencyEventId; return this; }
            public Builder situationAnalysis(EmergencySituationAnalysis situationAnalysis) { result.situationAnalysis = situationAnalysis; return this; }
            public Builder priorityAssessment(ResponsePriorityAssessment priorityAssessment) { result.priorityAssessment = priorityAssessment; return this; }
            public Builder resourceDecision(ResourceAllocationDecision resourceDecision) { result.resourceDecision = resourceDecision; return this; }
            public Builder communicationStrategy(CommunicationStrategy communicationStrategy) { result.communicationStrategy = communicationStrategy; return this; }
            public Builder finalDecision(IntegratedEmergencyDecision finalDecision) { result.finalDecision = finalDecision; return this; }
            public Builder emergencyEvent(EmergencyEvent emergencyEvent) { result.emergencyEvent = emergencyEvent; return this; }
            public Builder decisionTimestamp(Instant decisionTimestamp) { result.decisionTimestamp = decisionTimestamp; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public EmergencyDecisionResult build() { return result; }
        }

        // Getters
        public String getDecisionId() { return decisionId; }
        public String getAssessmentId() { return assessmentId; }
        public String getEmergencyEventId() { return emergencyEventId; }
        public EmergencySituationAnalysis getSituationAnalysis() { return situationAnalysis; }
        public ResponsePriorityAssessment getPriorityAssessment() { return priorityAssessment; }
        public ResourceAllocationDecision getResourceDecision() { return resourceDecision; }
        public CommunicationStrategy getCommunicationStrategy() { return communicationStrategy; }
        public IntegratedEmergencyDecision getFinalDecision() { return finalDecision; }
        public EmergencyEvent getEmergencyEvent() { return emergencyEvent; }
        public Instant getDecisionTimestamp() { return decisionTimestamp; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}