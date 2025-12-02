package com.allsenses.ai.model;

/**
 * Enumeration representing different levels of threat severity
 * detected by the AI analysis engine.
 */
public enum ThreatLevel {
    /**
     * No threat detected - normal environmental conditions
     */
    NONE("No threat detected"),
    
    /**
     * Low-level anomaly detected - monitoring continues
     */
    LOW("Low-level anomaly detected"),
    
    /**
     * Medium threat level - requires attention and verification
     */
    MEDIUM("Medium threat level detected"),
    
    /**
     * High threat level - likely emergency situation
     */
    HIGH("High threat level detected"),
    
    /**
     * Critical threat level - immediate emergency response required
     */
    CRITICAL("Critical threat - immediate response required");

    private final String description;

    ThreatLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines if this threat level requires emergency response
     */
    public boolean requiresEmergencyResponse() {
        return this == HIGH || this == CRITICAL;
    }

    /**
     * Determines if this threat level requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return this == CRITICAL;
    }

    /**
     * Gets the minimum confidence score required for this threat level
     */
    public double getMinimumConfidenceThreshold() {
        return switch (this) {
            case NONE -> 0.0;
            case LOW -> 0.3;
            case MEDIUM -> 0.5;
            case HIGH -> 0.7;
            case CRITICAL -> 0.8;
        };
    }
}