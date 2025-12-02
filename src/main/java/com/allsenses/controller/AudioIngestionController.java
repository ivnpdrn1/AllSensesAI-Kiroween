package com.allsenses.controller;

import com.allsenses.model.ThreatAssessment;
import com.allsenses.service.AudioThreatAnalysisService;
import com.allsenses.service.AutonomousEmergencyEventProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Audio Ingestion Controller for AllSenses AI Guardian
 * 
 * Handles real-time audio capture and processing for threat detection.
 * This is the primary entry point for the complete audio-to-emergency cycle.
 */
@RestController
@RequestMapping("/api/v1/audio")
@CrossOrigin(origins = "*")
public class AudioIngestionController {

    @Autowired
    private AudioThreatAnalysisService audioAnalysisService;

    @Autowired
    private AutonomousEmergencyEventProcessor emergencyProcessor;

    /**
     * Process audio data for threat detection
     * This is the main endpoint for the complete audio cycle
     */
    @PostMapping("/analyze")
    public ResponseEntity<AudioAnalysisResponse> analyzeAudio(
            @RequestBody AudioAnalysisRequest request) {
        
        try {
            // Validate audio data
            if (request.getAudioData() == null || request.getAudioData().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AudioAnalysisResponse.builder()
                                .success(false)
                                .errorMessage("Audio data is required")
                                .build());
            }

            // Analyze audio for threats using AI
            ThreatAssessment assessment = audioAnalysisService.analyzeAudioForThreats(
                request.getAudioData(), 
                request.getUserId(),
                request.getLocation(),
                request.getTimestamp()
            );

            // Process emergency response if threat detected
            AutonomousEmergencyEventProcessor.EmergencyProcessingResult emergencyResult = null;
            if (assessment.getThreatLevel().equals("HIGH") || assessment.getThreatLevel().equals("CRITICAL")) {
                emergencyResult = emergencyProcessor.processEmergencyFromThreatAssessment(assessment);
            }

            AudioAnalysisResponse response = AudioAnalysisResponse.builder()
                    .assessmentId(assessment.getAssessmentId())
                    .threatLevel(assessment.getThreatLevel())
                    .confidenceScore(assessment.getConfidenceScore())
                    .llmReasoning(assessment.getLlmReasoning())
                    .audioFeatures(assessment.getAudioFeatures())
                    .emergencyTriggered(emergencyResult != null && emergencyResult.isSuccess())
                    .emergencyEventId(emergencyResult != null ? emergencyResult.getEmergencyEventId() : null)
                    .processingTimeMs(assessment.getProcessingTimeMs())
                    .success(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AudioAnalysisResponse.builder()
                            .success(false)
                            .errorMessage("Audio analysis failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Start continuous audio monitoring session
     */
    @PostMapping("/start-monitoring")
    public ResponseEntity<Map<String, Object>> startAudioMonitoring(
            @RequestBody AudioMonitoringRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate user consent
            if (!request.isConsentGiven()) {
                response.put("success", false);
                response.put("error", "User consent required for audio monitoring");
                return ResponseEntity.badRequest().body(response);
            }

            // Create monitoring session
            String sessionId = "AUDIO-SESSION-" + System.currentTimeMillis();
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("monitoringActive", true);
            response.put("audioSettings", Map.of(
                "sampleRate", 16000,
                "channels", 1,
                "chunkDurationMs", 3000,
                "compressionFormat", "webm"
            ));
            response.put("message", "Audio monitoring started successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to start audio monitoring: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Stop audio monitoring session
     */
    @PostMapping("/stop-monitoring")
    public ResponseEntity<Map<String, Object>> stopAudioMonitoring(
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sessionId = request.get("sessionId");
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("monitoringActive", false);
            response.put("message", "Audio monitoring stopped successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to stop audio monitoring: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Test audio processing with sample data
     */
    @PostMapping("/test")
    public ResponseEntity<AudioAnalysisResponse> testAudioProcessing() {
        try {
            // Simulate emergency audio scenario
            String simulatedAudioData = "SIMULATED_DISTRESS_AUDIO_DATA";
            
            ThreatAssessment assessment = audioAnalysisService.analyzeAudioForThreats(
                simulatedAudioData,
                "test-user-123",
                "42.3601° N, 71.0589° W",
                Instant.now()
            );

            AudioAnalysisResponse response = AudioAnalysisResponse.builder()
                    .assessmentId(assessment.getAssessmentId())
                    .threatLevel(assessment.getThreatLevel())
                    .confidenceScore(assessment.getConfidenceScore())
                    .llmReasoning(assessment.getLlmReasoning())
                    .audioFeatures(assessment.getAudioFeatures())
                    .emergencyTriggered(assessment.getThreatLevel().equals("HIGH"))
                    .processingTimeMs(assessment.getProcessingTimeMs())
                    .success(true)
                    .testMode(true)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AudioAnalysisResponse.builder()
                            .success(false)
                            .errorMessage("Audio test failed: " + e.getMessage())
                            .build());
        }
    }

    // Request/Response DTOs
    public static class AudioAnalysisRequest {
        private String audioData;
        private String userId;
        private String location;
        private Instant timestamp;
        private String audioFormat = "webm";
        private int sampleRate = 16000;

        // Getters and Setters
        public String getAudioData() { return audioData; }
        public void setAudioData(String audioData) { this.audioData = audioData; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public String getAudioFormat() { return audioFormat; }
        public void setAudioFormat(String audioFormat) { this.audioFormat = audioFormat; }
        public int getSampleRate() { return sampleRate; }
        public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
    }

    public static class AudioMonitoringRequest {
        private String userId;
        private boolean consentGiven;
        private String location;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public boolean isConsentGiven() { return consentGiven; }
        public void setConsentGiven(boolean consentGiven) { this.consentGiven = consentGiven; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class AudioAnalysisResponse {
        private String assessmentId;
        private String threatLevel;
        private double confidenceScore;
        private String llmReasoning;
        private String audioFeatures;
        private boolean emergencyTriggered;
        private String emergencyEventId;
        private long processingTimeMs;
        private boolean success;
        private boolean testMode = false;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private AudioAnalysisResponse response = new AudioAnalysisResponse();
            public Builder assessmentId(String assessmentId) { response.assessmentId = assessmentId; return this; }
            public Builder threatLevel(String threatLevel) { response.threatLevel = threatLevel; return this; }
            public Builder confidenceScore(double confidenceScore) { response.confidenceScore = confidenceScore; return this; }
            public Builder llmReasoning(String llmReasoning) { response.llmReasoning = llmReasoning; return this; }
            public Builder audioFeatures(String audioFeatures) { response.audioFeatures = audioFeatures; return this; }
            public Builder emergencyTriggered(boolean emergencyTriggered) { response.emergencyTriggered = emergencyTriggered; return this; }
            public Builder emergencyEventId(String emergencyEventId) { response.emergencyEventId = emergencyEventId; return this; }
            public Builder processingTimeMs(long processingTimeMs) { response.processingTimeMs = processingTimeMs; return this; }
            public Builder success(boolean success) { response.success = success; return this; }
            public Builder testMode(boolean testMode) { response.testMode = testMode; return this; }
            public Builder errorMessage(String errorMessage) { response.errorMessage = errorMessage; return this; }
            public AudioAnalysisResponse build() { return response; }
        }

        // Getters
        public String getAssessmentId() { return assessmentId; }
        public String getThreatLevel() { return threatLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getLlmReasoning() { return llmReasoning; }
        public String getAudioFeatures() { return audioFeatures; }
        public boolean isEmergencyTriggered() { return emergencyTriggered; }
        public String getEmergencyEventId() { return emergencyEventId; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public boolean isSuccess() { return success; }
        public boolean isTestMode() { return testMode; }
        public String getErrorMessage() { return errorMessage; }
    }
}