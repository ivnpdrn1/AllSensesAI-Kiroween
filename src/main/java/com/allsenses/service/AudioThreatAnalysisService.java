package com.allsenses.service;

import com.allsenses.model.ThreatAssessment;
import com.allsenses.repository.ThreatAssessmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Audio Threat Analysis Service for AllSenses AI Guardian
 * 
 * Processes audio data to detect distress signals and potential threats
 * using AWS Bedrock LLM models for autonomous threat assessment.
 */
@Service
public class AudioThreatAnalysisService {

    @Autowired
    private BedrockLlmService bedrockService;

    @Autowired
    private ThreatAssessmentRepository threatAssessmentRepository;

    /**
     * Analyze audio data for potential threats and distress signals
     */
    public ThreatAssessment analyzeAudioForThreats(String audioData, String userId, 
                                                  String location, Instant timestamp) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract audio features
            AudioFeatures features = extractAudioFeatures(audioData);
            
            // Simulate audio transcription (in real implementation, use AWS Transcribe)
            String transcript = simulateAudioTranscription(audioData);
            
            // Build LLM prompt for threat analysis
            String analysisPrompt = buildAudioThreatAnalysisPrompt(transcript, features, location);
            
            // Get LLM analysis using Bedrock
            BedrockLlmService.LlmAnalysisResult llmResult = bedrockService.analyzeThreatScenario(analysisPrompt);
            
            // Create threat assessment
            ThreatAssessment assessment = new ThreatAssessment();
            assessment.setAssessmentId("AUDIO-" + System.currentTimeMillis());
            assessment.setUserId(userId);
            assessment.setThreatLevel(determineThreatLevel(llmResult, features));
            assessment.setConfidenceScore(calculateConfidenceScore(llmResult, features));
            assessment.setLlmReasoning(llmResult.getReasoning());
            assessment.setAudioFeatures(features.toJsonString());
            assessment.setLocation(location);
            assessment.setTimestamp(timestamp);
            assessment.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            assessment.setDataSources("AUDIO");
            
            // Save assessment
            threatAssessmentRepository.save(assessment);
            
            return assessment;
            
        } catch (Exception e) {
            // Create error assessment
            ThreatAssessment errorAssessment = new ThreatAssessment();
            errorAssessment.setAssessmentId("AUDIO-ERROR-" + System.currentTimeMillis());
            errorAssessment.setUserId(userId);
            errorAssessment.setThreatLevel("UNKNOWN");
            errorAssessment.setConfidenceScore(0.0);
            errorAssessment.setLlmReasoning("Audio analysis failed: " + e.getMessage());
            errorAssessment.setLocation(location);
            errorAssessment.setTimestamp(timestamp);
            errorAssessment.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return errorAssessment;
        }
    }

    /**
     * Extract audio features for analysis
     */
    private AudioFeatures extractAudioFeatures(String audioData) {
        // Simulate audio feature extraction
        // In real implementation, this would use audio processing libraries
        
        AudioFeatures features = new AudioFeatures();
        
        // Simulate different audio scenarios based on input
        if (audioData.contains("DISTRESS") || audioData.contains("EMERGENCY")) {
            features.setVolumeLevel(85.0); // High volume
            features.setPitchVariation(0.8); // High pitch variation (stress indicator)
            features.setSpeechRate(2.5); // Fast speech rate
            features.setStressIndicators(0.9); // High stress
            features.setDistressVocalization(true);
            features.setBackgroundNoise(0.3);
        } else if (audioData.contains("HELP") || audioData.contains("DANGER")) {
            features.setVolumeLevel(75.0);
            features.setPitchVariation(0.7);
            features.setSpeechRate(2.0);
            features.setStressIndicators(0.8);
            features.setDistressVocalization(true);
            features.setBackgroundNoise(0.4);
        } else if (audioData.contains("NORMAL") || audioData.contains("CONVERSATION")) {
            features.setVolumeLevel(60.0);
            features.setPitchVariation(0.3);
            features.setSpeechRate(1.2);
            features.setStressIndicators(0.1);
            features.setDistressVocalization(false);
            features.setBackgroundNoise(0.2);
        } else {
            // Default simulation for demo
            features.setVolumeLevel(70.0 + (Math.random() * 20));
            features.setPitchVariation(0.4 + (Math.random() * 0.4));
            features.setSpeechRate(1.0 + (Math.random() * 1.5));
            features.setStressIndicators(Math.random() * 0.6);
            features.setDistressVocalization(Math.random() > 0.7);
            features.setBackgroundNoise(Math.random() * 0.5);
        }
        
        return features;
    }

    /**
     * Simulate audio transcription (replace with AWS Transcribe in production)
     */
    private String simulateAudioTranscription(String audioData) {
        // Simulate transcription based on audio data content
        if (audioData.contains("DISTRESS") || audioData.contains("EMERGENCY")) {
            return "Help me! Someone help! I'm in danger!";
        } else if (audioData.contains("HELP")) {
            return "Help! I need help right now!";
        } else if (audioData.contains("DANGER")) {
            return "This is dangerous, I need to get out of here!";
        } else if (audioData.contains("NORMAL")) {
            return "Hi, how are you doing today? Everything is fine here.";
        } else {
            // Simulate various scenarios for demo
            String[] scenarios = {
                "Help me please, something is wrong!",
                "I'm scared, can someone help me?",
                "Everything is fine, just talking normally",
                "Hey, what's going on over there?",
                "Call 911, there's an emergency!"
            };
            return scenarios[(int)(Math.random() * scenarios.length)];
        }
    }

    /**
     * Build LLM prompt for audio threat analysis
     */
    private String buildAudioThreatAnalysisPrompt(String transcript, AudioFeatures features, String location) {
        return String.format("""
            You are an AI safety system analyzing audio data for potential threats or distress situations.
            
            AUDIO TRANSCRIPT: "%s"
            
            AUDIO FEATURES:
            - Volume Level: %.1f dB
            - Pitch Variation: %.2f (0=stable, 1=highly variable)
            - Speech Rate: %.1f words/second
            - Stress Indicators: %.2f (0=calm, 1=highly stressed)
            - Distress Vocalization Detected: %s
            - Background Noise Level: %.2f
            
            LOCATION CONTEXT: %s
            
            ANALYSIS INSTRUCTIONS:
            1. Analyze the transcript for explicit distress calls, threats, or emergency situations
            2. Consider audio features that indicate stress, fear, or danger
            3. Evaluate the combination of vocal patterns and spoken content
            4. Consider location context for threat assessment
            
            Determine:
            - THREAT_LEVEL: NONE, LOW, MEDIUM, HIGH, CRITICAL
            - CONFIDENCE: 0.0 to 1.0 (how certain you are of the assessment)
            - REASONING: Detailed explanation of your analysis
            
            Respond in this format:
            THREAT_LEVEL: [level]
            CONFIDENCE: [score]
            REASONING: [detailed analysis of why you reached this conclusion, considering both transcript content and audio features]
            """, 
            transcript,
            features.getVolumeLevel(),
            features.getPitchVariation(),
            features.getSpeechRate(),
            features.getStressIndicators(),
            features.isDistressVocalization() ? "YES" : "NO",
            features.getBackgroundNoise(),
            location != null ? location : "Unknown location"
        );
    }

    /**
     * Determine threat level from LLM analysis and audio features
     */
    private String determineThreatLevel(BedrockLlmService.LlmAnalysisResult llmResult, AudioFeatures features) {
        String llmThreatLevel = extractThreatLevelFromResponse(llmResult.getReasoning());
        
        // Validate with audio features
        if (features.isDistressVocalization() && features.getStressIndicators() > 0.7) {
            // High stress + distress vocalization = at least MEDIUM threat
            if (llmThreatLevel.equals("NONE") || llmThreatLevel.equals("LOW")) {
                return "MEDIUM";
            }
        }
        
        return llmThreatLevel;
    }

    /**
     * Calculate confidence score combining LLM and audio feature analysis
     */
    private double calculateConfidenceScore(BedrockLlmService.LlmAnalysisResult llmResult, AudioFeatures features) {
        double llmConfidence = extractConfidenceFromResponse(llmResult.getReasoning());
        
        // Adjust confidence based on audio feature clarity
        double featureConfidence = 0.5; // Base confidence
        
        if (features.isDistressVocalization()) {
            featureConfidence += 0.3;
        }
        
        if (features.getStressIndicators() > 0.6) {
            featureConfidence += 0.2;
        }
        
        if (features.getVolumeLevel() > 80) {
            featureConfidence += 0.1;
        }
        
        // Combine LLM and feature confidence
        return Math.min(1.0, (llmConfidence + featureConfidence) / 2.0);
    }

    /**
     * Extract threat level from LLM response
     */
    private String extractThreatLevelFromResponse(String response) {
        if (response.contains("THREAT_LEVEL: CRITICAL")) return "CRITICAL";
        if (response.contains("THREAT_LEVEL: HIGH")) return "HIGH";
        if (response.contains("THREAT_LEVEL: MEDIUM")) return "MEDIUM";
        if (response.contains("THREAT_LEVEL: LOW")) return "LOW";
        return "NONE";
    }

    /**
     * Extract confidence score from LLM response
     */
    private double extractConfidenceFromResponse(String response) {
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.startsWith("CONFIDENCE:")) {
                    String confidenceStr = line.substring("CONFIDENCE:".length()).trim();
                    return Double.parseDouble(confidenceStr);
                }
            }
        } catch (Exception e) {
            // Default confidence if parsing fails
        }
        return 0.5;
    }

    /**
     * Audio Features data class
     */
    public static class AudioFeatures {
        private double volumeLevel;
        private double pitchVariation;
        private double speechRate;
        private double stressIndicators;
        private boolean distressVocalization;
        private double backgroundNoise;

        public String toJsonString() {
            Map<String, Object> features = new HashMap<>();
            features.put("volumeLevel", volumeLevel);
            features.put("pitchVariation", pitchVariation);
            features.put("speechRate", speechRate);
            features.put("stressIndicators", stressIndicators);
            features.put("distressVocalization", distressVocalization);
            features.put("backgroundNoise", backgroundNoise);
            
            return features.toString();
        }

        // Getters and Setters
        public double getVolumeLevel() { return volumeLevel; }
        public void setVolumeLevel(double volumeLevel) { this.volumeLevel = volumeLevel; }
        public double getPitchVariation() { return pitchVariation; }
        public void setPitchVariation(double pitchVariation) { this.pitchVariation = pitchVariation; }
        public double getSpeechRate() { return speechRate; }
        public void setSpeechRate(double speechRate) { this.speechRate = speechRate; }
        public double getStressIndicators() { return stressIndicators; }
        public void setStressIndicators(double stressIndicators) { this.stressIndicators = stressIndicators; }
        public boolean isDistressVocalization() { return distressVocalization; }
        public void setDistressVocalization(boolean distressVocalization) { this.distressVocalization = distressVocalization; }
        public double getBackgroundNoise() { return backgroundNoise; }
        public void setBackgroundNoise(double backgroundNoise) { this.backgroundNoise = backgroundNoise; }
    }
}