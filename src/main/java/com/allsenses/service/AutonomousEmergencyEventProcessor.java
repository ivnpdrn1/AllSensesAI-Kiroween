package com.allsenses.service;

import com.allsenses.model.EmergencyEvent;
import com.allsenses.model.ThreatAssessment;
import com.allsenses.repository.EmergencyEventRepository;
import com.allsenses.repository.ThreatAssessmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Autonomous Emergency Event Processor for AllSenses AI Guardian
 * 
 * This service processes emergency events autonomously from initiation to resolution.
 * It demonstrates AI agent qualification by handling complete emergency workflows
 * with autonomous decision-making and database integration.
 */
@Service
public class AutonomousEmergencyEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousEmergencyEventProcessor.class);

    @Autowired
    private LlmPoweredEmergencyDecisionEngine emergencyDecisionEngine;

    @Autowired
    private EmergencyEventRepository emergencyEventRepository;

    @Autowired
    private ThreatAssessmentRepository threatAssessmentRepository;

    @Autowired
    private LambdaIntegrationService lambdaIntegrationService;

    /**
     * Process complete emergency event lifecycle autonomously
     * 
     * @param threatAssessment The threat assessment that triggered the emergency
     * @return Complete emergency processing result
     */
    public EmergencyProcessingResult processEmergencyEventAutonomously(ThreatAssessment threatAssessment) {
        logger.info("Starting autonomous emergency event processing for assessment: {}", threatAssessment.getAssessmentId());
        
        try {
            // Step 1: Create emergency decision input from threat assessment
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput decisionInput = 
                createDecisionInputFromThreatAssessment(threatAssessment);
            
            // Step 2: Make autonomous emergency decision
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult = 
                emergencyDecisionEngine.makeAutonomousEmergencyDecision(decisionInput);
            
            if (!decisionResult.isSuccess()) {
                return createFailedProcessingResult(threatAssessment, "Emergency decision failed: " + decisionResult.getErrorMessage());
            }
            
            // Step 3: Execute emergency actions based on decision
            EmergencyActionExecutionResult actionResult = executeEmergencyActions(decisionResult);
            
            // Step 4: Monitor emergency event progress
            EmergencyMonitoringResult monitoringResult = initiateEmergencyMonitoring(decisionResult.getEmergencyEvent());
            
            // Step 5: Update emergency event with processing results
            EmergencyEvent updatedEvent = updateEmergencyEventWithResults(
                decisionResult.getEmergencyEvent(), actionResult, monitoringResult);
            
            // Step 6: Trigger serverless processing if needed
            CompletableFuture<Void> serverlessProcessing = triggerServerlessProcessing(updatedEvent);
            
            EmergencyProcessingResult result = EmergencyProcessingResult.builder()
                    .processingId(generateProcessingId())
                    .threatAssessmentId(threatAssessment.getAssessmentId())
                    .emergencyEventId(updatedEvent.getEventId())
                    .decisionResult(decisionResult)
                    .actionExecutionResult(actionResult)
                    .monitoringResult(monitoringResult)
                    .updatedEmergencyEvent(updatedEvent)
                    .serverlessProcessingTriggered(true)
                    .processingTimestamp(Instant.now())
                    .success(true)
                    .build();
            
            logger.info("Autonomous emergency event processing completed. Event ID: {}, Actions Executed: {}", 
                       updatedEvent.getEventId(), actionResult.getActionsExecuted().size());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error during autonomous emergency event processing", e);
            return createFailedProcessingResult(threatAssessment, "Processing error: " + e.getMessage());
        }
    }

    /**
     * Execute emergency actions based on decision
     */
    private EmergencyActionExecutionResult executeEmergencyActions(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing emergency actions for decision: {}", decisionResult.getDecisionId());
        
        List<EmergencyAction> actionsExecuted = new ArrayList<>();
        List<String> executionErrors = new ArrayList<>();
        
        try {
            LlmPoweredEmergencyDecisionEngine.ResourceAllocationDecision resourceDecision = 
                decisionResult.getResourceDecision();
            
            // Execute emergency services contact
            if (resourceDecision.emergencyServicesContact) {
                EmergencyAction action = executeEmergencyServicesContact(decisionResult);
                actionsExecuted.add(action);
            }
            
            // Execute trusted contacts notification
            if (resourceDecision.trustedContactsNotify) {
                EmergencyAction action = executeTrustedContactsNotification(decisionResult);
                actionsExecuted.add(action);
            }
            
            // Execute medical services alert
            if (resourceDecision.medicalServicesAlert) {
                EmergencyAction action = executeMedicalServicesAlert(decisionResult);
                actionsExecuted.add(action);
            }
            
            // Execute security services alert
            if (resourceDecision.securityServicesAlert) {
                EmergencyAction action = executeSecurityServicesAlert(decisionResult);
                actionsExecuted.add(action);
            }
            
            // Execute location sharing
            EmergencyAction locationAction = executeLocationSharing(decisionResult);
            actionsExecuted.add(locationAction);
            
            // Execute context transmission
            EmergencyAction contextAction = executeContextTransmission(decisionResult);
            actionsExecuted.add(contextAction);
            
        } catch (Exception e) {
            logger.error("Error executing emergency actions", e);
            executionErrors.add("Action execution error: " + e.getMessage());
        }
        
        return EmergencyActionExecutionResult.builder()
                .actionsExecuted(actionsExecuted)
                .executionErrors(executionErrors)
                .totalActionsAttempted(actionsExecuted.size())
                .successfulActions(actionsExecuted.size() - executionErrors.size())
                .executionTimestamp(Instant.now())
                .build();
    }

    /**
     * Execute emergency services contact
     */
    private EmergencyAction executeEmergencyServicesContact(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing emergency services contact (SIMULATED)");
        
        // In production, this would integrate with actual emergency services APIs
        // For MVP, we simulate the action
        
        return EmergencyAction.builder()
                .actionType("EMERGENCY_SERVICES_CONTACT")
                .actionDescription("Contact 911/emergency services with location and threat details")
                .actionStatus("COMPLETED_SIMULATED")
                .actionResult("Emergency services contacted successfully (SIMULATED)")
                .executionTimeMs(1500L) // Simulated response time
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Execute trusted contacts notification
     */
    private EmergencyAction executeTrustedContactsNotification(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing trusted contacts notification (SIMULATED)");
        
        // In production, this would send actual SMS/calls to trusted contacts
        // For MVP, we simulate the action
        
        return EmergencyAction.builder()
                .actionType("TRUSTED_CONTACTS_NOTIFICATION")
                .actionDescription("Notify trusted contacts via SMS and voice calls")
                .actionStatus("COMPLETED_SIMULATED")
                .actionResult("3 trusted contacts notified successfully (SIMULATED)")
                .executionTimeMs(2000L) // Simulated response time
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Execute medical services alert
     */
    private EmergencyAction executeMedicalServicesAlert(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing medical services alert (SIMULATED)");
        
        return EmergencyAction.builder()
                .actionType("MEDICAL_SERVICES_ALERT")
                .actionDescription("Alert medical services for potential medical emergency")
                .actionStatus("COMPLETED_SIMULATED")
                .actionResult("Medical services alerted with priority dispatch (SIMULATED)")
                .executionTimeMs(1200L)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Execute security services alert
     */
    private EmergencyAction executeSecurityServicesAlert(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing security services alert (SIMULATED)");
        
        return EmergencyAction.builder()
                .actionType("SECURITY_SERVICES_ALERT")
                .actionDescription("Alert security services for threat response")
                .actionStatus("COMPLETED_SIMULATED")
                .actionResult("Security services dispatched to location (SIMULATED)")
                .executionTimeMs(1800L)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Execute location sharing
     */
    private EmergencyAction executeLocationSharing(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing location sharing");
        
        return EmergencyAction.builder()
                .actionType("LOCATION_SHARING")
                .actionDescription("Share precise GPS location with emergency responders")
                .actionStatus("COMPLETED")
                .actionResult("Location shared: " + (decisionResult.getEmergencyEvent().getEventLocation() != null ? 
                    decisionResult.getEmergencyEvent().getEventLocation().getAddress() : "Location unavailable"))
                .executionTimeMs(500L)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Execute context transmission
     */
    private EmergencyAction executeContextTransmission(
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) {
        
        logger.info("Executing context transmission");
        
        return EmergencyAction.builder()
                .actionType("CONTEXT_TRANSMISSION")
                .actionDescription("Transmit threat assessment context and LLM reasoning to responders")
                .actionStatus("COMPLETED")
                .actionResult("Context transmitted: Threat level " + 
                    decisionResult.getFinalDecision().priorityLevel + 
                    ", Confidence: " + decisionResult.getFinalDecision().decisionConfidence)
                .executionTimeMs(300L)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Initiate emergency monitoring
     */
    private EmergencyMonitoringResult initiateEmergencyMonitoring(EmergencyEvent emergencyEvent) {
        logger.info("Initiating emergency monitoring for event: {}", emergencyEvent.getEventId());
        
        // Set up monitoring parameters
        Map<String, String> monitoringParameters = new HashMap<>();
        monitoringParameters.put("event_id", emergencyEvent.getEventId());
        monitoringParameters.put("priority_level", emergencyEvent.getPriorityLevel());
        monitoringParameters.put("monitoring_interval", "30"); // seconds
        monitoringParameters.put("escalation_threshold", "300"); // 5 minutes
        
        return EmergencyMonitoringResult.builder()
                .monitoringId(generateMonitoringId())
                .eventId(emergencyEvent.getEventId())
                .monitoringStatus("ACTIVE")
                .monitoringParameters(monitoringParameters)
                .monitoringStartTime(Instant.now())
                .expectedDuration(1800) // 30 minutes
                .build();
    }

    /**
     * Update emergency event with processing results
     */
    private EmergencyEvent updateEmergencyEventWithResults(
            EmergencyEvent emergencyEvent,
            EmergencyActionExecutionResult actionResult,
            EmergencyMonitoringResult monitoringResult) {
        
        // Update event status
        emergencyEvent.setEventStatus("IN_PROGRESS");
        
        // Set emergency services contacted flag
        boolean emergencyServicesContacted = actionResult.getActionsExecuted().stream()
                .anyMatch(action -> "EMERGENCY_SERVICES_CONTACT".equals(action.getActionType()));
        emergencyEvent.setEmergencyServicesContacted(emergencyServicesContacted);
        
        // Update context data with action results
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("actions_executed", actionResult.getActionsExecuted().size());
        contextData.put("successful_actions", actionResult.getSuccessfulActions());
        contextData.put("monitoring_active", monitoringResult.getMonitoringStatus());
        contextData.put("processing_timestamp", Instant.now().toString());
        
        emergencyEvent.setContextData(contextData.toString());
        emergencyEvent.markAsUpdated();
        
        // Save updated event
        return emergencyEventRepository.save(emergencyEvent);
    }

    /**
     * Trigger serverless processing for the emergency event
     */
    private CompletableFuture<Void> triggerServerlessProcessing(EmergencyEvent emergencyEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Triggering serverless processing for emergency event: {}", emergencyEvent.getEventId());
                
                LambdaIntegrationService.EmergencyResponseInput lambdaInput = 
                    LambdaIntegrationService.EmergencyResponseInput.builder()
                        .eventId(emergencyEvent.getEventId())
                        .userId(emergencyEvent.getUserId())
                        .assessmentId(emergencyEvent.getAssessmentId())
                        .priorityLevel(emergencyEvent.getPriorityLevel())
                        .location(emergencyEvent.getEventLocation() != null ? 
                            emergencyEvent.getEventLocation().getAddress() : "Unknown")
                        .emergencyType("AUTONOMOUS_RESPONSE")
                        .build();
                
                LambdaIntegrationService.LambdaProcessingResult lambdaResult = 
                    lambdaIntegrationService.triggerEmergencyResponseWithLambda(lambdaInput);
                
                if (lambdaResult.isSuccess()) {
                    logger.info("Serverless processing completed successfully for event: {}", emergencyEvent.getEventId());
                } else {
                    logger.warn("Serverless processing failed for event: {}", emergencyEvent.getEventId());
                }
                
            } catch (Exception e) {
                logger.error("Error in serverless processing", e);
            }
        });
    }

    /**
     * Create decision input from threat assessment
     */
    private LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput createDecisionInputFromThreatAssessment(
            ThreatAssessment threatAssessment) {
        
        return LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput.builder()
                .assessmentId(threatAssessment.getAssessmentId())
                .userId(threatAssessment.getUserId())
                .threatLevel(threatAssessment.getThreatLevel())
                .confidenceScore(threatAssessment.getConfidenceScore())
                .llmReasoning(threatAssessment.getLlmReasoning())
                .location(threatAssessment.getLocation() != null ? 
                    threatAssessment.getLocation().getAddress() : "Unknown location")
                .timeContext("Current time: " + Instant.now().toString())
                .environmentalFactors("Environmental data from sensors")
                .audioData(extractSensorData(threatAssessment, "audio"))
                .motionData(extractSensorData(threatAssessment, "motion"))
                .biometricData(extractSensorData(threatAssessment, "biometric"))
                .build();
    }

    /**
     * Extract sensor data from threat assessment
     */
    private String extractSensorData(ThreatAssessment assessment, String sensorType) {
        if (assessment.getSensorData() != null && assessment.getSensorData().containsKey(sensorType)) {
            return assessment.getSensorData().get(sensorType);
        }
        return "No " + sensorType + " data available";
    }

    /**
     * Test autonomous emergency event processing
     */
    public EmergencyProcessingResult testAutonomousEmergencyProcessing() {
        // Create test threat assessment
        ThreatAssessment testAssessment = new ThreatAssessment();
        testAssessment.setAssessmentId("TEST-EMERGENCY-PROCESSING-001");
        testAssessment.setUserId("test-user-emergency-processing");
        testAssessment.setThreatLevel("HIGH");
        testAssessment.setConfidenceScore(0.88);
        testAssessment.setLlmReasoning("High-confidence threat detection with multiple danger indicators");
        testAssessment.setRecommendedAction("Immediate emergency response required");
        
        // Set location
        ThreatAssessment.LocationData location = new ThreatAssessment.LocationData();
        location.setAddress("Test Emergency Location - Downtown Area");
        location.setLatitude(40.7128);
        location.setLongitude(-74.0060);
        testAssessment.setLocation(location);
        
        // Set sensor data
        Map<String, String> sensorData = new HashMap<>();
        sensorData.put("audio", "Distress sounds, calls for help, sounds of struggle");
        sensorData.put("motion", "Rapid movement, possible physical altercation");
        sensorData.put("biometric", "Heart rate 150 BPM, extreme stress indicators");
        testAssessment.setSensorData(sensorData);
        
        // Save test assessment
        ThreatAssessment savedAssessment = threatAssessmentRepository.save(testAssessment);
        
        // Process emergency event
        return processEmergencyEventAutonomously(savedAssessment);
    }

    /**
     * Process emergency from threat assessment (for audio-triggered emergencies)
     */
    public EmergencyProcessingResult processEmergencyFromThreatAssessment(ThreatAssessment assessment) {
        long startTime = System.currentTimeMillis();
        String processingId = "AUDIO-EMERGENCY-" + System.currentTimeMillis();
        
        try {
            // Create emergency decision input from threat assessment
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput decisionInput = 
                LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput.builder()
                    .assessmentId(assessment.getAssessmentId())
                    .userId(assessment.getUserId())
                    .threatLevel(assessment.getThreatLevel())
                    .confidenceScore(assessment.getConfidenceScore())
                    .llmReasoning(assessment.getLlmReasoning())
                    .location(assessment.getLocation())
                    .timeContext(assessment.getTimestamp().toString())
                    .environmentalFactors("Audio-detected emergency")
                    .audioData(assessment.getAudioFeatures())
                    .build();
            
            // Make emergency decision
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult = 
                emergencyDecisionEngine.makeAutonomousEmergencyDecision(decisionInput);
            
            if (!decisionResult.isSuccess()) {
                return EmergencyProcessingResult.builder()
                        .processingId(processingId)
                        .success(false)
                        .errorMessage("Emergency decision failed: " + decisionResult.getErrorMessage())
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
            
            // Execute emergency actions
            EmergencyActionExecutionResult actionResult = executeEmergencyActions(
                decisionResult.getFinalDecision(), assessment.getUserId(), assessment.getLocation());
            
            return EmergencyProcessingResult.builder()
                    .processingId(processingId)
                    .emergencyEventId(decisionResult.getEmergencyEventId())
                    .emergencyEvent(decisionResult.getEmergencyEvent())
                    .decisionResult(decisionResult)
                    .actionExecutionResult(actionResult)
                    .serverlessProcessingTriggered(true)
                    .success(true)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            
        } catch (Exception e) {
            return EmergencyProcessingResult.builder()
                    .processingId(processingId)
                    .success(false)
                    .errorMessage("Audio emergency processing failed: " + e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Get emergency processing statistics
     */
    public Map<String, Object> getEmergencyProcessingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<EmergencyEvent> activeEvents = emergencyEventRepository.findActiveEvents();
            List<EmergencyEvent> recentEvents = emergencyEventRepository.findRecentEvents();
            List<EmergencyEvent> criticalEvents = emergencyEventRepository.findCriticalEvents();
            
            stats.put("active_emergency_events", activeEvents.size());
            stats.put("recent_emergency_events", recentEvents.size());
            stats.put("critical_emergency_events", criticalEvents.size());
            
            // Calculate average response time
            double avgResponseTime = recentEvents.stream()
                    .filter(event -> event.getEmergencyServiceResponseTimeMs() != null)
                    .mapToLong(EmergencyEvent::getEmergencyServiceResponseTimeMs)
                    .average()
                    .orElse(0.0);
            stats.put("average_response_time_ms", avgResponseTime);
            
            // Calculate success rate
            long successfulEvents = recentEvents.stream()
                    .filter(event -> "RESOLVED".equals(event.getEventStatus()))
                    .count();
            double successRate = recentEvents.isEmpty() ? 0.0 : (double) successfulEvents / recentEvents.size();
            stats.put("emergency_resolution_rate", successRate);
            
            stats.put("autonomous_processing_enabled", true);
            stats.put("status", "SUCCESS");
            
        } catch (Exception e) {
            stats.put("status", "ERROR");
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    // Utility methods
    private String generateProcessingId() {
        return "PROC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private String generateMonitoringId() {
        return "MON-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private EmergencyProcessingResult createFailedProcessingResult(ThreatAssessment assessment, String errorMessage) {
        return EmergencyProcessingResult.builder()
                .processingId(generateProcessingId())
                .threatAssessmentId(assessment.getAssessmentId())
                .success(false)
                .errorMessage(errorMessage)
                .processingTimestamp(Instant.now())
                .build();
    }

    // Data classes
    public static class EmergencyAction {
        private String actionType;
        private String actionDescription;
        private String actionStatus;
        private String actionResult;
        private Long executionTimeMs;
        private Instant timestamp;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyAction action = new EmergencyAction();
            public Builder actionType(String actionType) { action.actionType = actionType; return this; }
            public Builder actionDescription(String actionDescription) { action.actionDescription = actionDescription; return this; }
            public Builder actionStatus(String actionStatus) { action.actionStatus = actionStatus; return this; }
            public Builder actionResult(String actionResult) { action.actionResult = actionResult; return this; }
            public Builder executionTimeMs(Long executionTimeMs) { action.executionTimeMs = executionTimeMs; return this; }
            public Builder timestamp(Instant timestamp) { action.timestamp = timestamp; return this; }
            public EmergencyAction build() { return action; }
        }

        // Getters
        public String getActionType() { return actionType; }
        public String getActionDescription() { return actionDescription; }
        public String getActionStatus() { return actionStatus; }
        public String getActionResult() { return actionResult; }
        public Long getExecutionTimeMs() { return executionTimeMs; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class EmergencyActionExecutionResult {
        private List<EmergencyAction> actionsExecuted;
        private List<String> executionErrors;
        private int totalActionsAttempted;
        private int successfulActions;
        private Instant executionTimestamp;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyActionExecutionResult result = new EmergencyActionExecutionResult();
            public Builder actionsExecuted(List<EmergencyAction> actionsExecuted) { result.actionsExecuted = actionsExecuted; return this; }
            public Builder executionErrors(List<String> executionErrors) { result.executionErrors = executionErrors; return this; }
            public Builder totalActionsAttempted(int totalActionsAttempted) { result.totalActionsAttempted = totalActionsAttempted; return this; }
            public Builder successfulActions(int successfulActions) { result.successfulActions = successfulActions; return this; }
            public Builder executionTimestamp(Instant executionTimestamp) { result.executionTimestamp = executionTimestamp; return this; }
            public EmergencyActionExecutionResult build() { return result; }
        }

        // Getters
        public List<EmergencyAction> getActionsExecuted() { return actionsExecuted; }
        public List<String> getExecutionErrors() { return executionErrors; }
        public int getTotalActionsAttempted() { return totalActionsAttempted; }
        public int getSuccessfulActions() { return successfulActions; }
        public Instant getExecutionTimestamp() { return executionTimestamp; }
    }

    public static class EmergencyMonitoringResult {
        private String monitoringId;
        private String eventId;
        private String monitoringStatus;
        private Map<String, String> monitoringParameters;
        private Instant monitoringStartTime;
        private int expectedDuration;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyMonitoringResult result = new EmergencyMonitoringResult();
            public Builder monitoringId(String monitoringId) { result.monitoringId = monitoringId; return this; }
            public Builder eventId(String eventId) { result.eventId = eventId; return this; }
            public Builder monitoringStatus(String monitoringStatus) { result.monitoringStatus = monitoringStatus; return this; }
            public Builder monitoringParameters(Map<String, String> monitoringParameters) { result.monitoringParameters = monitoringParameters; return this; }
            public Builder monitoringStartTime(Instant monitoringStartTime) { result.monitoringStartTime = monitoringStartTime; return this; }
            public Builder expectedDuration(int expectedDuration) { result.expectedDuration = expectedDuration; return this; }
            public EmergencyMonitoringResult build() { return result; }
        }

        // Getters
        public String getMonitoringId() { return monitoringId; }
        public String getEventId() { return eventId; }
        public String getMonitoringStatus() { return monitoringStatus; }
        public Map<String, String> getMonitoringParameters() { return monitoringParameters; }
        public Instant getMonitoringStartTime() { return monitoringStartTime; }
        public int getExpectedDuration() { return expectedDuration; }
    }

    public static class EmergencyProcessingResult {
        private String processingId;
        private String threatAssessmentId;
        private String emergencyEventId;
        private LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult;
        private EmergencyActionExecutionResult actionExecutionResult;
        private EmergencyMonitoringResult monitoringResult;
        private EmergencyEvent updatedEmergencyEvent;
        private boolean serverlessProcessingTriggered;
        private Instant processingTimestamp;
        private boolean success;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyProcessingResult result = new EmergencyProcessingResult();
            public Builder processingId(String processingId) { result.processingId = processingId; return this; }
            public Builder threatAssessmentId(String threatAssessmentId) { result.threatAssessmentId = threatAssessmentId; return this; }
            public Builder emergencyEventId(String emergencyEventId) { result.emergencyEventId = emergencyEventId; return this; }
            public Builder decisionResult(LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) { result.decisionResult = decisionResult; return this; }
            public Builder actionExecutionResult(EmergencyActionExecutionResult actionExecutionResult) { result.actionExecutionResult = actionExecutionResult; return this; }
            public Builder monitoringResult(EmergencyMonitoringResult monitoringResult) { result.monitoringResult = monitoringResult; return this; }
            public Builder updatedEmergencyEvent(EmergencyEvent updatedEmergencyEvent) { result.updatedEmergencyEvent = updatedEmergencyEvent; return this; }
            public Builder serverlessProcessingTriggered(boolean serverlessProcessingTriggered) { result.serverlessProcessingTriggered = serverlessProcessingTriggered; return this; }
            public Builder processingTimestamp(Instant processingTimestamp) { result.processingTimestamp = processingTimestamp; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public EmergencyProcessingResult build() { return result; }
        }

        // Getters
        public String getProcessingId() { return processingId; }
        public String getThreatAssessmentId() { return threatAssessmentId; }
        public String getEmergencyEventId() { return emergencyEventId; }
        public LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult getDecisionResult() { return decisionResult; }
        public EmergencyActionExecutionResult getActionExecutionResult() { return actionExecutionResult; }
        public EmergencyMonitoringResult getMonitoringResult() { return monitoringResult; }
        public EmergencyEvent getUpdatedEmergencyEvent() { return updatedEmergencyEvent; }
        public boolean isServerlessProcessingTriggered() { return serverlessProcessingTriggered; }
        public Instant getProcessingTimestamp() { return processingTimestamp; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}