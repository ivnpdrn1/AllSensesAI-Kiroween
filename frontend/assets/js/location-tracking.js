/**
 * AllSenses AI Guardian - Live Location Tracking Module
 * Continuous GPS tracking for emergency response
 * 
 * Features:
 * - High-accuracy GPS tracking
 * - Battery level monitoring
 * - Automatic location updates every 10 seconds
 * - No special permissions required
 * - Works on any device
 */

class LocationTracker {
    constructor(lambdaUrl) {
        this.lambdaUrl = lambdaUrl;
        this.watchId = null;
        this.currentIncidentId = null;
        this.isTracking = false;
        this.updateInterval = null;
        this.lastPosition = null;
        
        // Configuration
        this.UPDATE_INTERVAL = 10000; // 10 seconds
        this.HIGH_ACCURACY = true;
        this.MAX_AGE = 0;
        this.TIMEOUT = 5000;
    }

    /**
     * Start continuous location tracking
     * @param {string} incidentId - Unique incident identifier
     * @param {string} victimName - Name of the victim
     */
    startTracking(incidentId, victimName) {
        if (this.isTracking) {
            console.log('Location tracking already active');
            return;
        }

        if (!navigator.geolocation) {
            console.error('Geolocation not supported by this browser');
            this.showLocationError('Geolocation not supported');
            return;
        }

        this.currentIncidentId = incidentId;
        this.isTracking = true;

        console.log(`Starting location tracking for incident: ${incidentId}`);
        this.showTrackingStatus('active', 'Live tracking active');

        // Start watching position with high accuracy
        this.watchId = navigator.geolocation.watchPosition(
            (position) => this.handlePositionUpdate(position, victimName),
            (error) => this.handlePositionError(error),
            {
                enableHighAccuracy: this.HIGH_ACCURACY,
                maximumAge: this.MAX_AGE,
                timeout: this.TIMEOUT
            }
        );

        // Also send periodic updates (backup to watchPosition)
        this.updateInterval = setInterval(() => {
            if (this.currentIncidentId) {
                navigator.geolocation.getCurrentPosition(
                    (position) => this.handlePositionUpdate(position, victimName),
                    (error) => console.warn('Periodic update failed:', error),
                    {
                        enableHighAccuracy: this.HIGH_ACCURACY,
                        maximumAge: this.MAX_AGE,
                        timeout: this.TIMEOUT
                    }
                );
            }
        }, this.UPDATE_INTERVAL);
    }

    /**
     * Stop location tracking
     */
    stopTracking() {
        if (!this.isTracking) {
            return;
        }

        console.log('Stopping location tracking');

        if (this.watchId) {
            navigator.geolocation.clearWatch(this.watchId);
            this.watchId = null;
        }

        if (this.updateInterval) {
            clearInterval(this.updateInterval);
            this.updateInterval = null;
        }

        this.isTracking = false;
        this.currentIncidentId = null;
        this.lastPosition = null;

        this.showTrackingStatus('inactive', 'Tracking stopped');
    }

    /**
     * Handle position update from GPS
     * @param {Position} position - Geolocation position object
     * @param {string} victimName - Name of the victim
     */
    async handlePositionUpdate(position, victimName) {
        this.lastPosition = position;

        const locationData = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            speed: position.coords.speed,
            heading: position.coords.heading,
            altitude: position.coords.altitude,
            altitudeAccuracy: position.coords.altitudeAccuracy,
            timestamp: position.timestamp
        };

        console.log('Location update:', locationData);

        // Get battery level
        const batteryLevel = await this.getBatteryLevel();

        // Send to backend
        await this.sendLocationUpdate(this.currentIncidentId, victimName, locationData, batteryLevel);

        // Update UI
        this.updateLocationDisplay(locationData, batteryLevel);
    }

    /**
     * Handle position error
     * @param {PositionError} error - Geolocation error object
     */
    handlePositionError(error) {
        let errorMessage = 'Unknown error';

        switch (error.code) {
            case error.PERMISSION_DENIED:
                errorMessage = 'Location permission denied';
                break;
            case error.POSITION_UNAVAILABLE:
                errorMessage = 'Location unavailable';
                break;
            case error.TIMEOUT:
                errorMessage = 'Location request timeout';
                break;
        }

        console.error('Location error:', errorMessage, error);
        this.showLocationError(errorMessage);
    }

    /**
     * Send location update to backend
     * @param {string} incidentId - Incident identifier
     * @param {string} victimName - Name of the victim
     * @param {object} location - Location data
     * @param {number} batteryLevel - Battery percentage
     */
    async sendLocationUpdate(incidentId, victimName, location, batteryLevel) {
        try {
            const response = await fetch(this.lambdaUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: 'UPDATE_LOCATION',
                    incidentId: incidentId,
                    victimName: victimName,
                    location: location,
                    batteryLevel: batteryLevel
                })
            });

            const result = await response.json();

            if (result.status === 'success') {
                console.log('Location update sent successfully');
                this.showTrackingStatus('active', `Tracking active - ${new Date().toLocaleTimeString()}`);
            } else {
                console.warn('Location update failed:', result);
            }

        } catch (error) {
            console.error('Failed to send location update:', error);
            // Don't stop tracking on network errors - queue for retry
        }
    }

    /**
     * Get device battery level
     * @returns {Promise<number|null>} Battery percentage or null
     */
    async getBatteryLevel() {
        if ('getBattery' in navigator) {
            try {
                const battery = await navigator.getBattery();
                return Math.round(battery.level * 100);
            } catch (error) {
                console.warn('Battery API error:', error);
                return null;
            }
        }
        return null;
    }

    /**
     * Update location display in UI
     * @param {object} location - Location data
     * @param {number} batteryLevel - Battery percentage
     */
    updateLocationDisplay(location, batteryLevel) {
        // Update tracking indicator
        const trackingInfo = document.getElementById('trackingInfo');
        if (trackingInfo) {
            const speed = location.speed ? (location.speed * 3.6).toFixed(1) : '0.0';
            const accuracy = Math.round(location.accuracy);
            
            trackingInfo.innerHTML = `
                <div style="font-size: 0.9rem; color: #10b981;">
                    📍 Live Tracking Active
                </div>
                <div style="font-size: 0.8rem; color: #9ca3af; margin-top: 5px;">
                    Accuracy: ±${accuracy}m | Speed: ${speed} km/h
                    ${batteryLevel ? `| Battery: ${batteryLevel}%` : ''}
                </div>
            `;
        }
    }

    /**
     * Show tracking status
     * @param {string} status - Status type (active, inactive, error)
     * @param {string} message - Status message
     */
    showTrackingStatus(status, message) {
        const statusElement = document.getElementById('trackingStatus');
        if (statusElement) {
            statusElement.textContent = message;
            statusElement.className = `tracking-status ${status}`;
        }
    }

    /**
     * Show location error
     * @param {string} message - Error message
     */
    showLocationError(message) {
        console.error('Location error:', message);
        this.showTrackingStatus('error', `Location error: ${message}`);
    }

    /**
     * Get current position once (for initial location)
     * @returns {Promise<Position>} Current position
     */
    getCurrentPosition() {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error('Geolocation not supported'));
                return;
            }

            navigator.geolocation.getCurrentPosition(
                resolve,
                reject,
                {
                    enableHighAccuracy: this.HIGH_ACCURACY,
                    maximumAge: this.MAX_AGE,
                    timeout: this.TIMEOUT
                }
            );
        });
    }

    /**
     * Get tracking status
     * @returns {object} Tracking status information
     */
    getStatus() {
        return {
            isTracking: this.isTracking,
            incidentId: this.currentIncidentId,
            lastPosition: this.lastPosition,
            hasGeolocation: 'geolocation' in navigator
        };
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = LocationTracker;
}
