# AllSenses Project Structure

## Root Level
```
├── pom.xml                           # Parent Maven configuration
├── docker-compose.yml               # Local development environment
├── infrastructure/                  # AWS CloudFormation templates
├── services/                        # Microservices modules
├── frontend/                        # Progressive Web App
├── docs/                           # API documentation and guides
└── .kiro/                          # Kiro IDE configuration
```

## Microservices Architecture (services/)
```
services/
├── user-management-service/         # User registration, consent, trusted contacts
│   ├── src/main/java/com/allsenses/user/
│   ├── src/main/resources/
│   └── pom.xml
├── data-ingestion-service/          # Sensor data collection and preprocessing
│   ├── src/main/java/com/allsenses/ingestion/
│   ├── src/main/resources/
│   └── pom.xml
├── ai-analysis-service/             # Threat detection and AI processing
│   ├── src/main/java/com/allsenses/ai/
│   ├── src/main/resources/
│   └── pom.xml
├── emergency-response-service/      # Emergency coordination and notifications
│   ├── src/main/java/com/allsenses/emergency/
│   ├── src/main/resources/
│   └── pom.xml
└── location-service/               # GPS processing and mapping
    ├── src/main/java/com/allsenses/location/
    ├── src/main/resources/
    └── pom.xml
```

## Frontend Structure (frontend/)
```
frontend/
├── index.html                      # Main PWA entry point
├── manifest.json                   # PWA configuration
├── service-worker.js              # Offline capabilities
├── assets/
│   ├── css/
│   │   ├── main.css               # Core styles
│   │   └── components.css         # Component-specific styles
│   ├── js/
│   │   ├── app.js                 # Main application logic
│   │   ├── api.js                 # Backend API integration
│   │   ├── emergency.js           # Emergency handling
│   │   └── consent.js             # Consent management
│   └── icons/                     # PWA icons and assets
└── pages/
    ├── dashboard.html             # User dashboard
    ├── settings.html              # Privacy and consent settings
    └── emergency-history.html     # Emergency event history
```

## Infrastructure as Code (infrastructure/)
```
infrastructure/
├── main.yaml                      # Master CloudFormation template
├── networking.yaml                # VPC, subnets, security groups
├── compute.yaml                   # ECS, Lambda, API Gateway
├── storage.yaml                   # RDS, DynamoDB, S3, ElastiCache
├── ai-ml.yaml                     # Bedrock, SageMaker configurations
└── monitoring.yaml                # CloudWatch, alerting, logging
```

## Configuration Files
- **pom.xml**: Maven parent configuration with shared dependencies
- **docker-compose.yml**: Local development environment with databases and message queues
- **application.yml**: Spring Boot configuration for each service
- **.kiro/**: IDE-specific configuration, specs, and steering rules

## Package Structure Convention
```
com.allsenses.{service}/
├── controller/                    # REST API endpoints
├── service/                       # Business logic
├── repository/                    # Data access layer
├── model/                         # JPA entities and DTOs
├── config/                        # Spring configuration
├── security/                      # Authentication and authorization
└── integration/                   # External service integrations
```

## Data Flow Architecture
1. **Ingestion**: Sensor data → Kinesis → Data Ingestion Service
2. **Processing**: Raw data → AI Analysis Service → Threat Assessment
3. **Response**: Confirmed threat → Emergency Response Service → 911 + Trusted Contacts
4. **Storage**: User data → RDS, Session data → DynamoDB, Cache → ElastiCache
5. **Frontend**: PWA ↔ API Gateway ↔ Microservices

## Development Environment
- Each microservice runs independently on different ports
- Local databases via Docker Compose
- AWS LocalStack for cloud service simulation
- Hot reload enabled for rapid development
- Comprehensive logging and monitoring