// LLM Reasoning Visualization Controller

class ReasoningController {
    constructor() {
        this.apiBaseUrl = 'http://localhost:8080/api/v1';
        this.decisionHistory = [];
        this.performanceMetrics = {
            responseTimes: [],
            confidenceDistribution: [0, 0, 0, 0, 0], // 0-20%, 20-40%, 40-60%, 60-80%, 80-100%
            accuracy: 95
        };
        
        this.init();
    }

    init() {
        console.log('Initializing LLM Reasoning Visualization...');
        
        this.setupEventListeners();
        this.loadDecisionHistory();
        this.updatePerformanceMetrics();
        this.startRealtimeUpdates();
        
        console.log('Reasoning visualization initialized');
    }

    setupEventListeners() {
        // Control buttons
        const clearLogsBtn = document.getElementById('clearLogs');
        const exportLogsBtn = document.getElementById('exportLogs');
        const logFilter = document.getElementById('logFilter');

        if (clearLogsBtn) {
            clearLogsBtn.addEventListener('click', () => this.clearLogs());
        }
        
        if (exportLogsBtn) {
            exportLogsBtn.addEventListener('click', () => this.exportLogs());
        }
        
        if (logFilter) {
            logFilter.addEventListener('change', (e) => this.filterLogs(e.target.value));
        }

        // Simulate reasoning process for demo
        this.simulateReasoningProcess();
    }

    async loadDecisionHistory() {
        try {
            // Load recent decisions from the API
            const response = await fetch(`${this.apiBaseUrl}/comprehensive-agent/decision-history`);
            if (response.ok) {
                const history = await response.json();
                this.decisionHistory = history.decisions || [];
            } else {
                // Use mock data for demonstration
                this.generateMockDecisionHistory();
            }
            
            this.renderDecisionLogs();
            
        } catch (error) {
            console.warn('Failed to load decision history, using mock data:', error);
            this.generateMockDecisionHistory();
            this.renderDecisionLogs();
        }
    }

    generateMockDecisionHistory() {
        const mockDecisions = [
            {
                id: 'DEC-001',
                timestamp: new Date(Date.now() - 300000).toISOString(),
                type: 'threat_detection',
                input: {
                    audio: 'Elevated voice patterns detected',
                    motion: 'Rapid movement detected',
                    location: 'Downtown area, 42.3601° N, 71.0589° W',
                    environment: 'Urban, evening, moderate noise'
                },
                llmReasoning: 'Analysis of audio patterns indicates potential distress. Voice stress analysis shows elevated pitch and rapid speech patterns consistent with anxiety or fear. Motion data shows sudden acceleration and erratic movement patterns. Location context suggests urban environment with potential safety concerns during evening hours.',
                threatLevel: 'MEDIUM',
                confidence: 0.78,
                action: 'MONITOR_CLOSELY',
                processingTime: 245,
                model: 'Claude-3 Sonnet'
            },
            {
                id: 'DEC-002',
                timestamp: new Date(Date.now() - 180000).toISOString(),
                type: 'emergency_response',
                input: {
                    audio: 'Distress call detected: "Help me"',
                    motion: 'Sudden stop, no movement for 30s',
                    location: 'Isolated area, 42.3598° N, 71.0592° W',
                    environment: 'Low light, minimal ambient noise'
                },
                llmReasoning: 'Clear distress vocalization detected with high confidence. Audio analysis confirms human voice saying "Help me" with emotional distress markers. Motion data shows sudden cessation of movement suggesting potential incapacitation. Location analysis indicates isolated area with limited visibility and foot traffic. Combination of factors strongly suggests genuine emergency situation requiring immediate response.',
                threatLevel: 'HIGH',
                confidence: 0.94,
                action: 'EMERGENCY_RESPONSE',
                processingTime: 189,
                model: 'Claude-3 Sonnet'
            },
            {
                id: 'DEC-003',
                timestamp: new Date(Date.now() - 60000).toISOString(),
                type: 'normal_operation',
                input: {
                    audio: 'Normal conversation patterns',
                    motion: 'Regular walking pace',
                    location: 'Residential area, 42.3605° N, 71.0585° W',
                    environment: 'Daylight, normal ambient noise'
                },
                llmReasoning: 'All sensor inputs indicate normal, safe conditions. Audio patterns show casual conversation with no stress indicators. Motion data consistent with normal walking behavior. Location is in well-populated residential area during daylight hours. No threat indicators detected across all monitored parameters.',
                threatLevel: 'NONE',
                confidence: 0.89,
                action: 'CONTINUE_MONITORING',
                processingTime: 156,
                model: 'Claude-3 Sonnet'
            }
        ];

        this.decisionHistory = mockDecisions;
    }

    renderDecisionLogs() {
        const logsContainer = document.getElementById('decisionLogs');
        if (!logsContainer) return;

        logsContainer.innerHTML = '';

        this.decisionHistory.forEach(decision => {
            const logEntry = this.createDetailedLogEntry(decision);
            logsContainer.appendChild(logEntry);
        });
    }

    createDetailedLogEntry(decision) {
        const entry = document.createElement('div');
        entry.className = `log-entry-detailed ${decision.type}`;
        
        const threatClass = decision.threatLevel.toLowerCase();
        const confidencePercent = Math.round(decision.confidence * 100);
        
        entry.innerHTML = `
            <div class="log-header">
                <span class="log-timestamp">${new Date(decision.timestamp).toLocaleString()}</span>
                <span class="log-type ${threatClass}">${decision.type.replace('_', ' ').toUpperCase()}</span>
            </div>
            
            <div class="log-content">
                <div class="log-section">
                    <strong>Input Data:</strong>
                    <div class="input-summary">
                        <div>🎤 Audio: ${decision.input.audio}</div>
                        <div>📱 Motion: ${decision.input.motion}</div>
                        <div>📍 Location: ${decision.input.location}</div>
                        <div>🌍 Environment: ${decision.input.environment}</div>
                    </div>
                </div>
                
                <div class="log-reasoning">
                    <strong>LLM Reasoning (${decision.model}):</strong><br>
                    ${decision.llmReasoning}
                </div>
                
                <div class="log-decision">
                    <strong>Decision:</strong> 
                    <span class="threat-level ${threatClass}">${decision.threatLevel}</span>
                    (${confidencePercent}% confidence) → ${decision.action}
                </div>
                
                <div class="log-metrics">
                    <span>Processing Time: ${decision.processingTime}ms</span>
                    <span>Decision ID: ${decision.id}</span>
                    <span>Model: ${decision.model}</span>
                </div>
            </div>
        `;

        return entry;
    }

    simulateReasoningProcess() {
        // Simulate a reasoning process every 30 seconds for demo
        setInterval(() => {
            this.runReasoningSimulation();
        }, 30000);

        // Run initial simulation after 5 seconds
        setTimeout(() => {
            this.runReasoningSimulation();
        }, 5000);
    }

    async runReasoningSimulation() {
        const stages = ['inputStage', 'analysisStage', 'decisionStage'];
        
        // Simulate input data
        this.updateInputData();
        
        // Animate through stages
        for (let i = 0; i < stages.length; i++) {
            const stage = document.getElementById(stages[i]);
            if (stage) {
                stage.classList.add('active');
                
                if (stages[i] === 'analysisStage') {
                    await this.simulateLLMThinking();
                }
                
                await this.delay(2000);
                
                stage.classList.remove('active');
                stage.classList.add('completed');
                
                await this.delay(500);
                
                if (i < stages.length - 1) {
                    stage.classList.remove('completed');
                }
            }
        }
        
        // Update decision output
        this.updateDecisionOutput();
        
        // Reset after delay
        setTimeout(() => {
            stages.forEach(stageId => {
                const stage = document.getElementById(stageId);
                if (stage) {
                    stage.classList.remove('active', 'completed');
                }
            });
        }, 5000);
    }

    updateInputData() {
        const mockInputs = [
            {
                audio: 'Normal conversation detected',
                motion: 'Walking at 3.2 mph',
                location: '42.3601° N, 71.0589° W',
                environment: 'Urban, daytime, moderate traffic'
            },
            {
                audio: 'Elevated voice patterns',
                motion: 'Rapid movement detected',
                location: '42.3598° N, 71.0592° W',
                environment: 'Isolated area, evening'
            },
            {
                audio: 'Distress vocalization detected',
                motion: 'Sudden stop, no movement',
                location: '42.3605° N, 71.0585° W',
                environment: 'Low light, minimal noise'
            }
        ];

        const input = mockInputs[Math.floor(Math.random() * mockInputs.length)];
        
        document.getElementById('audioInput').textContent = input.audio;
        document.getElementById('motionInput').textContent = input.motion;
        document.getElementById('locationInput').textContent = input.location;
        document.getElementById('environmentInput').textContent = input.environment;
    }

    async simulateLLMThinking() {
        const thinkingBubble = document.getElementById('llmThinking');
        const processingTime = document.getElementById('processingTime');
        
        if (thinkingBubble) {
            thinkingBubble.classList.add('active');
            
            const thoughts = [
                'Analyzing audio patterns for stress indicators...',
                'Correlating motion data with environmental context...',
                'Evaluating threat probability based on historical patterns...',
                'Calculating confidence scores across multiple factors...',
                'Determining appropriate response level...'
            ];
            
            for (let thought of thoughts) {
                thinkingBubble.textContent = thought;
                await this.delay(800);
            }
            
            thinkingBubble.classList.remove('active');
        }
        
        // Update processing time
        const time = 150 + Math.floor(Math.random() * 200);
        if (processingTime) {
            processingTime.textContent = `${time}ms`;
        }
    }

    updateDecisionOutput() {
        const scenarios = [
            { level: 'NONE', confidence: 0.89, action: 'CONTINUE_MONITORING' },
            { level: 'LOW', confidence: 0.65, action: 'INCREASED_MONITORING' },
            { level: 'MEDIUM', confidence: 0.78, action: 'ALERT_CONTACTS' },
            { level: 'HIGH', confidence: 0.94, action: 'EMERGENCY_RESPONSE' }
        ];

        const scenario = scenarios[Math.floor(Math.random() * scenarios.length)];
        
        const threatLevel = document.getElementById('threatLevel');
        const confidenceFill = document.getElementById('confidenceFill');
        const confidenceValue = document.getElementById('confidenceValue');
        const actionType = document.getElementById('actionType');

        if (threatLevel) {
            threatLevel.textContent = scenario.level;
            threatLevel.className = `threat-level ${scenario.level}`;
        }

        if (confidenceFill && confidenceValue) {
            const percentage = Math.round(scenario.confidence * 100);
            confidenceFill.style.width = `${percentage}%`;
            confidenceValue.textContent = `${percentage}%`;
        }

        if (actionType) {
            actionType.textContent = scenario.action;
        }

        // Update performance metrics
        this.updatePerformanceMetrics();
    }

    updatePerformanceMetrics() {
        // Update response time chart (simplified)
        const avgTime = document.getElementById('avgResponseTime');
        const maxTime = document.getElementById('maxResponseTime');
        
        if (avgTime) avgTime.textContent = '178ms';
        if (maxTime) maxTime.textContent = '245ms';

        // Update confidence histogram
        const distribution = [5, 12, 23, 35, 25]; // Mock percentages
        distribution.forEach((value, index) => {
            const bar = document.getElementById(`conf${index * 20}-${(index + 1) * 20}`);
            if (bar) {
                bar.style.height = `${value * 2}px`;
            }
        });

        // Update accuracy gauge
        const accuracyGauge = document.getElementById('accuracyGauge');
        const accuracyValue = document.getElementById('accuracyValue');
        
        if (accuracyGauge && accuracyValue) {
            const accuracy = 95;
            const degrees = (accuracy / 100) * 360;
            accuracyGauge.style.background = `conic-gradient(var(--success-color) 0deg, var(--success-color) ${degrees}deg, var(--border-color) ${degrees}deg)`;
            accuracyValue.textContent = `${accuracy}%`;
        }
    }

    filterLogs(filterType) {
        const logs = document.querySelectorAll('.log-entry-detailed');
        
        logs.forEach(log => {
            if (filterType === 'all') {
                log.style.display = 'block';
            } else {
                const logType = log.classList.contains(filterType);
                log.style.display = logType ? 'block' : 'none';
            }
        });
    }

    clearLogs() {
        const logsContainer = document.getElementById('decisionLogs');
        if (logsContainer) {
            logsContainer.innerHTML = '<div class="log-entry-detailed"><div class="log-content">Logs cleared</div></div>';
        }
        this.decisionHistory = [];
    }

    exportLogs() {
        const data = JSON.stringify(this.decisionHistory, null, 2);
        const blob = new Blob([data], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        
        const a = document.createElement('a');
        a.href = url;
        a.download = `allsenses-decision-logs-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    startRealtimeUpdates() {
        // Update metrics every 10 seconds
        setInterval(() => {
            this.updatePerformanceMetrics();
        }, 10000);
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Initialize reasoning controller when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.reasoningController = new ReasoningController();
});