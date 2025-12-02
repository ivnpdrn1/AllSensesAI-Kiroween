package com.allsenses.ai.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Embeddable class representing location data associated with a threat assessment.
 * Includes GPS coordinates, accuracy information, and address details.
 */
@Embeddable
public class LocationData {

    @NotNull
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90 degrees")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90 degrees")
    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180 degrees")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180 degrees")
    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "altitude_meters")
    private Double altitudeMeters;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @NotNull
    @Column(name = "location_timestamp", nullable = false)
    private LocalDateTime locationTimestamp;

    @Column(name = "location_source", length = 50)
    private String locationSource;

    // Constructors
    public LocationData() {
        this.locationTimestamp = LocalDateTime.now();
    }

    public LocationData(Double latitude, Double longitude, Double accuracyMeters) {
        this();
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
    }

    // Business methods
    public boolean isHighAccuracy() {
        return accuracyMeters != null && accuracyMeters <= 10.0;
    }

    public boolean isValidCoordinates() {
        return latitude != null && longitude != null &&
               latitude >= -90.0 && latitude <= 90.0 &&
               longitude >= -180.0 && longitude <= 180.0;
    }

    public String getFormattedCoordinates() {
        if (!isValidCoordinates()) {
            return "Invalid coordinates";
        }
        return String.format("%.6f, %.6f", latitude, longitude);
    }

    // Getters and Setters
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(Double accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
    }

    public Double getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(Double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public LocalDateTime getLocationTimestamp() {
        return locationTimestamp;
    }

    public void setLocationTimestamp(LocalDateTime locationTimestamp) {
        this.locationTimestamp = locationTimestamp;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracyMeters=" + accuracyMeters +
                ", address='" + address + '\'' +
                ", locationTimestamp=" + locationTimestamp +
                '}';
    }
}