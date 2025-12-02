package com.allsenses.ai.model;

/**
 * Enumeration representing different types of sensors that can provide data
 * for threat assessment analysis.
 */
public enum SensorType {
    /**
     * Audio sensor data - microphone input, sound analysis
     */
    AUDIO("Audio sensor data"),
    
    /**
     * Motion sensor data - accelerometer, gyroscope readings
     */
    MOTION("Motion sensor data"),
    
    /**
     * Environmental sensor data - temperature, humidity, air quality
     */
    ENVIRONMENTAL("Environmental sensor data"),
    
    /**
     * GPS location data
     */
    GPS("GPS location data"),
    
    /**
     * Heart rate and biometric data
     */
    BIOMETRIC("Biometric sensor data"),
    
    /**
     * Camera/visual sensor data
     */
    VISUAL("Visual sensor data"),
    
    /**
     * Proximity sensor data
     */
    PROXIMITY("Proximity sensor data");

    private final String description;

    SensorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines if this sensor type is considered high-priority for threat detection
     */
    public boolean isHighPriority() {
        return this == AUDIO || this == MOTION || this == BIOMETRIC;
    }

    /**
     * Determines if this sensor type requires privacy protection
     */
    public boolean requiresPrivacyProtection() {
        return this == AUDIO || this == VISUAL || this == BIOMETRIC || this == GPS;
    }
}