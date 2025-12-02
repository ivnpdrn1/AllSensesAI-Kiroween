package com.allsenses.ai.model;

/**
 * Enumeration representing the processing status of a threat assessment.
 */
public enum AssessmentStatus {
    /**
     * Assessment is pending processing
     */
    PENDING("Assessment pending processing"),
    
    /**
     * Assessment is currently being processed by AI models
     */
    PROCESSING("Assessment in progress"),
    
    /**
     * Assessment completed successfully
     */
    COMPLETED("Assessment completed"),
    
    /**
     * Assessment confirmed as valid threat - emergency response triggered
     */
    CONFIRMED("Threat confirmed - emergency response initiated"),
    
    /**
     * Assessment determined to be false positive
     */
    FALSE_POSITIVE("Assessment marked as false positive"),
    
    /**
     * Assessment failed due to processing error
     */
    FAILED("Assessment processing failed"),
    
    /**
     * Assessment cancelled by user or system
     */
    CANCELLED("Assessment cancelled");

    private final String description;

    AssessmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determines if this status indicates the assessment is still active
     */
    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * Determines if this status indicates the assessment is complete
     */
    public boolean isComplete() {
        return this == COMPLETED || this == CONFIRMED || 
               this == FALSE_POSITIVE || this == FAILED || this == CANCELLED;
    }

    /**
     * Determines if this status indicates an emergency response was triggered
     */
    public boolean triggeredEmergencyResponse() {
        return this == CONFIRMED;
    }
}