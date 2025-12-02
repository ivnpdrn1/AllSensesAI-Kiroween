# AllSenses AI Guardian - Frontend Dashboard

This is the demonstration interface for the AllSenses AI Guardian, showcasing the autonomous AI agent capabilities built on AWS.

## Features

### 1. Main Dashboard (`index.html`)
- **Agent Status Overview**: Real-time status of the AI agent
- **Capability Testing**: Test threat detection, emergency response, and complete workflow
- **Performance Metrics**: Live statistics and agent performance data
- **AWS Integration Status**: Connection status for all AWS services

### 2. LLM Reasoning Visualization (`reasoning.html`)
- **Live Reasoning Process**: Step-by-step visualization of LLM decision making
- **Decision History**: Detailed logs of all autonomous decisions with LLM reasoning
- **Performance Metrics**: Response times, confidence distribution, accuracy gauges
- **Model Configuration**: Current LLM model settings and parameters

### 3. AWS Integration Dashboard (`aws-integration.html`)
- **AI Agent Qualification**: Status of the three required qualification conditions
- **Service Architecture**: Visual representation of AWS service integration
- **Workflow Visualization**: Complete agent workflow from data ingestion to response
- **Health Monitoring**: Real-time health metrics for all AWS services
- **Integration Testing**: Test individual services and complete workflows

## AI Agent Qualification

The dashboard demonstrates compliance with AWS AI Agent requirements:

1. **LLM Integration** ✅
   - AWS Bedrock Claude/Titan models for autonomous reasoning
   - Multi-stage decision making process
   - Real-time threat assessment capabilities

2. **AWS Services Integration** ✅
   - **AWS Bedrock**: Foundation models for LLM reasoning
   - **DynamoDB**: Data persistence for users, events, and assessments
   - **AWS Lambda**: Serverless processing capabilities
   - **AWS SNS**: Notification services for emergency alerts
   - **API Gateway**: REST API endpoints for agent interaction

3. **Autonomous Capabilities** ✅
   - Independent threat detection and assessment
   - Autonomous emergency response decisions
   - Multi-stage reasoning without human intervention
   - Database and external service integration

## How to Use

### Starting the Backend
1. Ensure the Spring Boot application is running on `localhost:8080`
2. All AWS services should be configured and accessible

### Accessing the Dashboard
1. Open `index.html` in a web browser
2. The dashboard will automatically check system status
3. Use the test buttons to demonstrate autonomous capabilities
4. Navigate between pages using the navigation links

### Testing Features
- **Test Threat Detection**: Demonstrates LLM-powered threat analysis
- **Test Emergency Response**: Shows autonomous emergency decision making
- **Test Complete Workflow**: Runs end-to-end agent workflow
- **View LLM Reasoning**: See detailed decision-making process
- **AWS Integration**: Monitor service health and test integrations

## Technical Architecture

### Frontend Stack
- **HTML5**: Modern web standards with semantic markup
- **CSS3**: Responsive design with Grid and Flexbox
- **Vanilla JavaScript**: No framework dependencies for maximum compatibility
- **Real-time Updates**: WebSocket-like polling for live data

### API Integration
- RESTful API calls to Spring Boot backend
- Real-time status monitoring
- Comprehensive error handling
- Mock data fallbacks for demonstration

### Responsive Design
- Mobile-friendly responsive layout
- Touch-friendly controls
- Optimized for various screen sizes
- Accessible design patterns

## File Structure

```
frontend/
├── index.html                 # Main dashboard
├── reasoning.html             # LLM reasoning visualization
├── aws-integration.html       # AWS service integration
├── assets/
│   ├── css/
│   │   ├── main.css          # Core styles
│   │   ├── dashboard.css     # Dashboard-specific styles
│   │   ├── reasoning.css     # Reasoning page styles
│   │   └── aws-integration.css # AWS integration styles
│   └── js/
│       ├── app.js            # Main application logic
│       ├── dashboard.js      # Dashboard functionality
│       ├── reasoning.js      # Reasoning visualization
│       └── aws-integration.js # AWS integration controller
└── README.md                 # This file
```

## Demonstration Scenarios

### Scenario 1: Normal Operation
- System shows all green status indicators
- Regular monitoring with no threats detected
- All AWS services healthy and connected

### Scenario 2: Threat Detection
- Simulated sensor data triggers threat analysis
- LLM reasoning process visualized in real-time
- Confidence scoring and threat level assessment

### Scenario 3: Emergency Response
- High-confidence threat triggers autonomous response
- Emergency decision engine activates
- Notification system sends alerts
- Complete workflow from detection to response

### Scenario 4: System Health Monitoring
- Real-time AWS service health monitoring
- Performance metrics and error tracking
- Service integration status updates

This dashboard provides a comprehensive demonstration of the AllSenses AI Guardian's autonomous capabilities and AWS integration, meeting all requirements for AI agent qualification.