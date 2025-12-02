package com.allsenses.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Embeddable class representing a sensor reading associated with a threat assessment.
 */
@Embeddable
public class SensorReading {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false)
    private SensorType sensorType;

    @Column(name = "sensor_value", columnDefinition = "TEXT")
    private String sensorValue;

    @Column(name = "confidence_level", precision = 3, scale = 2)
    private Double confidenceLevel;

    @NotNull
    @Column(name = "reading_timestamp", nullable = false)
    private LocalDateTime readingTimestamp;

    @Column(name = "anomaly_detected")
    private Boolean anomalyDetected;

    // Constructors
    public SensorReading() {
        this.readingTimestamp = LocalDateTime.now();
        this.anomalyDetected = false;
    }

    public SensorReading(SensorType sensorType, String sensorValue, Double confidenceLevel) {
        this();
        this.sensorType = sensorType;
        this.sensorValue = sensorValue;
        this.confidenceLevel = confidenceLevel;
    }

    // Getters and Setters
    public SensorType getSensorType() {
        return sensorType;
    }

    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public String getSensorValue() {
        return sensorValue;
    }

    public void setSensorValue(String sensorValue) {
        this.sensorValue = sensorValue;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public LocalDateTime getReadingTimestamp() {
        return readingTimestamp;
    }

    public void setReadingTimestamp(LocalDateTime readingTimestamp) {
        this.readingTimestamp = readingTimestamp;
    }

    public Boolean getAnomalyDetected() {
        return anomalyDetected;
    }

    public void setAnomalyDetected(Boolean anomalyDetected) {
        this.anomalyDetected = anomalyDetected;
    }

    @Override
    public String toString() {
        return "SensorReading{" +
                "sensorType=" + sensorType +
                ", confidenceLevel=" + confidenceLevel +
                ", readingTimestamp=" + readingTimestamp +
                ", anomalyDetected=" + anomalyDetected +
                '}';
    }
}