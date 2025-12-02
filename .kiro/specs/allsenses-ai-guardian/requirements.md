# AllSenses AI Guardian - Requirements Document

## Introduction

AllSenses is a real-time AI guardian system designed to protect vulnerable individuals by continuously monitoring environmental, behavioral, and contextual signals to detect potential danger or distress situations. The system operates on a privacy-first framework, automatically contacting emergency services and trusted contacts when threats are verified, while ensuring all actions are consent-based, encrypted, and temporary to maintain user trust and privacy.

The system leverages cloud-native architecture with AWS managed services, microservices design patterns, and advanced AI/ML capabilities to provide scalable, reliable, and accurate threat detection and emergency response.

## Glossary

- **AllSenses System**: The complete AI guardian platform including detection, analysis, and emergency response components
- **AWS End User Messaging (EUM)**: AWS service for compliant SMS delivery using 10DLC registration
- **10DLC**: 10-Digit Long Code - A phone number type registered for application-to-person (A2P) messaging
- **Brand Registration**: Verified business identity required for 10DLC SMS compliance (ID: AllSensesAI-Brand)
- **Campaign Registration**: Approved messaging use case for SMS delivery (ID: AllSensesAI-SafetyAlerts)
- **Originator Number**: The registered phone number used to send SMS messages (+1-217-393-3490)
- **Emergency Contact**: Pre-designated trusted individual who receives SMS alerts during emergencies
- **Threat Assessment**: AI-powered analysis determining danger level and emergency response requirements
- **SMS MessageId**: Unique identifier confirming successful SMS delivery via AWS SNS

## Requirements

### Requirement 1

**User Story:** As a vulnerable individual (rider, pedestrian, worker), I want an AI system to continuously monitor my environment for signs of danger, so that help can be automatically summoned when I'm unable to call for assistance myself.

#### Acceptance Criteria

1. WHEN the system is activated THEN it SHALL continuously monitor multimodal data streams including audio, motion, and environmental cues
2. WHEN environmental signals indicate potential danger THEN the system SHALL process and analyze the data in real-time using AI reasoning pipelines
3. WHEN multiple sensor inputs suggest distress THEN the system SHALL correlate the signals to assess threat level
4. IF the system detects anomalous patterns THEN it SHALL initiate verification protocols before escalating

### Requirement 2

**User Story:** As a user in distress, I want the system to automatically contact emergency services and my trusted contacts, so that help arrives quickly even when I cannot make the call myself.

#### Acceptance Criteria

1. WHEN danger is confirmed through AI analysis THEN the system SHALL automatically contact 911 or regional emergency services
2. WHEN emergency services are contacted THEN the system SHALL simultaneously alert predefined family or security contacts
3. WHEN contacting responders THEN the system SHALL transmit encrypted voice samples, environmental context, and precise GPS coordinates
4. IF emergency protocols vary by region THEN the system SHALL adapt to local emergency service procedures
5. WHEN alerts are sent THEN the system SHALL provide real-time location tracking for rapid intervention

### Requirement 3

**User Story:** As a privacy-conscious user, I want all system actions to be consent-based and encrypted, so that my safety never compromises my personal privacy and data security.

#### Acceptance Criteria

1. WHEN the system is first installed THEN it SHALL require explicit user consent for all monitoring and emergency features
2. WHEN data is collected THEN it SHALL be encrypted using industry-standard encryption protocols
3. WHEN voice samples are transmitted THEN they SHALL be encrypted and automatically deleted after emergency resolution
4. IF the user withdraws consent THEN the system SHALL immediately cease all monitoring and delete stored data
5. WHEN processing personal data THEN the system SHALL ensure all actions are temporary and purpose-limited

### Requirement 4

**User Story:** As a system administrator, I want the platform to be modular and scalable, so that it can integrate new sensor types and adapt to different deployment scenarios.

#### Acceptance Criteria

1. WHEN new sensor types become available THEN the system SHALL support integration without requiring core system changes
2. WHEN deploying in different regions THEN the system SHALL adapt to local emergency protocols and contact procedures
3. WHEN system load increases THEN the platform SHALL scale automatically using cloud-native architecture
4. IF new AI models become available THEN the system SHALL support integration from services like AWS Bedrock or SageMaker
5. WHEN processing multimodal data THEN the system SHALL maintain low latency and high availability

### Requirement 5

**User Story:** As an emergency responder, I want to receive comprehensive context about the emergency situation, so that I can respond appropriately and efficiently.

#### Acceptance Criteria

1. WHEN an emergency alert is triggered THEN responders SHALL receive precise GPS coordinates with accuracy within 10 meters
2. WHEN contextual data is available THEN the system SHALL provide environmental information relevant to the emergency
3. WHEN voice samples are captured THEN they SHALL be transmitted securely to help responders understand the situation
4. IF multiple data sources are available THEN the system SHALL prioritize and summarize the most critical information
5. WHEN emergency services respond THEN the system SHALL provide continuous updates until the situation is resolved

### Requirement 6

**User Story:** As a user, I want the system to minimize false alarms while maintaining high sensitivity to real threats, so that emergency resources are used appropriately and my trust in the system is maintained.

#### Acceptance Criteria

1. WHEN potential threats are detected THEN the system SHALL use multi-stage verification before triggering emergency protocols
2. WHEN confidence levels are below threshold THEN the system SHALL escalate through progressive alert levels rather than immediate emergency contact
3. IF environmental context suggests false alarm potential THEN the system SHALL require additional confirmation signals
4. WHEN machine learning models are updated THEN they SHALL be trained to reduce false positive rates while maintaining threat detection sensitivity
5. WHEN user feedback is available THEN the system SHALL incorporate it to improve future threat assessment accuracy

### Requirement 7 - Enhanced Emergency Detection

**User Story:** As a user in potential danger, I want the system to provide clear visual feedback that it's actively monitoring and to detect emergencies through both spoken words and sudden loud noises, so that I know the system is working and help will be summoned through multiple detection methods.

#### Acceptance Criteria

1. WHEN the system is actively monitoring THEN it SHALL display a clear visual listening indicator showing real-time monitoring status
2. WHEN emergency words are spoken THEN the system SHALL detect keywords including "help", "emergency", "danger", and "911" using continuous speech recognition
3. WHEN sudden loud noises occur THEN the system SHALL detect abrupt volume spikes above configurable thresholds using real-time audio analysis
4. WHEN either emergency words or abrupt noises are detected THEN the system SHALL immediately send real SMS notifications to designated emergency contacts
5. WHEN emergencies are detected THEN the system SHALL display full-screen visual emergency alerts with clear status information

### Requirement 8 - Compliant SMS Emergency Notification System

**User Story:** As an emergency contact, I want to receive immediate, compliant SMS notifications with detailed emergency information when someone I'm designated to help is in danger, so that I can respond quickly and appropriately with full regulatory compliance.

#### Acceptance Criteria

1. WHEN an emergency is detected THEN THE AllSenses System SHALL send real SMS messages via AWS End User Messaging within 3 seconds of detection
2. WHEN SMS notifications are sent THEN THE AllSenses System SHALL use the registered originator number +12173933490 for all outbound messages
3. WHEN SMS messages are transmitted THEN THE AllSenses System SHALL include victim name, detection type, danger status, geolocation link, incident ID, and timestamp
4. WHEN notifications are delivered THEN THE AllSenses System SHALL provide AWS SNS MessageId confirmation for delivery tracking and audit compliance
5. WHEN emergency contacts are configured THEN THE AllSenses System SHALL validate phone numbers and store notification preferences with consent tracking

### Requirement 9 - 10DLC SMS Compliance and Brand Registration

**User Story:** As a system administrator, I want the SMS notification system to be fully compliant with carrier regulations and AWS End User Messaging requirements, so that emergency messages are reliably delivered without being blocked or filtered as spam.

#### Acceptance Criteria

1. WHEN the system sends SMS messages THEN THE AllSenses System SHALL use the approved Brand Registration (ID: AllSensesAI-Brand) for all communications
2. WHEN SMS campaigns are executed THEN THE AllSenses System SHALL operate under the approved Campaign Registration (ID: AllSensesAI-SafetyAlerts, Type: US_TEN_DLC_CAMPAIGN_REGISTRATION)
3. WHEN originator numbers are assigned THEN THE AllSenses System SHALL use only the active, registered 10DLC number +12173933490 with verified SMS capabilities
4. WHEN compliance status changes THEN THE AllSenses System SHALL validate brand, campaign, and originator number status before sending messages
5. WHEN SMS delivery fails THEN THE AllSenses System SHALL log failure reasons and provide fallback notification mechanisms

### Requirement 10 - Real-Time Google Maps Live Location Tracking

**User Story:** As an emergency responder, I want to access a live-updating map showing the victim's real-time location and movement trail, so that I can track their position continuously and respond more effectively without requiring special permissions or account linking.

#### Acceptance Criteria

1. WHEN an emergency is detected THEN THE AllSenses System SHALL generate a unique tracking URL (track.allsensesai.com?incident=EMG-XXX) and include it in the emergency SMS
2. WHEN the tracking URL is accessed THEN THE AllSenses System SHALL display a Google Maps interface showing the victim's current location with automatic updates every 5-10 seconds
3. WHEN location updates are received THEN THE AllSenses System SHALL store location history in DynamoDB and display a movement trail on the map
4. WHEN displaying location data THEN THE AllSenses System SHALL show accuracy circles, battery level, speed, and last update timestamp
5. WHEN the victim's device sends location updates THEN THE AllSenses System SHALL continuously transmit GPS coordinates to the backend without requiring Google account authentication or special permissions
6. WHEN emergency contacts access the tracking page THEN THE AllSenses System SHALL provide a responsive, mobile-friendly interface that works on any device without app installation
7. WHEN location data is stored THEN THE AllSenses System SHALL automatically delete tracking data after 24 hours using DynamoDB TTL for privacy compliance
8. IF the tracking page cannot load Google Maps THEN THE AllSenses System SHALL provide a fallback display showing coordinates and a static map link