package com.allsenses.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autonomous Decision Service for AllSenses AI Guardian
 * 
 * This service demonstrates AI agent qualification (Condition 3) by implementing
 * autonomous decision-making capabilities using LLM responses. The agent can
 * make decisions with or without human inputs based on LLM reasoning.
 */
@Service
public class AutonomousDecisionService {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousDecisionService.class);

    @Autowired
    private ThreatAssessmentReasoningService threatAssessmentService;

    @Autowired
    private BedrockLlmService bedrockLlmService;

    /**
     * Make autonomous decision based on threat assessment
     * 
     * This method demonstrates autonomous capabilities by making decisions
     * without human intervention based on LLM reasoning.
     * 
     * @param sensorData Sensor data context
     * @return Autonomous decision result
     */
    public AutonomousDecisionResult makeAutonomousDecision(
            ThreatAssessmentReasoningService.SensorDataContext sensorData) {
        
        logger.info("Starting autonomous decision-making process for user: {}", sensorData.getUserId());
        
        try {
            // Step 1: Perform LLM-powered threat assessment
            ThreatAssessmentReasoningService.ThreatAssessmentResult assessment = 
                threatAssessmentService.performThreatAssessment(sensorData);
            
            if (!assessment.isSuccess()) {
                return createFailedDecision("Threat assessment failed", assessment);
            }
            
            // Step 2: Generate autonomous decision based on LLM assessment
            AutonomousDecision decision = generateAutonomousDecision(assessment);
            
            // Step 3: Execute autonomous actions if required
            List<AutonomousAction> executedActions = executeAutonomousActions(decision, assessment);
            
            // Step 4: Create decision result
            AutonomousDecisionResult result = AutonomousDecisionResult.builder()
                    .decisionId(generateDecisionId())
                    .userId(sensorData.getUserId())
                    .threatAssessment(assessment)
                    .autonomousDecision(decision)
                    .executedActions(executedActions)
                    .decisionTimestamp(LocalDateTime.now())
                    .success(true)
                    .requiresHumanIntervention(decision.requiresHumanIntervention())
                    .build();
            
            logger.info("Autonomous decision completed. Decision: {}, Actions: {}", 
                       decision.getDecisionType(), executedActions.size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during autonomous decision-making", e);
            return createFailedDecision("Decision-making error: " + e.getMessage(), null);
        }
    }

    /**
     * Generate autonomous decision based on LLM threat assessment
     */
    private AutonomousDecision generateAutonomousDecision(
            ThreatAssessmentReasoningService.ThreatAssessmentResult assessment) {
        
        DecisionType decisionType;
        List<ActionType> requiredActions = new ArrayList<>();
        boolean requiresHumanIntervention = false;
        String decisionReasoning;
        
        // Decision logic based on threat level and confidence
        switch (assessment.getThreatLevel()) {
            case CRITICAL:
                if (assessment.getConfidenceScore() >= 0.8) {
                    decisionType = DecisionType.IMMEDIATE_EMERGENCY_RESPONSE;
                    requiredActions.add(ActionType.CONTACT_EMERGENCY_SERVICES);
                    requiredActions.add(ActionType.NOTIFY_TRUSTED_CONTACTS);
                    requiredActions.add(ActionType.ACTIVATE_EMERGENCY_PROTOCOL);
                    requiresHumanIntervention = false; // Fully autonomous
                } else {
                    decisionType = DecisionType.ESCALATED_MONITORING;
                    requiredActions.add(ActionType.NOTIFY_TRUSTED_CONTACTS);
                    requiredActions.add(ActionType.INCREASE_MONITORING);
                    requiresHumanIntervention = true; // Human verification needed
                }
                decisionReasoning = "Critical threat detected with " + 
                    (assessment.getConfidenceScore() >= 0.8 ? "high" : "moderate") + " confidence";
                break;
                
            case HIGH:
                if (assessment.getConfidenceScore() >= 0.7) {
                    decisionType = DecisionType.ALERT_AND_MONITOR;
                    requiredActions.add(ActionType.NOTIFY_TRUSTED_CONTACTS);
                    requiredActions.add(ActionType.INCREASE_MONITORING);
                    requiresHumanIntervention = false;
                } else {
                    decisionType = DecisionType.ENHANCED_MONITORING;
                    requiredActions.add(ActionType.INCREASE_MONITORING);
                    requiresHumanIntervention = true;
                }
                decisionReasoning = "High threat level requires immediate attention and monitoring";
                break;
                
            case MEDIUM:
                decisionType = DecisionType.ENHANCED_MONITORING;
                requiredActions.add(ActionType.INCREASE_MONITORING);
                requiresHumanIntervention = false;
                decisionReasoning = "Medium threat level - enhanced monitoring activated";
                break;
                
            case LOW:
                decisionType = DecisionType.CONTINUE_MONITORING;
                requiredActions.add(ActionType.CONTINUE_NORMAL_MONITORING);
                requiresHumanIntervention = false;
                decisionReasoning = "Low threat level - continue normal monitoring";
                break;
                
            default: // NONE
                decisionType = DecisionType.NO_ACTION_REQUIRED;
                requiredActions.add(ActionType.CONTINUE_NORMAL_MONITORING);
                requiresHumanIntervention = false;
                decisionReasoning = "No threat detected - normal operations";
                break;
        }
        
        return AutonomousDecision.builder()
                .decisionType(decisionType)
                .requiredActions(requiredActions)
                .requiresHumanIntervention(requiresHumanIntervention)
                .decisionReasoning(decisionReasoning)
                .confidenceInDecision(calculateDecisionConfidence(assessment))
                .build();
    }

    /**
     * Execute autonomous actions based on decision
     */
    private List<AutonomousAction> executeAutonomousActions(
            AutonomousDecision decision, 
            ThreatAssessmentReasoningService.ThreatAssessmentResult assessment) {
        
        List<AutonomousAction> executedActions = new ArrayList<>();
        
        for (ActionType actionType : decision.getRequiredActions()) {
            AutonomousAction action = executeAction(actionType, assessment);
            executedActions.add(action);
        }
        
        return executedActions;
    }

    /**
     * Execute individual autonomous action
     */
    private AutonomousAction executeAction(
            ActionType actionType, 
            ThreatAssessmentReasoningService.ThreatAssessmentResult assessment) {
        
        logger.info("Executing autonomous action: {}", actionType);
        
        try {
            String actionResult;
            boolean success = true;
            
            switch (actionType) {
                case CONTACT_EMERGENCY_SERVICES:
                    actionResult = "Emergency services contact initiated (SIMULATED)";
                    // In real implementation: integrate with emergency services API
                    break;
                    
                case NOTIFY_TRUSTED_CONTACTS:
                    actionResult = "Trusted contacts notified via SMS/call (SIMULATED)";
                    // In real implementation: integrate with SNS for notifications
                    break;
                    
                case ACTIVATE_EMERGENCY_PROTOCOL:
                    actionResult = "Emergency protocol activated - location shared, context transmitted";
                    // In real implementation: activate full emergency response
                    break;
                    
                case INCREASE_MONITORING:
                    actionResult = "Monitoring frequency increased to high-sensitivity mode";
                    // In real implementation: adjust sensor sampling rates
                    break;
                    
                case CONTINUE_NORMAL_MONITORING:
                    actionResult = "Normal monitoring continued";
                    break;
                    
                default:
                    actionResult = "Unknown action type";
                    success = false;
                    break;
            }
            
            return AutonomousAction.builder()
                    .actionType(actionType)
                    .actionResult(actionResult)
                    .executionTimestamp(LocalDateTime.now())
                    .success(success)
                    .build();
            
        } catch (Exception e) {
            logger.error("Failed to execute autonomous action: {}", actionType, e);
            return AutonomousAction.builder()
                    .actionType(actionType)
                    .actionResult("Action failed: " + e.getMessage())
                    .executionTimestamp(LocalDateTime.now())
                    .success(false)
                    .build();
        }
    }

    /**
     * Calculate confidence in the autonomous decision
     */
    private double calculateDecisionConfidence(ThreatAssessmentReasoningService.ThreatAssessmentResult assessment) {
        // Base confidence on LLM assessment confidence and threat level consistency
        double baseConfidence = assessment.getConfidenceScore();
        
        // Adjust based on threat level clarity
        if (assessment.getThreatLevel() == ThreatAssessmentReasoningService.ThreatLevel.CRITICAL ||
            assessment.getThreatLevel() == ThreatAssessmentReasoningService.ThreatLevel.NONE) {
            return Math.min(1.0, baseConfidence + 0.1); // Higher confidence for clear cases
        } else {
            return Math.max(0.1, baseConfidence - 0.1); // Lower confidence for ambiguous cases
        }
    }

    /**
     * Create failed decision result
     */
    private AutonomousDecisionResult createFailedDecision(String errorMessage, 
            ThreatAssessmentReasoningService.ThreatAssessmentResult assessment) {
        return AutonomousDecisionResult.builder()
                .decisionId(generateDecisionId())
                .userId(assessment != null ? assessment.getUserId() : "unknown")
                .threatAssessment(assessment)
                .autonomousDecision(AutonomousDecision.builder()
                    .decisionType(DecisionType.SYSTEM_ERROR)
                    .decisionReasoning(errorMessage)
                    .requiresHumanIntervention(true)
                    .build())
                .executedActions(new ArrayList<>())
                .decisionTimestamp(LocalDateTime.now())
                .success(false)
                .requiresHumanIntervention(true)
                .build();
    }

    /**
     * Generate unique decision ID
     */
    private String generateDecisionId() {
        return "DECISION-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    /**
     * Test autonomous decision-making with sample scenario
     */
    public AutonomousDecisionResult testAutonomousDecision() {
        ThreatAssessmentReasoningService.SensorDataContext testData = 
            ThreatAssessmentReasoningService.SensorDataContext.builder()
                .userId("test-user-autonomous")
                .location("Test Location for Autonomous Decision")
                .audioData("Distress sounds detected")
                .motionData("Unusual movement patterns")
                .additionalContext("Testing autonomous decision-making capabilities")
                .build();
        
        return makeAutonomousDecision(testData);
    }

    // Enums and Data Classes

    public enum DecisionType {
        NO_ACTION_REQUIRED,
        CONTINUE_MONITORING,
        ENHANCED_MONITORING,
        ALERT_AND_MONITOR,
        ESCALATED_MONITORING,
        IMMEDIATE_EMERGENCY_RESPONSE,
        SYSTEM_ERROR
    }

    public enum ActionType {
        CONTINUE_NORMAL_MONITORING,
        INCREASE_MONITORING,
        NOTIFY_TRUSTED_CONTACTS,
        CONTACT_EMERGENCY_SERVICES,
        ACTIVATE_EMERGENCY_PROTOCOL
    }

    public static class AutonomousDecision {
        private DecisionType decisionType;
        private List<ActionType> requiredActions;
        private boolean requiresHumanIntervention;
        private String decisionReasoning;
        private double confidenceInDecision;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private AutonomousDecision decision = new AutonomousDecision();
            public Builder decisionType(DecisionType decisionType) { decision.decisionType = decisionType; return this; }
            public Builder requiredActions(List<ActionType> requiredActions) { decision.requiredActions = requiredActions; return this; }
            public Builder requiresHumanIntervention(boolean requiresHumanIntervention) { decision.requiresHumanIntervention = requiresHumanIntervention; return this; }
            public Builder decisionReasoning(String decisionReasoning) { decision.decisionReasoning = decisionReasoning; return this; }
            public Builder confidenceInDecision(double confidenceInDecision) { decision.confidenceInDecision = confidenceInDecision; return this; }
            public AutonomousDecision build() { return decision; }
        }

        // Getters
        public DecisionType getDecisionType() { return decisionType; }
        public List<ActionType> getRequiredActions() { return requiredActions; }
        public boolean requiresHumanIntervention() { return requiresHumanIntervention; }
        public String getDecisionReasoning() { return decisionReasoning; }
        public double getConfidenceInDecision() { return confidenceInDecision; }
    }

    public static class AutonomousAction {
        private ActionType actionType;
        private String actionResult;
        private LocalDateTime executionTimestamp;
        private boolean success;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private AutonomousAction action = new AutonomousAction();
            public Builder actionType(ActionType actionType) { action.actionType = actionType; return this; }
            public Builder actionResult(String actionResult) { action.actionResult = actionResult; return this; }
            public Builder executionTimestamp(LocalDateTime executionTimestamp) { action.executionTimestamp = executionTimestamp; return this; }
            public Builder success(boolean success) { action.success = success; return this; }
            public AutonomousAction build() { return action; }
        }

        // Getters
        public ActionType getActionType() { return actionType; }
        public String getActionResult() { return actionResult; }
        public LocalDateTime getExecutionTimestamp() { return executionTimestamp; }
        public boolean isSuccess() { return success; }
    }

    public static class AutonomousDecisionResult {
        private String decisionId;
        private String userId;
        private ThreatAssessmentReasoningService.ThreatAssessmentResult threatAssessment;
        private AutonomousDecision autonomousDecision;
        private List<AutonomousAction> executedActions;
        private LocalDateTime decisionTimestamp;
        private boolean success;
        private boolean requiresHumanIntervention;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private AutonomousDecisionResult result = new AutonomousDecisionResult();
            public Builder decisionId(String decisionId) { result.decisionId = decisionId; return this; }
            public Builder userId(String userId) { result.userId = userId; return this; }
            public Builder threatAssessment(ThreatAssessmentReasoningService.ThreatAssessmentResult threatAssessment) { result.threatAssessment = threatAssessment; return this; }
            public Builder autonomousDecision(AutonomousDecision autonomousDecision) { result.autonomousDecision = autonomousDecision; return this; }
            public Builder executedActions(List<AutonomousAction> executedActions) { result.executedActions = executedActions; return this; }
            public Builder decisionTimestamp(LocalDateTime decisionTimestamp) { result.decisionTimestamp = decisionTimestamp; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder requiresHumanIntervention(boolean requiresHumanIntervention) { result.requiresHumanIntervention = requiresHumanIntervention; return this; }
            public AutonomousDecisionResult build() { return result; }
        }

        // Getters
        public String getDecisionId() { return decisionId; }
        public String getUserId() { return userId; }
        public ThreatAssessmentReasoningService.ThreatAssessmentResult getThreatAssessment() { return threatAssessment; }
        public AutonomousDecision getAutonomousDecision() { return autonomousDecision; }
        public List<AutonomousAction> getExecutedActions() { return executedActions; }
        public LocalDateTime getDecisionTimestamp() { return decisionTimestamp; }
        public boolean isSuccess() { return success; }
        public boolean requiresHumanIntervention() { return requiresHumanIntervention; }
    }
}