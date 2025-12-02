// Dashboard-specific functionality for testing and visualization

class DashboardController {
    constructor(app) {
        this.app = app;
        this.isTestingInProgress = false;
        this.reasoningSteps = ['step1', 'step2', 'step3', 'step4'];
    }

    async testThreatDetection() {
        if (this.isTestingInProgress) return;
        
        this.isTestingInProgress = true;
        this.app.logDecision('Threat Detection Test', 'Starting autonomous threat detection test...', 'info');
        
        try {
            // Animate reasoning process
            this.animateReasoningProcess();
            
            // Call threat detection API
            const response = await fetch(`${this.app.apiBaseUrl}/threat-detection/test/autonomous-analysis`);
            const result = await response.json();
            
            if (result.success) {
                this.app.logDecision('LLM Analysis', `Threat Level: ${result.threat_level} (Confidence: ${result.confidence_score})`, 'success');
                this.app.logDecision('LLM Reasoning', result.llm_reasoning || 'Autonomous threat assessment completed', 'info');
                
                // Update statistics
                this.app.updateStatistics({
                    totalDecisions: this.app.statistics.totalDecisions + 1,
                    threatDetections: this.app.statistics.threatDetections + 1,
                    avgResponseTime: result.processing_time_ms || 150
                });
            } else {
                this.app.logDecision('Threat Detection', `Test failed: ${result.error_message}`, 'error');
            }
            
        } catch (error) {
            this.app.logDecision('Threat Detection', `Test error: ${error.message}`, 'error');
        } finally {
            this.isTestingInProgress = false;
            this.resetReasoningAnimation();
        }
    }

    async testEmergencyResponse() {
        if (this.isTestingInProgress) return;
        
        this.isTestingInProgress = true;
        this.app.logDecision('Emergency Response Test', 'Testing autonomous emergency response system...', 'info');
        
        try {
            // Animate reasoning process
            this.animateReasoningProcess();
            
            // Call emergency response API
            const response = await fetch(`${this.app.apiBaseUrl}/emergency-events/test/decision-engine`);
            const result = await response.json();
            
            if (result.success) {
                const decision = result.final_decision;
                this.app.logDecision('Emergency Decision', `Priority: ${decision.priority_level}, Response: ${decision.response_type}`, 'success');
                this.app.logDecision('LLM Decision', decision.llm_reasoning || 'Autonomous emergency decision completed', 'info');
                
                // Test notifications
                await this.testNotificationSystem();
                
                // Update statistics
                this.app.updateStatistics({
                    totalDecisions: this.app.statistics.totalDecisions + 1,
                    emergencyResponses: this.app.statistics.emergencyResponses + 1,
                    avgResponseTime: result.processing_time_ms || 200
                });
            } else {
                this.app.logDecision('Emergency Response', `Test failed: ${result.error_message}`, 'error');
            }
            
        } catch (error) {
            this.app.logDecision('Emergency Response', `Test error: ${error.message}`, 'error');
        } finally {
            this.isTestingInProgress = false;
            this.resetReasoningAnimation();
        }
    }

    async testCompleteWorkflow() {
        if (this.isTestingInProgress) return;
        
        this.isTestingInProgress = true;
        this.app.logDecision('Complete Workflow Test', 'Testing end-to-end AI agent workflow...', 'info');
        
        try {
            // Animate reasoning process
            this.animateReasoningProcess();
            
            // Call complete workflow API
            const response = await fetch(`${this.app.apiBaseUrl}/emergency-events/test/complete-workflow`);
            const result = await response.json();
            
            if (result.workflow_status === 'FULLY_OPERATIONAL') {
                this.app.logDecision('Workflow Status', 'All systems operational - AI Agent fully qualified', 'success');
                this.app.logDecision('LLM Integration', `Status: ${result.llm_integration}`, 'success');
                this.app.logDecision('Autonomous Capabilities', `Status: ${result.autonomous_capabilities}`, 'success');
                
                // Log individual component results
                if (result.emergency_decision) {
                    this.app.logDecision('Emergency Decision', `Success: ${result.emergency_decision.success}`, 'success');
                }
                
                if (result.autonomous_processing) {
                    this.app.logDecision('Autonomous Processing', `Actions: ${result.autonomous_processing.actions_executed}`, 'success');
                }
                
                if (result.sns_notifications) {
                    this.app.logDecision('SNS Notifications', `Sent: ${result.sns_notifications.notifications_sent}`, 'success');
                }
                
                // Update statistics
                this.app.updateStatistics({
                    totalDecisions: this.app.statistics.totalDecisions + 3,
                    threatDetections: this.app.statistics.threatDetections + 1,
                    emergencyResponses: this.app.statistics.emergencyResponses + 1,
                    avgResponseTime: 180
                });
                
            } else {
                this.app.logDecision('Workflow Status', `Status: ${result.workflow_status}`, 'warning');
                if (result.error_message) {
                    this.app.logDecision('Workflow Error', result.error_message, 'error');
                }
            }
            
        } catch (error) {
            this.app.logDecision('Complete Workflow', `Test error: ${error.message}`, 'error');
        } finally {
            this.isTestingInProgress = false;
            this.resetReasoningAnimation();
        }
    }

    async testNotificationSystem() {
        try {
            const response = await fetch(`${this.app.apiBaseUrl}/threat-detection/test/sns-integration`);
            const result = await response.json();
            
            if (result.success) {
                this.app.logDecision('SNS Notifications', `Notifications sent: ${result.total_notifications_sent}`, 'success');
            } else {
                this.app.logDecision('SNS Notifications', 'Notification test failed', 'warning');
            }
        } catch (error) {
            this.app.logDecision('SNS Notifications', 'Notification system unavailable', 'warning');
        }
    }

    animateReasoningProcess() {
        this.reasoningSteps.forEach((stepId, index) => {
            const stepElement = document.getElementById(stepId);
            if (stepElement) {
                setTimeout(() => {
                    stepElement.classList.add('active');
                    
                    // Update step content based on the step
                    const contentElement = stepElement.querySelector('.step-content');
                    if (contentElement) {
                        switch (stepId) {
                            case 'step1':
                                contentElement.textContent = 'Processing multimodal sensor data...';
                                break;
                            case 'step2':
                                contentElement.textContent = 'LLM analyzing threat patterns...';
                                break;
                            case 'step3':
                                contentElement.textContent = 'Calculating confidence scores...';
                                break;
                            case 'step4':
                                contentElement.textContent = 'Making autonomous decision...';
                                break;
                        }
                    }
                    
                    // Mark as completed after a delay
                    setTimeout(() => {
                        stepElement.classList.remove('active');
                        stepElement.classList.add('completed');
                    }, 1500);
                    
                }, index * 800);
            }
        });
    }

    resetReasoningAnimation() {
        setTimeout(() => {
            this.reasoningSteps.forEach(stepId => {
                const stepElement = document.getElementById(stepId);
                if (stepElement) {
                    stepElement.classList.remove('active', 'completed');
                    
                    // Reset content
                    const contentElement = stepElement.querySelector('.step-content');
                    if (contentElement) {
                        switch (stepId) {
                            case 'step1':
                                contentElement.textContent = 'Analyzing multimodal sensor data...';
                                break;
                            case 'step2':
                                contentElement.textContent = 'LLM evaluating threat indicators...';
                                break;
                            case 'step3':
                                contentElement.textContent = 'Calculating confidence levels...';
                                break;
                            case 'step4':
                                contentElement.textContent = 'Autonomous decision execution...';
                                break;
                        }
                    }
                }
            });
        }, 2000);
    }
}

// Extend the main app with dashboard functionality
document.addEventListener('DOMContentLoaded', () => {
    // Wait for the main app to initialize
    setTimeout(() => {
        if (window.allSensesApp) {
            const dashboard = new DashboardController(window.allSensesApp);
            
            // Override test methods in the main app
            window.allSensesApp.testThreatDetection = () => dashboard.testThreatDetection();
            window.allSensesApp.testEmergencyResponse = () => dashboard.testEmergencyResponse();
            window.allSensesApp.testCompleteWorkflow = () => dashboard.testCompleteWorkflow();
            
            console.log('Dashboard controller initialized');
        }
    }, 100);
});