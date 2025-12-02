package com.allsenses.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * DynamoDB entity representing an emergency event triggered by autonomous AI agent decisions.
 * Tracks the complete lifecycle of an emergency response from detection to resolution.
 * 
 * This entity demonstrates Condition 3 (AI Agent Qualification) by storing
 * autonomous decision results and emergency response actions in AWS DynamoDB.
 */
@DynamoDbBean
public class EmergencyEvent {

    private String eventId;
    private String userId;
    private String assessmentId;
    private String eventStatus;
    private String priorityLevel;
    private ThreatAssessment.LocationData eventLocation;
    private Boolean emergencyServicesContacted;
    private Long emergencyServiceResponseTimeMs;
    private List<Map<String, String>> contactsNotified;
    private String voiceSampleReference;
    private String contextData;
    private String autonomousDecisionId;
    private String llmReasoningUsed;
    private Instant eventTimestamp;
    private Instant resolutionTimestamp;
    private String resolutionNotes;
    private Boolean falseAlarm;
    private Instant createdAt;
    private Instant updatedAt;

    public EmergencyEvent() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.eventTimestamp = Instant.now();
        this.emergencyServicesContacted = false;
        this.falseAlarm = false;
    }

    // Partition key
    @DynamoDbPartitionKey
    @DynamoDbAttribute("eventId")
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("assessmentId")
    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }

    @DynamoDbAttribute("eventStatus")
    public String getEventStatus() {
        return eventStatus;
    }

    public void setEventStatus(String eventStatus) {
        this.eventStatus = eventStatus;
    }

    @DynamoDbAttribute("priorityLevel")
    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    @DynamoDbAttribute("eventLocation")
    public ThreatAssessment.LocationData getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(ThreatAssessment.LocationData eventLocation) {
        this.eventLocation = eventLocation;
    }

    @DynamoDbAttribute("emergencyServicesContacted")
    public Boolean getEmergencyServicesContacted() {
        return emergencyServicesContacted;
    }

    public void setEmergencyServicesContacted(Boolean emergencyServicesContacted) {
        this.emergencyServicesContacted = emergencyServicesContacted;
    }

    @DynamoDbAttribute("emergencyServiceResponseTimeMs")
    public Long getEmergencyServiceResponseTimeMs() {
        return emergencyServiceResponseTimeMs;
    }

    public void setEmergencyServiceResponseTimeMs(Long emergencyServiceResponseTimeMs) {
        this.emergencyServiceResponseTimeMs = emergencyServiceResponseTimeMs;
    }

    @DynamoDbAttribute("contactsNotified")
    public List<Map<String, String>> getContactsNotified() {
        return contactsNotified;
    }

    public void setContactsNotified(List<Map<String, String>> contactsNotified) {
        this.contactsNotified = contactsNotified;
    }

    @DynamoDbAttribute("voiceSampleReference")
    public String getVoiceSampleReference() {
        return voiceSampleReference;
    }

    public void setVoiceSampleReference(String voiceSampleReference) {
        this.voiceSampleReference = voiceSampleReference;
    }

    @DynamoDbAttribute("contextData")
    public String getContextData() {
        return contextData;
    }

    public void setContextData(String contextData) {
        this.contextData = contextData;
    }

    @DynamoDbAttribute("autonomousDecisionId")
    public String getAutonomousDecisionId() {
        return autonomousDecisionId;
    }

    public void setAutonomousDecisionId(String autonomousDecisionId) {
        this.autonomousDecisionId = autonomousDecisionId;
    }

    @DynamoDbAttribute("llmReasoningUsed")
    public String getLlmReasoningUsed() {
        return llmReasoningUsed;
    }

    public void setLlmReasoningUsed(String llmReasoningUsed) {
        this.llmReasoningUsed = llmReasoningUsed;
    }

    @DynamoDbAttribute("eventTimestamp")
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @DynamoDbAttribute("resolutionTimestamp")
    public Instant getResolutionTimestamp() {
        return resolutionTimestamp;
    }

    public void setResolutionTimestamp(Instant resolutionTimestamp) {
        this.resolutionTimestamp = resolutionTimestamp;
    }

    @DynamoDbAttribute("resolutionNotes")
    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    @DynamoDbAttribute("falseAlarm")
    public Boolean getFalseAlarm() {
        return falseAlarm;
    }

    public void setFalseAlarm(Boolean falseAlarm) {
        this.falseAlarm = falseAlarm;
    }

    @DynamoDbAttribute("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("updatedAt")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public boolean isActive() {
        return "INITIATED".equals(eventStatus) || 
               "IN_PROGRESS".equals(eventStatus) ||
               "SERVICES_CONTACTED".equals(eventStatus);
    }

    public boolean isResolved() {
        return "RESOLVED".equals(eventStatus) || 
               "CANCELLED".equals(eventStatus);
    }

    public boolean requiresImmediateResponse() {
        return "CRITICAL".equals(priorityLevel) && isActive();
    }

    public void markAsResolved(String notes) {
        this.eventStatus = "RESOLVED";
        this.resolutionTimestamp = Instant.now();
        this.resolutionNotes = notes;
        this.updatedAt = Instant.now();
    }

    public void markAsFalseAlarm(String notes) {
        this.falseAlarm = true;
        this.eventStatus = "CANCELLED";
        this.resolutionTimestamp = Instant.now();
        this.resolutionNotes = notes;
        this.updatedAt = Instant.now();
    }

    public long getResponseTimeMs() {
        return emergencyServiceResponseTimeMs != null ? emergencyServiceResponseTimeMs : 0L;
    }

    public LocalDateTime getEventTimestampAsLocalDateTime() {
        return eventTimestamp != null ? LocalDateTime.ofInstant(eventTimestamp, ZoneOffset.UTC) : null;
    }

    public void setEventTimestampFromLocalDateTime(LocalDateTime localDateTime) {
        this.eventTimestamp = localDateTime != null ? localDateTime.toInstant(ZoneOffset.UTC) : null;
    }

    // Update timestamp on modification
    public void markAsUpdated() {
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "EmergencyEvent{" +
                "eventId='" + eventId + '\'' +
                ", userId='" + userId + '\'' +
                ", eventStatus='" + eventStatus + '\'' +
                ", priorityLevel='" + priorityLevel + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", emergencyServicesContacted=" + emergencyServicesContacted +
                ", autonomousDecisionId='" + autonomousDecisionId + '\'' +
                '}';
    }
}