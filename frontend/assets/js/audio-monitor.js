// Audio Monitor Controller for AllSenses AI Guardian

class AudioMonitorController {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api/v1';
        this.isMonitoring = false;
        this.mediaRecorder = null;
        this.audioContext = null;
        this.analyser = null;
        this.dataArray = null;
        this.animationId = null;
        this.sessionId = null;
        this.analysisHistory = [];
        
        this.init();
    }

    init() {
        console.log('Initializing Audio Monitor...');
        
        this.setupEventListeners();
        this.initializeCanvas();
        
        console.log('Audio Monitor initialized');
    }

    setupEventListeners() {
        // Control buttons
        const startBtn = document.getElementById('startMonitoring');
        const stopBtn = document.getElementById('stopMonitoring');
        const consentCheckbox = document.getElementById('consentCheckbox');
        const clearHistoryBtn = document.getElementById('clearHistory');
        const historyFilter = document.getElementById('historyFilter');

        // Test buttons
        const testNormalBtn = document.getElementById('testNormal');
        const testDistressBtn = document.getElementById('testDistress');
        const testEmergencyBtn = document.getElementById('testEmergency');

        if (startBtn) {
            startBtn.addEventListener('click', () => this.startMonitoring());
        }
        
        if (stopBtn) {
            stopBtn.addEventListener('click', () => this.stopMonitoring());
        }
        
        if (consentCheckbox) {
            consentCheckbox.addEventListener('change', (e) => {
                const startBtn = document.getElementById('startMonitoring');
                if (startBtn) {
                    startBtn.disabled = !e.target.checked;
                }
            });
        }
        
        if (clearHistoryBtn) {
            clearHistoryBtn.addEventListener('click', () => this.clearHistory());
        }
        
        if (historyFilter) {
            historyFilter.addEventListener('change', (e) => this.filterHistory(e.target.value));
        }

        // Test scenario buttons
        if (testNormalBtn) {
            testNormalBtn.addEventListener('click', () => this.testScenario('NORMAL'));
        }
        
        if (testDistressBtn) {
            testDistressBtn.addEventListener('click', () => this.testScenario('DISTRESS'));
        }
        
        if (testEmergencyBtn) {
            testEmergencyBtn.addEventListener('click', () => this.testScenario('EMERGENCY'));
        }
    }

    initializeCanvas() {
        const canvas = document.getElementById('waveformCanvas');
        if (canvas) {
            this.canvas = canvas;
            this.canvasContext = canvas.getContext('2d');
            this.drawEmptyWaveform();
        }
    }

    async startMonitoring() {
        try {
            this.updateMonitoringStatus('starting', 'Starting audio monitoring...');
            
            // Check consent
            const consentCheckbox = document.getElementById('consentCheckbox');
            if (!consentCheckbox || !consentCheckbox.checked) {
                throw new Error('Consent required for audio monitoring');
            }

            // Request microphone permission
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    sampleRate: 16000,
                    channelCount: 1,
                    echoCancellation: true,
                    noiseSuppression: true
                }
            });

            // Start monitoring session with backend
            const sessionResponse = await fetch(`${this.apiBaseUrl}/audio/start-monitoring`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    userId: 'demo-user-123',
                    consentGiven: true,
                    location: '42.3601° N, 71.0589° W'
                })
            });

            if (!sessionResponse.ok) {
                throw new Error('Failed to start monitoring session');
            }

            const sessionData = await sessionResponse.json();
            this.sessionId = sessionData.sessionId;

            // Set up audio processing
            this.setupAudioProcessing(stream);
            
            // Update UI
            this.isMonitoring = true;
            this.updateMonitoringStatus('monitoring', 'Audio monitoring active');
            this.updateControlButtons();
            
            // Start continuous analysis
            this.startContinuousAnalysis();
            
            this.logHistory('System', 'Audio monitoring started successfully', 'success');

        } catch (error) {
            console.error('Failed to start monitoring:', error);
            this.updateMonitoringStatus('error', 'Failed to start monitoring');
            this.logHistory('System', `Failed to start monitoring: ${error.message}`, 'error');
        }
    }

    async stopMonitoring() {
        try {
            this.updateMonitoringStatus('stopping', 'Stopping audio monitoring...');
            
            // Stop audio processing
            if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
                this.mediaRecorder.stop();
            }
            
            if (this.audioContext) {
                this.audioContext.close();
            }
            
            if (this.animationId) {
                cancelAnimationFrame(this.animationId);
            }

            // Stop monitoring session with backend
            if (this.sessionId) {
                await fetch(`${this.apiBaseUrl}/audio/stop-monitoring`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        sessionId: this.sessionId
                    })
                });
            }

            // Update UI
            this.isMonitoring = false;
            this.sessionId = null;
            this.updateMonitoringStatus('ready', 'Ready to start');
            this.updateControlButtons();
            this.drawEmptyWaveform();
            this.resetMetrics();
            
            this.logHistory('System', 'Audio monitoring stopped', 'info');

        } catch (error) {
            console.error('Failed to stop monitoring:', error);
            this.logHistory('System', `Failed to stop monitoring: ${error.message}`, 'error');
        }
    }

    setupAudioProcessing(stream) {
        // Create audio context
        this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const source = this.audioContext.createMediaStreamSource(stream);
        
        // Create analyser for visualization
        this.analyser = this.audioContext.createAnalyser();
        this.analyser.fftSize = 2048;
        source.connect(this.analyser);
        
        const bufferLength = this.analyser.frequencyBinCount;
        this.dataArray = new Uint8Array(bufferLength);
        
        // Start visualization
        this.drawWaveform();
        
        // Set up media recorder for audio capture
        this.mediaRecorder = new MediaRecorder(stream, {
            mimeType: 'audio/webm'
        });
        
        let audioChunks = [];
        
        this.mediaRecorder.ondataavailable = (event) => {
            audioChunks.push(event.data);
        };
        
        this.mediaRecorder.onstop = async () => {
            if (audioChunks.length > 0) {
                const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
                await this.processAudioChunk(audioBlob);
                audioChunks = [];
            }
        };
    }

    startContinuousAnalysis() {
        if (!this.isMonitoring) return;
        
        // Record 3-second chunks for analysis
        if (this.mediaRecorder && this.mediaRecorder.state === 'inactive') {
            this.mediaRecorder.start();
            
            setTimeout(() => {
                if (this.mediaRecorder && this.mediaRecorder.state === 'recording') {
                    this.mediaRecorder.stop();
                }
                
                // Schedule next analysis
                setTimeout(() => {
                    this.startContinuousAnalysis();
                }, 1000); // 1 second gap between recordings
                
            }, 3000); // 3 second recording duration
        }
    }

    async processAudioChunk(audioBlob) {
        try {
            // Convert blob to base64 for transmission
            const audioData = await this.blobToBase64(audioBlob);
            
            // Send to backend for analysis
            const response = await fetch(`${this.apiBaseUrl}/audio/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    audioData: audioData,
                    userId: 'demo-user-123',
                    location: '42.3601° N, 71.0589° W',
                    timestamp: new Date().toISOString(),
                    audioFormat: 'webm',
                    sampleRate: 16000
                })
            });

            if (response.ok) {
                const result = await response.json();
                this.handleAnalysisResult(result);
            } else {
                console.warn('Audio analysis request failed');
            }

        } catch (error) {
            console.error('Failed to process audio chunk:', error);
        }
    }

    handleAnalysisResult(result) {
        // Update current analysis display
        this.updateAnalysisDisplay(result);
        
        // Update metrics
        this.updateAudioMetrics(result);
        
        // Check for emergency
        if (result.emergencyTriggered) {
            this.handleEmergencyDetected(result);
        }
        
        // Add to history
        this.addToHistory(result);
        
        // Log the analysis
        const logType = result.threatLevel === 'NONE' ? 'info' : 
                       result.threatLevel === 'HIGH' || result.threatLevel === 'CRITICAL' ? 'emergency' : 'threat';
        
        this.logHistory('AI Analysis', 
            `Threat: ${result.threatLevel} (${Math.round(result.confidenceScore * 100)}% confidence)`, 
            logType);
    }

    updateAnalysisDisplay(result) {
        const statusElement = document.getElementById('analysisStatus');
        const confidenceElement = document.getElementById('analysisConfidence');
        const processingTimeElement = document.getElementById('processingTime');
        const reasoningElement = document.getElementById('llmReasoning');

        if (statusElement) {
            statusElement.textContent = `${result.threatLevel} threat detected`;
        }
        
        if (confidenceElement) {
            confidenceElement.textContent = `${Math.round(result.confidenceScore * 100)}%`;
        }
        
        if (processingTimeElement) {
            processingTimeElement.textContent = `${result.processingTimeMs}ms`;
        }
        
        if (reasoningElement) {
            const reasoningText = reasoningElement.querySelector('p');
            if (reasoningText) {
                reasoningText.textContent = result.llmReasoning || 'No detailed reasoning available';
            }
        }
    }

    updateAudioMetrics(result) {
        // Parse audio features if available
        let features = {};
        try {
            if (result.audioFeatures) {
                // Simple parsing of the features string
                const featuresStr = result.audioFeatures;
                if (featuresStr.includes('volumeLevel')) {
                    const volumeMatch = featuresStr.match(/volumeLevel=([0-9.]+)/);
                    if (volumeMatch) features.volumeLevel = parseFloat(volumeMatch[1]);
                }
                if (featuresStr.includes('stressIndicators')) {
                    const stressMatch = featuresStr.match(/stressIndicators=([0-9.]+)/);
                    if (stressMatch) features.stressIndicators = parseFloat(stressMatch[1]);
                }
            }
        } catch (e) {
            console.warn('Failed to parse audio features:', e);
        }

        // Update volume meter
        const volumeFill = document.getElementById('volumeFill');
        const volumeValue = document.getElementById('volumeValue');
        if (volumeFill && volumeValue && features.volumeLevel) {
            const volumePercent = Math.min(100, (features.volumeLevel / 100) * 100);
            volumeFill.style.width = `${volumePercent}%`;
            volumeValue.textContent = `${Math.round(features.volumeLevel)} dB`;
        }

        // Update stress meter
        const stressFill = document.getElementById('stressFill');
        const stressValue = document.getElementById('stressValue');
        if (stressFill && stressValue && features.stressIndicators !== undefined) {
            const stressPercent = features.stressIndicators * 100;
            stressFill.style.width = `${stressPercent}%`;
            stressValue.textContent = `${Math.round(stressPercent)}%`;
        }

        // Update threat indicator
        const threatIndicator = document.getElementById('threatIndicator');
        if (threatIndicator) {
            threatIndicator.textContent = result.threatLevel;
            threatIndicator.className = `threat-indicator ${result.threatLevel}`;
        }
    }

    handleEmergencyDetected(result) {
        const emergencyIndicator = document.getElementById('emergencyIndicator');
        const emergencyDetails = document.getElementById('emergencyDetails');
        const emergencyEventId = document.getElementById('emergencyEventId');
        const responseActions = document.getElementById('responseActions');

        if (emergencyIndicator) {
            emergencyIndicator.className = 'emergency-indicator active';
            emergencyIndicator.innerHTML = `
                <span class="emergency-dot"></span>
                <span>EMERGENCY DETECTED</span>
            `;
        }

        if (emergencyDetails) {
            emergencyDetails.style.display = 'block';
        }

        if (emergencyEventId && result.emergencyEventId) {
            emergencyEventId.textContent = result.emergencyEventId;
        }

        if (responseActions) {
            responseActions.textContent = 'Emergency services contacted, trusted contacts notified';
        }

        // Log emergency
        this.logHistory('EMERGENCY', 
            `Emergency response triggered - Event ID: ${result.emergencyEventId}`, 
            'emergency');
    }

    drawWaveform() {
        if (!this.isMonitoring || !this.analyser) return;

        this.animationId = requestAnimationFrame(() => this.drawWaveform());

        this.analyser.getByteTimeDomainData(this.dataArray);

        const canvas = this.canvas;
        const canvasContext = this.canvasContext;
        const width = canvas.width;
        const height = canvas.height;

        canvasContext.fillStyle = '#1e293b';
        canvasContext.fillRect(0, 0, width, height);

        canvasContext.lineWidth = 2;
        canvasContext.strokeStyle = '#10b981';
        canvasContext.beginPath();

        const sliceWidth = width / this.dataArray.length;
        let x = 0;

        for (let i = 0; i < this.dataArray.length; i++) {
            const v = this.dataArray[i] / 128.0;
            const y = v * height / 2;

            if (i === 0) {
                canvasContext.moveTo(x, y);
            } else {
                canvasContext.lineTo(x, y);
            }

            x += sliceWidth;
        }

        canvasContext.lineTo(canvas.width, canvas.height / 2);
        canvasContext.stroke();

        // Update audio level display
        const audioLevelText = document.getElementById('audioLevelText');
        if (audioLevelText) {
            const average = this.dataArray.reduce((a, b) => a + b) / this.dataArray.length;
            const level = Math.round((average - 128) * 0.5);
            audioLevelText.textContent = `Audio Level: ${level} dB`;
        }
    }

    drawEmptyWaveform() {
        if (!this.canvas) return;

        const canvasContext = this.canvasContext;
        const width = this.canvas.width;
        const height = this.canvas.height;

        canvasContext.fillStyle = '#1e293b';
        canvasContext.fillRect(0, 0, width, height);

        canvasContext.lineWidth = 1;
        canvasContext.strokeStyle = '#475569';
        canvasContext.beginPath();
        canvasContext.moveTo(0, height / 2);
        canvasContext.lineTo(width, height / 2);
        canvasContext.stroke();
    }

    async testScenario(scenario) {
        try {
            this.logHistory('Test', `Testing ${scenario.toLowerCase()} scenario...`, 'info');
            
            // Send test request to backend
            const response = await fetch(`${this.apiBaseUrl}/audio/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    audioData: `SIMULATED_${scenario}_AUDIO_DATA`,
                    userId: 'demo-user-123',
                    location: '42.3601° N, 71.0589° W',
                    timestamp: new Date().toISOString(),
                    audioFormat: 'test',
                    sampleRate: 16000
                })
            });

            if (response.ok) {
                const result = await response.json();
                this.handleAnalysisResult(result);
            } else {
                throw new Error('Test scenario failed');
            }

        } catch (error) {
            console.error('Test scenario failed:', error);
            this.logHistory('Test', `Test scenario failed: ${error.message}`, 'error');
        }
    }

    updateMonitoringStatus(status, message) {
        const statusDot = document.getElementById('monitoringStatus');
        const statusText = document.getElementById('monitoringStatusText');

        if (statusDot && statusText) {
            statusDot.className = `status-dot ${status}`;
            statusText.textContent = message;
        }
    }

    updateControlButtons() {
        const startBtn = document.getElementById('startMonitoring');
        const stopBtn = document.getElementById('stopMonitoring');

        if (startBtn && stopBtn) {
            startBtn.disabled = this.isMonitoring;
            stopBtn.disabled = !this.isMonitoring;
        }
    }

    resetMetrics() {
        const volumeFill = document.getElementById('volumeFill');
        const volumeValue = document.getElementById('volumeValue');
        const stressFill = document.getElementById('stressFill');
        const stressValue = document.getElementById('stressValue');
        const threatIndicator = document.getElementById('threatIndicator');

        if (volumeFill) volumeFill.style.width = '0%';
        if (volumeValue) volumeValue.textContent = '0 dB';
        if (stressFill) stressFill.style.width = '0%';
        if (stressValue) stressValue.textContent = '0%';
        if (threatIndicator) {
            threatIndicator.textContent = 'NONE';
            threatIndicator.className = 'threat-indicator NONE';
        }

        // Reset analysis display
        const statusElement = document.getElementById('analysisStatus');
        const confidenceElement = document.getElementById('analysisConfidence');
        const processingTimeElement = document.getElementById('processingTime');

        if (statusElement) statusElement.textContent = 'Waiting for audio...';
        if (confidenceElement) confidenceElement.textContent = '0%';
        if (processingTimeElement) processingTimeElement.textContent = '0ms';

        // Reset emergency status
        const emergencyIndicator = document.getElementById('emergencyIndicator');
        const emergencyDetails = document.getElementById('emergencyDetails');

        if (emergencyIndicator) {
            emergencyIndicator.className = 'emergency-indicator';
            emergencyIndicator.innerHTML = `
                <span class="emergency-dot"></span>
                <span>No Emergency Detected</span>
            `;
        }

        if (emergencyDetails) {
            emergencyDetails.style.display = 'none';
        }
    }

    addToHistory(result) {
        this.analysisHistory.unshift({
            timestamp: new Date(),
            threatLevel: result.threatLevel,
            confidence: result.confidenceScore,
            reasoning: result.llmReasoning,
            emergency: result.emergencyTriggered
        });

        // Keep only last 50 entries
        if (this.analysisHistory.length > 50) {
            this.analysisHistory = this.analysisHistory.slice(0, 50);
        }
    }

    logHistory(source, message, type = 'info') {
        const historyLog = document.getElementById('historyLog');
        if (!historyLog) return;

        const timestamp = new Date().toLocaleTimeString();
        const logEntry = document.createElement('div');
        logEntry.className = `history-entry ${type}`;
        logEntry.innerHTML = `
            <span class="timestamp">${timestamp}</span>
            <span class="message">[${source}] ${message}</span>
        `;

        historyLog.insertBefore(logEntry, historyLog.firstChild);

        // Keep only last 100 entries
        while (historyLog.children.length > 100) {
            historyLog.removeChild(historyLog.lastChild);
        }
    }

    clearHistory() {
        const historyLog = document.getElementById('historyLog');
        if (historyLog) {
            historyLog.innerHTML = `
                <div class="history-entry">
                    <span class="timestamp">System</span>
                    <span class="message">History cleared</span>
                </div>
            `;
        }
        this.analysisHistory = [];
    }

    filterHistory(filterType) {
        const entries = document.querySelectorAll('.history-entry');
        
        entries.forEach(entry => {
            if (filterType === 'all') {
                entry.style.display = 'flex';
            } else {
                const hasClass = entry.classList.contains(filterType.slice(0, -1)); // Remove 's' from 'threats'/'emergencies'
                entry.style.display = hasClass ? 'flex' : 'none';
            }
        });
    }

    async blobToBase64(blob) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const base64 = reader.result.split(',')[1];
                resolve(base64);
            };
            reader.onerror = reject;
            reader.readAsDataURL(blob);
        });
    }
}

// Initialize audio monitor when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.audioMonitorController = new AudioMonitorController();
});