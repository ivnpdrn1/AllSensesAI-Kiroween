# AllSenses Technology Stack

## Cloud Infrastructure
- **AWS**: Primary cloud platform for scalability and managed services
- **AWS API Gateway**: API routing and management
- **AWS Lambda**: Serverless functions for real-time data processing
- **AWS Kinesis**: Real-time data streaming and analytics
- **AWS S3**: Temporary encrypted storage for voice samples and context data

## AI/ML Services
- **AWS Bedrock**: Foundation models for threat analysis and natural language processing
- **AWS SageMaker**: Custom machine learning models for threat detection
- **Multi-modal AI**: Audio analysis, motion pattern recognition, environmental context correlation

## Backend Services
- **Java 17**: Primary programming language for microservices
- **Spring Boot 3.2.0**: Microservices framework
- **Spring Cloud**: Service discovery and configuration management
- **Spring Security**: Authentication, authorization, and encryption
- **Maven**: Build system and dependency management

## Data Storage
- **AWS RDS (PostgreSQL)**: Structured data (user profiles, consent records, emergency events)
- **AWS DynamoDB**: Session management and real-time state tracking
- **AWS ElastiCache (Redis)**: High-performance caching for real-time processing
- **Encryption**: AES-256 for data at rest, TLS 1.3 for data in transit

## Frontend
- **Progressive Web App (PWA)**: Cross-platform user interface
- **HTML5**: Modern web standards and offline capabilities
- **CSS3**: Responsive design with Grid and Flexbox
- **Vanilla JavaScript (ES6+)**: Client-side logic without framework dependencies
- **WebSocket**: Real-time communication for status updates

## External Integrations
- **Emergency Services APIs**: 911 and regional emergency service integration
- **SMS/Voice Services**: Multi-channel communication for trusted contacts
- **Mapping Services**: GPS processing and location context enrichment
- **Encryption Services**: Key management and secure communication

## Development Tools
- **Docker**: Containerization for consistent deployment
- **AWS CloudFormation**: Infrastructure as Code
- **GitHub Actions**: CI/CD pipeline automation
- **SonarQube**: Code quality and security analysis

## Common Commands

### Backend Development
```bash
# Build all microservices
mvn clean compile

# Run specific service locally
mvn spring-boot:run -pl user-management-service

# Run tests
mvn test

# Package for deployment
mvn clean package

# Deploy to AWS
aws cloudformation deploy --template-file infrastructure.yaml
```

### Frontend Development
```bash
# Serve PWA locally
python -m http.server 8080

# Build for production
npm run build

# Test offline capabilities
npm run test:pwa
```

## Security Requirements
- All PII must be encrypted using AES-256
- API endpoints require JWT authentication
- Voice samples automatically deleted after emergency resolution
- Consent audit trails must be immutable
- Regular security scanning and penetration testing