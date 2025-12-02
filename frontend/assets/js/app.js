// AllSenses AI Guardian - Main Application Logic

class AllSensesApp {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api/v1';
        this.agentStatus = 'initializing';
        this.statistics = {
            totalDecisions: 0,
            threatDetections: 0,
            emergencyResponses: 0,
            avgResponseTime: 0
        };
        
        this.init();
    }

    async init() {
        console.log('Initializing AllSenses AI Guardian Dashboard...');
        
        // Initialize UI components
        this.initializeStatusIndicators();
        this.setupEventListeners();
        
        // Check system status
        await this.checkSystemStatus();
        
        // Start periodic status updates
        this.startStatusUpdates();
        
        console.log('Dashboard initialization complete');
    }

    initializeStatusIndicators() {
        const statusDot = document.getElementById('agentStatus');
        const statusText = document.getElementById('agentStatusText');
        
        if (statusDot && statusText) {
            statusDot.className = 'status-dot';
            statusText.textContent = 'Initializing AI Agent...';
        }
    }

    setupEventListeners() {
        // Test buttons
        const testThreatBtn = document.getElementById('testThreatDetection');
        const testEmergencyBtn = document.getElementById('testEmergencyResponse');
        const testWorkflowBtn = document.getElementById('testCompleteWorkflow');

        if (testThreatBtn) {
            testThreatBtn.addEventListener('click', () => this.testThreatDetection());
        }
        
        if (testEmergencyBtn) {
            testEmergencyBtn.addEventListener('click', () => this.testEmergencyResponse());
        }
        
        if (testWorkflowBtn) {
            testWorkflowBtn.addEventListener('click', () => this.testCompleteWorkflow());
        }
    }

    async checkSystemStatus() {
        try {
            this.updateAgentStatus('checking', 'Checking system status...');
            
            // Check each component
            const llmStatus = await this.checkLLMIntegration();
            const awsStatus = await this.checkAWSServices();
            const autonomousStatus = await this.checkAutonomousCapabilities();
            
            // Update UI
            this.updateCapabilityStatus('llmStatus', llmStatus);
            this.updateCapabilityStatus('awsStatus', awsStatus);
            this.updateCapabilityStatus('autonomousStatus', autonomousStatus);
            
            // Update overall status
            if (llmStatus.success && awsStatus.success && autonomousStatus.success) {
                this.updateAgentStatus('active', 'AI Agent Operational');
                this.logDecision('System Status', 'All systems operational. AI Agent ready for autonomous operations.', 'success');
            } else {
                this.updateAgentStatus('error', 'System Issues Detected');
                this.logDecision('System Status', 'Some systems are not operational. Check individual components.', 'error');
            }
            
        } catch (error) {
            console.error('System status check failed:', error);
            this.updateAgentStatus('error', 'System Check Failed');
            this.logDecision('System Status', `System check failed: ${error.message}`, 'error');
        }
    }

    async checkLLMIntegration() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/threat-detection/test/llm-integration`);
            const result = await response.json();
            
            this.updateIntegrationStatus('bedrockIntegration', result.success);
            
            return {
                success: result.success,
                message: result.success ? 'AWS Bedrock LLM Connected' : 'LLM Integration Failed'
            };
        } catch (error) {
            this.updateIntegrationStatus('bedrockIntegration', false);
            return { success: false, message: 'LLM Connection Error' };
        }
    }

    async checkAWSServices() {
        try {
            // Check multiple AWS services
            const promises = [
                this.checkService('users/statistics', 'dynamoIntegration'),
                this.checkService('emergency-events/statistics', 'lambdaIntegration'),
                this.checkService('threat-detection/test/sns-integration', 'snsIntegration')
            ];
            
            const results = await Promise.all(promises);
            const allSuccess = results.every(r => r.success);
            
            return {
                success: allSuccess,
                message: allSuccess ? 'All AWS Services Connected' : 'Some AWS Services Unavailable'
            };
        } catch (error) {
            return { success: false, message: 'AWS Services Check Failed' };
        }
    }

    async checkService(endpoint, integrationId) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/${endpoint}`);
            const success = response.ok;
            this.updateIntegrationStatus(integrationId, success);
            return { success };
        } catch (error) {
            this.updateIntegrationStatus(integrationId, false);
            return { success: false };
        }
    }

    async checkAutonomousCapabilities() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/comprehensive-agent/test/autonomous-capabilities`);
            const result = await response.json();
            
            return {
                success: result.autonomous_capabilities === 'ACTIVE',
                message: result.autonomous_capabilities === 'ACTIVE' ? 
                    'Autonomous Capabilities Active' : 'Autonomous Processing Unavailable'
            };
        } catch (error) {
            return { success: false, message: 'Autonomous Capabilities Check Failed' };
        }
    }

    updateAgentStatus(status, message) {
        const statusDot = document.getElementById('agentStatus');
        const statusText = document.getElementById('agentStatusText');
        
        if (statusDot && statusText) {
            statusDot.className = `status-dot ${status}`;
            statusText.textContent = message;
        }
        
        this.agentStatus = status;
    }

    updateCapabilityStatus(elementId, status) {
        const element = document.getElementById(elementId);
        if (element) {
            element.className = `status ${status.success ? 'success' : 'error'}`;
            element.textContent = status.message;
        }
    }

    updateIntegrationStatus(elementId, connected) {
        const element = document.getElementById(elementId);
        if (element) {
            element.className = `integration-status ${connected ? 'connected' : 'error'}`;
            const statusText = element.querySelector('span:last-child');
            if (statusText) {
                statusText.textContent = connected ? 'Connected' : 'Disconnected';
            }
        }
    }

    logDecision(source, message, type = 'info') {
        const logContainer = document.getElementById('decisionsLog');
        if (!logContainer) return;

        const timestamp = new Date().toLocaleTimeString();
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${type}`;
        logEntry.innerHTML = `
            <span class="timestamp">${timestamp}</span>
            <span class="message">[${source}] ${message}</span>
        `;

        logContainer.insertBefore(logEntry, logContainer.firstChild);

        // Keep only last 50 entries
        while (logContainer.children.length > 50) {
            logContainer.removeChild(logContainer.lastChild);
        }

        // Scroll to top
        logContainer.scrollTop = 0;
    }

    updateStatistics(newStats) {
        Object.assign(this.statistics, newStats);
        
        const elements = {
            totalDecisions: document.getElementById('totalDecisions'),
            threatDetections: document.getElementById('threatDetections'),
            emergencyResponses: document.getElementById('emergencyResponses'),
            avgResponseTime: document.getElementById('avgResponseTime')
        };

        Object.entries(elements).forEach(([key, element]) => {
            if (element) {
                const value = key === 'avgResponseTime' ? 
                    `${this.statistics[key]}ms` : this.statistics[key];
                element.textContent = value;
                element.parentElement.classList.add('update-animation');
                setTimeout(() => {
                    element.parentElement.classList.remove('update-animation');
                }, 500);
            }
        });
    }

    startStatusUpdates() {
        // Update statistics every 30 seconds
        setInterval(() => {
            this.updateStatisticsFromAPI();
        }, 30000);
    }

    async updateStatisticsFromAPI() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/comprehensive-agent/statistics`);
            if (response.ok) {
                const stats = await response.json();
                this.updateStatistics({
                    totalDecisions: stats.total_decisions || this.statistics.totalDecisions,
                    threatDetections: stats.threat_detections || this.statistics.threatDetections,
                    emergencyResponses: stats.emergency_responses || this.statistics.emergencyResponses,
                    avgResponseTime: stats.avg_response_time || this.statistics.avgResponseTime
                });
            }
        } catch (error) {
            console.warn('Failed to update statistics:', error);
        }
    }

    // Test methods will be implemented in dashboard.js
    async testThreatDetection() {
        // Implemented in dashboard.js
    }

    async testEmergencyResponse() {
        // Implemented in dashboard.js
    }

    async testCompleteWorkflow() {
        // Implemented in dashboard.js
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.allSensesApp = new AllSensesApp();
});