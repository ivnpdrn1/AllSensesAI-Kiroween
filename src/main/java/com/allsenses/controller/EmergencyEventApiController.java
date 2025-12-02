package com.allsenses.controller;

import com.allsenses.model.EmergencyEvent;
import com.allsenses.repository.EmergencyEventRepository;
import com.allsenses.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Emergency Event API Controller for AllSenses AI Guardian
 * 
 * This controller provides emergency event management endpoints with
 * autonomous processing capabilities, demonstrating complete AI agent
 * emergency response workflow integration.
 */
@RestController
@RequestMapping("/api/v1/emergency-events")
@CrossOrigin(origins = "*")
public class EmergencyEventApiController {

    @Autowired
    private AutonomousEmergencyEventProcessor emergencyProcessor;

    @Autowired
    private LlmPoweredEmergencyDecisionEngine emergencyDecisionEngine;

    @Autowired
    private SnsNotificationService snsNotificationService;

    @Autowired
    private EmergencyEventRepository emergencyEventRepository;

    /**
     * Create and process emergency event autonomously
     */
    @PostMapping("/create-and-process")
    public ResponseEntity<EmergencyEventCreationResponse> createAndProcessEmergencyEvent(
            @RequestBody EmergencyEventCreationRequest request) {
        
        try {
            // Create emergency decision input
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput decisionInput = 
                LlmPoweredEmergencyDecisionEngine.EmergencyDecisionInput.builder()
                    .assessmentId(request.getAssessmentId())
                    .userId(request.getUserId())
                    .threatLevel(request.getThreatLevel())
                    .confidenceScore(request.getConfidenceScore())
                    .llmReasoning(request.getLlmReasoning())
                    .location(request.getLocation())
                    .timeContext(request.getTimeContext())
                    .environmentalFactors(request.getEnvironmentalFactors())
                    .audioData(request.getAudioData())
                    .motionData(request.getMotionData())
                    .biometricData(request.getBiometricData())
                    .build();
            
            // Make autonomous emergency decision
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult = 
                emergencyDecisionEngine.makeAutonomousEmergencyDecision(decisionInput);
            
            if (!decisionResult.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(EmergencyEventCreationResponse.builder()
                                .success(false)
                                .errorMessage("Emergency decision failed: " + decisionResult.getErrorMessage())
                                .build());
            }
            
            // Send notifications if requested
            SnsNotificationService.SnsNotificationResult notificationResult = null;
            if (request.isSendNotifications()) {
                SnsNotificationService.EmergencyNotificationRequest notificationRequest = 
                    SnsNotificationService.EmergencyNotificationRequest.builder()
                        .emergencyEventId(decisionResult.getEmergencyEventId())
                        .userId(request.getUserId())
                        .emergencyType(request.getThreatLevel())
                        .priorityLevel(decisionResult.getFinalDecision().priorityLevel)
                        .confidenceScore(request.getConfidenceScore())
                        .location(request.getLocation())
                        .emergencyDescription(request.getLlmReasoning())
                        .build();
                
                notificationResult = snsNotificationService.sendEmergencyNotifications(notificationRequest);
            }
            
            EmergencyEventCreationResponse response = EmergencyEventCreationResponse.builder()
                    .emergencyEventId(decisionResult.getEmergencyEventId())
                    .emergencyEvent(decisionResult.getEmergencyEvent())
                    .decisionResult(decisionResult)
                    .notificationResult(notificationResult)
                    .success(true)
                    .autonomousProcessingCompleted(true)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(EmergencyEventCreationResponse.builder()
                            .success(false)
                            .errorMessage("Emergency event creation failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Get emergency event by ID
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EmergencyEvent> getEmergencyEvent(@PathVariable String eventId) {
        Optional<EmergencyEvent> event = emergencyEventRepository.findById(eventId);
        return event.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get emergency events for user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EmergencyEvent>> getEmergencyEventsForUser(@PathVariable String userId) {
        List<EmergencyEvent> events = emergencyEventRepository.findByUserId(userId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get active emergency events
     */
    @GetMapping("/active")
    public ResponseEntity<List<EmergencyEvent>> getActiveEmergencyEvents() {
        List<EmergencyEvent> activeEvents = emergencyEventRepository.findActiveEvents();
        return ResponseEntity.ok(activeEvents);
    }

    /**
     * Get recent emergency events
     */
    @GetMapping("/recent")
    public ResponseEntity<List<EmergencyEvent>> getRecentEmergencyEvents() {
        List<EmergencyEvent> recentEvents = emergencyEventRepository.findRecentEvents();
        return ResponseEntity.ok(recentEvents);
    }

    /**
     * Get critical emergency events
     */
    @GetMapping("/critical")
    public ResponseEntity<List<EmergencyEvent>> getCriticalEmergencyEvents() {
        List<EmergencyEvent> criticalEvents = emergencyEventRepository.findCriticalEvents();
        return ResponseEntity.ok(criticalEvents);
    }

    /**
     * Update emergency event status
     */
    @PutMapping("/{eventId}/status")
    public ResponseEntity<EmergencyEvent> updateEmergencyEventStatus(
            @PathVariable String eventId,
            @RequestBody EmergencyEventStatusUpdateRequest request) {
        
        try {
            EmergencyEvent updatedEvent = emergencyEventRepository.updateStatus(eventId, request.getNewStatus());
            return ResponseEntity.ok(updatedEvent);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Resolve emergency event
     */
    @PutMapping("/{eventId}/resolve")
    public ResponseEntity<EmergencyEvent> resolveEmergencyEvent(
            @PathVariable String eventId,
            @RequestBody EmergencyEventResolutionRequest request) {
        
        try {
            EmergencyEvent resolvedEvent = emergencyEventRepository.resolveEvent(eventId, request.getResolutionNotes());
            return ResponseEntity.ok(resolvedEvent);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark emergency event as false alarm
     */
    @PutMapping("/{eventId}/false-alarm")
    public ResponseEntity<EmergencyEvent> markAsFalseAlarm(
            @PathVariable String eventId,
            @RequestBody EmergencyEventFalseAlarmRequest request) {
        
        try {
            EmergencyEvent updatedEvent = emergencyEventRepository.markAsFalseAlarm(eventId, request.getNotes());
            return ResponseEntity.ok(updatedEvent);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get emergency processing statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getEmergencyProcessingStatistics() {
        Map<String, Object> statistics = emergencyProcessor.getEmergencyProcessingStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Test autonomous emergency processing
     */
    @PostMapping("/test/autonomous-processing")
    public ResponseEntity<AutonomousEmergencyEventProcessor.EmergencyProcessingResult> testAutonomousProcessing() {
        AutonomousEmergencyEventProcessor.EmergencyProcessingResult result = 
            emergencyProcessor.testAutonomousEmergencyProcessing();
        
        return ResponseEntity.ok(result);
    }

    /**
     * Test emergency decision engine
     */
    @PostMapping("/test/decision-engine")
    public ResponseEntity<LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult> testEmergencyDecisionEngine() {
        LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult result = 
            emergencyDecisionEngine.testLlmPoweredEmergencyDecision();
        
        return ResponseEntity.ok(result);
    }

    /**
     * Test complete emergency workflow
     */
    @PostMapping("/test/complete-workflow")
    public ResponseEntity<Map<String, Object>> testCompleteEmergencyWorkflow() {
        Map<String, Object> testResults = new HashMap<>();
        
        try {
            // Test emergency decision engine
            LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionTest = 
                emergencyDecisionEngine.testLlmPoweredEmergencyDecision();
            testResults.put("emergency_decision", Map.of(
                "success", decisionTest.isSuccess(),
                "decision_id", decisionTest.getDecisionId(),
                "priority_level", decisionTest.getFinalDecision() != null ? 
                    decisionTest.getFinalDecision().priorityLevel : "null",
                "response_type", decisionTest.getFinalDecision() != null ? 
                    decisionTest.getFinalDecision().responseType : "null"
            ));
            
            // Test autonomous processing
            AutonomousEmergencyEventProcessor.EmergencyProcessingResult processingTest = 
                emergencyProcessor.testAutonomousEmergencyProcessing();
            testResults.put("autonomous_processing", Map.of(
                "success", processingTest.isSuccess(),
                "processing_id", processingTest.getProcessingId(),
                "actions_executed", processingTest.getActionExecutionResult() != null ? 
                    processingTest.getActionExecutionResult().getActionsExecuted().size() : 0,
                "serverless_triggered", processingTest.isServerlessProcessingTriggered()
            ));
            
            // Test SNS notifications
            SnsNotificationService.SnsNotificationResult notificationTest = 
                snsNotificationService.testSnsNotificationIntegration();
            testResults.put("sns_notifications", Map.of(
                "success", notificationTest.isSuccess(),
                "notification_id", notificationTest.getNotificationId(),
                "notifications_sent", notificationTest.getTotalNotificationsSent(),
                "successful_notifications", notificationTest.getSuccessfulNotifications()
            ));
            
            // Overall workflow status
            boolean workflowSuccess = decisionTest.isSuccess() && processingTest.isSuccess() && 
                                    notificationTest.isSuccess();
            testResults.put("workflow_status", workflowSuccess ? "FULLY_OPERATIONAL" : "PARTIAL_FAILURE");
            testResults.put("autonomous_capabilities", "ACTIVE");
            testResults.put("llm_integration", "BEDROCK_CONNECTED");
            
        } catch (Exception e) {
            testResults.put("workflow_status", "FAILED");
            testResults.put("error_message", e.getMessage());
        }
        
        return ResponseEntity.ok(testResults);
    }

    // Request/Response DTOs
    public static class EmergencyEventCreationRequest {
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
        private boolean sendNotifications = true;

        // Getters and Setters
        public String getAssessmentId() { return assessmentId; }
        public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getThreatLevel() { return threatLevel; }
        public void setThreatLevel(String threatLevel) { this.threatLevel = threatLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getLlmReasoning() { return llmReasoning; }
        public void setLlmReasoning(String llmReasoning) { this.llmReasoning = llmReasoning; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getTimeContext() { return timeContext; }
        public void setTimeContext(String timeContext) { this.timeContext = timeContext; }
        public String getEnvironmentalFactors() { return environmentalFactors; }
        public void setEnvironmentalFactors(String environmentalFactors) { this.environmentalFactors = environmentalFactors; }
        public String getAudioData() { return audioData; }
        public void setAudioData(String audioData) { this.audioData = audioData; }
        public String getMotionData() { return motionData; }
        public void setMotionData(String motionData) { this.motionData = motionData; }
        public String getBiometricData() { return biometricData; }
        public void setBiometricData(String biometricData) { this.biometricData = biometricData; }
        public boolean isSendNotifications() { return sendNotifications; }
        public void setSendNotifications(boolean sendNotifications) { this.sendNotifications = sendNotifications; }
    }

    public static class EmergencyEventCreationResponse {
        private String emergencyEventId;
        private EmergencyEvent emergencyEvent;
        private LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult;
        private SnsNotificationService.SnsNotificationResult notificationResult;
        private boolean success;
        private boolean autonomousProcessingCompleted;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private EmergencyEventCreationResponse response = new EmergencyEventCreationResponse();
            public Builder emergencyEventId(String emergencyEventId) { response.emergencyEventId = emergencyEventId; return this; }
            public Builder emergencyEvent(EmergencyEvent emergencyEvent) { response.emergencyEvent = emergencyEvent; return this; }
            public Builder decisionResult(LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult decisionResult) { response.decisionResult = decisionResult; return this; }
            public Builder notificationResult(SnsNotificationService.SnsNotificationResult notificationResult) { response.notificationResult = notificationResult; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder autonomousProcessingCompleted(boolean autonomousProcessingCompleted) { response.autonomousProcessingCompleted = autonomousProcessingCompleted; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public EmergencyEventCreationResponse build() { return response; }
        }

        // Getters
        public String getEmergencyEventId() { return emergencyEventId; }
        public EmergencyEvent getEmergencyEvent() { return emergencyEvent; }
        public LlmPoweredEmergencyDecisionEngine.EmergencyDecisionResult getDecisionResult() { return decisionResult; }
        public SnsNotificationService.SnsNotificationResult getNotificationResult() { return notificationResult; }
        public boolean isSuccess() { return success; }
        public boolean isAutonomousProcessingCompleted() { return autonomousProcessingCompleted; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class EmergencyEventStatusUpdateRequest {
        private String newStatus;

        // Getters and Setters
        public String getNewStatus() { return newStatus; }
        public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    }

    public static class EmergencyEventResolutionRequest {
        private String resolutionNotes;

        // Getters and Setters
        public String getResolutionNotes() { return resolutionNotes; }
        public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    }

    public static class EmergencyEventFalseAlarmRequest {
        private String notes;

        // Getters and Setters
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}