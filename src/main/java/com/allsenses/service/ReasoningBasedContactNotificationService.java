package com.allsenses.service;

import com.allsenses.model.User;
import com.allsenses.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Reasoning-Based Contact Notification Service for AllSenses AI Guardian
 * 
 * This service uses AWS Bedrock LLMs to make intelligent decisions about
 * which contacts to notify, when to notify them, and how to prioritize
 * notifications during emergency situations.
 */
@Service
public class ReasoningBasedContactNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ReasoningBasedContactNotificationService.class);

    @Autowired
    private BedrockLlmService bedrockLlmService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Make reasoning-based contact notification decisions
     */
    public ContactNotificationDecisionResult makeContactNotificationDecisions(
            ContactNotificationInput notificationInput) {
        
        logger.info("Making reasoning-based contact notification decisions for user: {}", 
                   notificationInput.getUserId());
        
        try {
            // Get user and trusted contacts
            Optional<User> userOpt = userRepository.findById(notificationInput.getUserId());
            if (userOpt.isEmpty()) {
                return createFailedDecisionResult("User not found: " + notificationInput.getUserId());
            }
            
            User user = userOpt.get();
            List<User.TrustedContact> trustedContacts = user.getTrustedContacts();
            
            if (trustedContacts == null || trustedContacts.isEmpty()) {
                return createNoContactsResult(notificationInput.getUserId());
            }
            
            // Analyze notification context using LLM
            NotificationContextAnalysis contextAnalysis = analyzeNotificationContext(notificationInput);
            
            // Prioritize contacts using LLM reasoning
            ContactPrioritizationResult prioritization = prioritizeContacts(
                trustedContacts, notificationInput, contextAnalysis);
            
            // Determine notification strategy
            NotificationStrategy strategy = determineNotificationStrategy(
                notificationInput, contextAnalysis, prioritization);
            
            // Create notification plan
            List<ContactNotificationPlan> notificationPlans = createNotificationPlans(
                prioritization.getPrioritizedContacts(), strategy, notificationInput);
            
            ContactNotificationDecisionResult result = ContactNotificationDecisionResult.builder()
                    .decisionId(generateDecisionId())
                    .userId(notificationInput.getUserId())
                    .contextAnalysis(contextAnalysis)
                    .contactPrioritization(prioritization)
                    .notificationStrategy(strategy)
                    .notificationPlans(notificationPlans)
                    .totalContactsToNotify(notificationPlans.size())
                    .decisionTimestamp(Instant.now())
                    .success(true)
                    .build();
            
            logger.info("Contact notification decisions completed. Contacts to notify: {}, Strategy: {}", 
                       notificationPlans.size(), strategy.getStrategyType());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error making contact notification decisions", e);
            return createFailedDecisionResult("Decision error: " + e.getMessage());
        }
    } 
   /**
     * Analyze notification context using LLM reasoning
     */
    private NotificationContextAnalysis analyzeNotificationContext(ContactNotificationInput input) {
        String prompt = buildContextAnalysisPrompt(input);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultContextAnalysis();
        }
        
        return parseContextAnalysis(llmResponse.getResponse());
    }

    /**
     * Prioritize contacts using LLM reasoning
     */
    private ContactPrioritizationResult prioritizeContacts(
            List<User.TrustedContact> contacts,
            ContactNotificationInput input,
            NotificationContextAnalysis contextAnalysis) {
        
        String prompt = buildContactPrioritizationPrompt(contacts, input, contextAnalysis);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultPrioritization(contacts);
        }
        
        return parseContactPrioritization(llmResponse.getResponse(), contacts);
    }

    /**
     * Determine notification strategy using LLM reasoning
     */
    private NotificationStrategy determineNotificationStrategy(
            ContactNotificationInput input,
            NotificationContextAnalysis contextAnalysis,
            ContactPrioritizationResult prioritization) {
        
        String prompt = buildNotificationStrategyPrompt(input, contextAnalysis, prioritization);
        
        BedrockLlmService.LlmReasoningResponse llmResponse = 
            bedrockLlmService.generateAutonomousReasoning(prompt);
        
        if (!llmResponse.isSuccess()) {
            return createDefaultNotificationStrategy();
        }
        
        return parseNotificationStrategy(llmResponse.getResponse());
    }

    /**
     * Build context analysis prompt
     */
    private String buildContextAnalysisPrompt(ContactNotificationInput input) {
        return String.format("""
            Analyze the emergency context for contact notification decisions:
            
            EMERGENCY DETAILS:
            - Emergency Type: %s
            - Priority Level: %s
            - Confidence Score: %.2f
            - Location: %s
            - Time Context: %s
            
            CONTEXT FACTORS TO ANALYZE:
            1. URGENCY_LEVEL: How urgent is immediate contact notification?
            2. PRIVACY_SENSITIVITY: How sensitive is the emergency information?
            3. CONTACT_APPROPRIATENESS: What types of contacts are most appropriate?
            4. COMMUNICATION_URGENCY: What communication methods are most suitable?
            5. ESCALATION_NEED: Is escalation to additional contacts needed?
            
            Provide analysis:
            URGENCY_ASSESSMENT: [LOW/MEDIUM/HIGH/CRITICAL]
            PRIVACY_LEVEL: [PUBLIC/PRIVATE/CONFIDENTIAL/RESTRICTED]
            APPROPRIATE_CONTACT_TYPES: [FAMILY/FRIENDS/MEDICAL/PROFESSIONAL/ALL]
            RECOMMENDED_COMMUNICATION: [SMS/VOICE/EMAIL/ALL]
            ESCALATION_REQUIRED: [YES/NO]
            CONTEXT_REASONING: [Explain the analysis]
            """, 
            input.getEmergencyType(), input.getPriorityLevel(), input.getConfidenceScore(),
            input.getLocation(), input.getTimeContext());
    }

    /**
     * Build contact prioritization prompt
     */
    private String buildContactPrioritizationPrompt(
            List<User.TrustedContact> contacts,
            ContactNotificationInput input,
            NotificationContextAnalysis contextAnalysis) {
        
        StringBuilder contactsInfo = new StringBuilder();
        for (int i = 0; i < contacts.size(); i++) {
            User.TrustedContact contact = contacts.get(i);
            contactsInfo.append(String.format("Contact %d: %s (%s) - %s - Primary: %s\n", 
                i + 1, contact.getName(), contact.getRelationship(), 
                contact.getPreferredContactMethod(), contact.getIsPrimary()));
        }
        
        return String.format("""
            Prioritize trusted contacts for emergency notification:
            
            EMERGENCY CONTEXT:
            - Type: %s
            - Priority: %s
            - Urgency: %s
            - Privacy Level: %s
            
            AVAILABLE CONTACTS:
            %s
            
            PRIORITIZATION CRITERIA:
            1. Primary contacts get highest priority
            2. Family members for medical emergencies
            3. Close friends for security threats
            4. Professional contacts for work-related incidents
            5. Consider preferred communication methods
            6. Consider relationship appropriateness for emergency type
            
            Provide prioritization:
            CONTACT_PRIORITY_ORDER: [List contact numbers in priority order: 1,2,3...]
            HIGH_PRIORITY_CONTACTS: [Contact numbers that should be notified immediately]
            MEDIUM_PRIORITY_CONTACTS: [Contact numbers for secondary notification]
            NOTIFICATION_REASONING: [Explain prioritization decisions]
            """, 
            input.getEmergencyType(), input.getPriorityLevel(), 
            contextAnalysis.getUrgencyAssessment(), contextAnalysis.getPrivacyLevel(),
            contactsInfo.toString());
    }

    /**
     * Build notification strategy prompt
     */
    private String buildNotificationStrategyPrompt(
            ContactNotificationInput input,
            NotificationContextAnalysis contextAnalysis,
            ContactPrioritizationResult prioritization) {
        
        return String.format("""
            Determine optimal notification strategy:
            
            CONTEXT:
            - Urgency: %s
            - Privacy: %s
            - High Priority Contacts: %d
            - Total Contacts: %d
            
            STRATEGY OPTIONS:
            1. IMMEDIATE_ALL: Notify all contacts simultaneously
            2. CASCADING: Notify in priority order with delays
            3. SELECTIVE: Notify only highest priority contacts
            4. ESCALATING: Start with few, escalate if no response
            
            Determine:
            STRATEGY_TYPE: [IMMEDIATE_ALL/CASCADING/SELECTIVE/ESCALATING]
            NOTIFICATION_TIMING: [SIMULTANEOUS/STAGGERED/DELAYED]
            RETRY_STRATEGY: [IMMEDIATE/DELAYED/ESCALATING]
            MAX_CONTACTS_TO_NOTIFY: [Number]
            STRATEGY_REASONING: [Explain strategy choice]
            """, 
            contextAnalysis.getUrgencyAssessment(), contextAnalysis.getPrivacyLevel(),
            prioritization.getHighPriorityContacts().size(), prioritization.getTotalContacts());
    }

    /**
     * Create notification plans for contacts
     */
    private List<ContactNotificationPlan> createNotificationPlans(
            List<PrioritizedContact> prioritizedContacts,
            NotificationStrategy strategy,
            ContactNotificationInput input) {
        
        List<ContactNotificationPlan> plans = new ArrayList<>();
        int maxContacts = Math.min(strategy.getMaxContactsToNotify(), prioritizedContacts.size());
        
        for (int i = 0; i < maxContacts; i++) {
            PrioritizedContact contact = prioritizedContacts.get(i);
            
            ContactNotificationPlan plan = ContactNotificationPlan.builder()
                    .planId(generatePlanId())
                    .contactId(contact.getContact().getContactId())
                    .contactName(contact.getContact().getName())
                    .contactRelationship(contact.getContact().getRelationship())
                    .priorityLevel(contact.getPriorityLevel())
                    .notificationMethod(determineNotificationMethod(contact.getContact(), strategy))
                    .notificationTiming(calculateNotificationTiming(i, strategy))
                    .retryStrategy(strategy.getRetryStrategy())
                    .messageContent(generateMessageContent(input, contact.getContact()))
                    .build();
            
            plans.add(plan);
        }
        
        return plans;
    }

    /**
     * Test reasoning-based contact notification
     */
    public ContactNotificationDecisionResult testReasoningBasedContactNotification() {
        ContactNotificationInput testInput = ContactNotificationInput.builder()
                .userId("test-user-contact-notification")
                .emergencyType("PHYSICAL_THREAT")
                .priorityLevel("HIGH")
                .confidenceScore(0.87)
                .location("Downtown area, parking garage")
                .timeContext("Late evening, 11:30 PM")
                .emergencyDescription("High-confidence threat detection with physical danger indicators")
                .build();
        
        return makeContactNotificationDecisions(testInput);
    }

    // Parsing and utility methods
    private NotificationContextAnalysis parseContextAnalysis(String response) {
        return NotificationContextAnalysis.builder()
                .urgencyAssessment(extractValue(response, "URGENCY_ASSESSMENT:", "HIGH"))
                .privacyLevel(extractValue(response, "PRIVACY_LEVEL:", "PRIVATE"))
                .appropriateContactTypes(extractValue(response, "APPROPRIATE_CONTACT_TYPES:", "ALL"))
                .recommendedCommunication(extractValue(response, "RECOMMENDED_COMMUNICATION:", "VOICE"))
                .escalationRequired(extractBooleanValue(response, "ESCALATION_REQUIRED:", true))
                .contextReasoning(extractValue(response, "CONTEXT_REASONING:", "Emergency context analysis"))
                .build();
    }

    private ContactPrioritizationResult parseContactPrioritization(String response, List<User.TrustedContact> contacts) {
        String priorityOrder = extractValue(response, "CONTACT_PRIORITY_ORDER:", "1,2,3");
        String highPriorityStr = extractValue(response, "HIGH_PRIORITY_CONTACTS:", "1,2");
        
        List<PrioritizedContact> prioritizedContacts = new ArrayList<>();
        String[] priorities = priorityOrder.split(",");
        
        for (int i = 0; i < priorities.length && i < contacts.size(); i++) {
            try {
                int contactIndex = Integer.parseInt(priorities[i].trim()) - 1;
                if (contactIndex >= 0 && contactIndex < contacts.size()) {
                    User.TrustedContact contact = contacts.get(contactIndex);
                    boolean isHighPriority = highPriorityStr.contains(String.valueOf(contactIndex + 1));
                    
                    PrioritizedContact prioritized = PrioritizedContact.builder()
                            .contact(contact)
                            .priorityRank(i + 1)
                            .priorityLevel(isHighPriority ? "HIGH" : "MEDIUM")
                            .build();
                    
                    prioritizedContacts.add(prioritized);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid contact priority format: {}", priorities[i]);
            }
        }
        
        return ContactPrioritizationResult.builder()
                .prioritizedContacts(prioritizedContacts)
                .highPriorityContacts(prioritizedContacts.stream()
                    .filter(c -> "HIGH".equals(c.getPriorityLevel()))
                    .toList())
                .totalContacts(prioritizedContacts.size())
                .build();
    }

    private NotificationStrategy parseNotificationStrategy(String response) {
        return NotificationStrategy.builder()
                .strategyType(extractValue(response, "STRATEGY_TYPE:", "SELECTIVE"))
                .notificationTiming(extractValue(response, "NOTIFICATION_TIMING:", "STAGGERED"))
                .retryStrategy(extractValue(response, "RETRY_STRATEGY:", "IMMEDIATE"))
                .maxContactsToNotify(extractIntValue(response, "MAX_CONTACTS_TO_NOTIFY:", 3))
                .strategyReasoning(extractValue(response, "STRATEGY_REASONING:", "Selective notification strategy"))
                .build();
    }

    // Utility methods
    private String determineNotificationMethod(User.TrustedContact contact, NotificationStrategy strategy) {
        String preferred = contact.getPreferredContactMethod();
        if (preferred != null && !preferred.isEmpty()) {
            return preferred;
        }
        return "SMS"; // Default fallback
    }

    private int calculateNotificationTiming(int contactIndex, NotificationStrategy strategy) {
        if ("SIMULTANEOUS".equals(strategy.getNotificationTiming())) {
            return 0; // Immediate
        } else if ("STAGGERED".equals(strategy.getNotificationTiming())) {
            return contactIndex * 30; // 30 second intervals
        } else {
            return contactIndex * 60; // 1 minute intervals
        }
    }

    private String generateMessageContent(ContactNotificationInput input, User.TrustedContact contact) {
        return String.format("EMERGENCY ALERT: %s emergency detected for your contact. " +
                           "Priority: %s. Location: %s. Please respond immediately.",
                           input.getEmergencyType(), input.getPriorityLevel(), input.getLocation());
    }

    private String extractValue(String response, String pattern, String defaultValue) {
        try {
            int startIndex = response.indexOf(pattern);
            if (startIndex == -1) return defaultValue;
            
            startIndex += pattern.length();
            int endIndex = response.indexOf('\n', startIndex);
            if (endIndex == -1) endIndex = response.length();
            
            return response.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String response, String pattern, boolean defaultValue) {
        String value = extractValue(response, pattern, String.valueOf(defaultValue));
        return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
    }

    private int extractIntValue(String response, String pattern, int defaultValue) {
        try {
            String value = extractValue(response, pattern, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String generateDecisionId() {
        return "CONTACT-DECISION-" + System.currentTimeMillis();
    }

    private String generatePlanId() {
        return "PLAN-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    // Default creation methods
    private NotificationContextAnalysis createDefaultContextAnalysis() {
        return NotificationContextAnalysis.builder()
                .urgencyAssessment("MEDIUM")
                .privacyLevel("PRIVATE")
                .appropriateContactTypes("FAMILY")
                .recommendedCommunication("SMS")
                .escalationRequired(false)
                .contextReasoning("Default context analysis")
                .build();
    }

    private ContactPrioritizationResult createDefaultPrioritization(List<User.TrustedContact> contacts) {
        List<PrioritizedContact> prioritized = new ArrayList<>();
        for (int i = 0; i < Math.min(3, contacts.size()); i++) {
            prioritized.add(PrioritizedContact.builder()
                    .contact(contacts.get(i))
                    .priorityRank(i + 1)
                    .priorityLevel(i == 0 ? "HIGH" : "MEDIUM")
                    .build());
        }
        
        return ContactPrioritizationResult.builder()
                .prioritizedContacts(prioritized)
                .highPriorityContacts(prioritized.stream()
                    .filter(c -> "HIGH".equals(c.getPriorityLevel()))
                    .toList())
                .totalContacts(prioritized.size())
                .build();
    }

    private NotificationStrategy createDefaultNotificationStrategy() {
        return NotificationStrategy.builder()
                .strategyType("SELECTIVE")
                .notificationTiming("STAGGERED")
                .retryStrategy("IMMEDIATE")
                .maxContactsToNotify(2)
                .strategyReasoning("Default notification strategy")
                .build();
    }

    private ContactNotificationDecisionResult createFailedDecisionResult(String errorMessage) {
        return ContactNotificationDecisionResult.builder()
                .decisionId(generateDecisionId())
                .success(false)
                .errorMessage(errorMessage)
                .decisionTimestamp(Instant.now())
                .build();
    }

    private ContactNotificationDecisionResult createNoContactsResult(String userId) {
        return ContactNotificationDecisionResult.builder()
                .decisionId(generateDecisionId())
                .userId(userId)
                .notificationPlans(new ArrayList<>())
                .totalContactsToNotify(0)
                .success(true)
                .errorMessage("No trusted contacts configured for user")
                .decisionTimestamp(Instant.now())
                .build();
    }    // D
ata classes
    public static class ContactNotificationInput {
        private String userId;
        private String emergencyType;
        private String priorityLevel;
        private double confidenceScore;
        private String location;
        private String timeContext;
        private String emergencyDescription;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ContactNotificationInput input = new ContactNotificationInput();
            public Builder userId(String userId) { input.userId = userId; return this; }
            public Builder emergencyType(String emergencyType) { input.emergencyType = emergencyType; return this; }
            public Builder priorityLevel(String priorityLevel) { input.priorityLevel = priorityLevel; return this; }
            public Builder confidenceScore(double confidenceScore) { input.confidenceScore = confidenceScore; return this; }
            public Builder location(String location) { input.location = location; return this; }
            public Builder timeContext(String timeContext) { input.timeContext = timeContext; return this; }
            public Builder emergencyDescription(String emergencyDescription) { input.emergencyDescription = emergencyDescription; return this; }
            public ContactNotificationInput build() { return input; }
        }

        // Getters
        public String getUserId() { return userId; }
        public String getEmergencyType() { return emergencyType; }
        public String getPriorityLevel() { return priorityLevel; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getLocation() { return location; }
        public String getTimeContext() { return timeContext; }
        public String getEmergencyDescription() { return emergencyDescription; }
    }

    public static class NotificationContextAnalysis {
        private String urgencyAssessment;
        private String privacyLevel;
        private String appropriateContactTypes;
        private String recommendedCommunication;
        private boolean escalationRequired;
        private String contextReasoning;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private NotificationContextAnalysis analysis = new NotificationContextAnalysis();
            public Builder urgencyAssessment(String urgencyAssessment) { analysis.urgencyAssessment = urgencyAssessment; return this; }
            public Builder privacyLevel(String privacyLevel) { analysis.privacyLevel = privacyLevel; return this; }
            public Builder appropriateContactTypes(String appropriateContactTypes) { analysis.appropriateContactTypes = appropriateContactTypes; return this; }
            public Builder recommendedCommunication(String recommendedCommunication) { analysis.recommendedCommunication = recommendedCommunication; return this; }
            public Builder escalationRequired(boolean escalationRequired) { analysis.escalationRequired = escalationRequired; return this; }
            public Builder contextReasoning(String contextReasoning) { analysis.contextReasoning = contextReasoning; return this; }
            public NotificationContextAnalysis build() { return analysis; }
        }

        // Getters
        public String getUrgencyAssessment() { return urgencyAssessment; }
        public String getPrivacyLevel() { return privacyLevel; }
        public String getAppropriateContactTypes() { return appropriateContactTypes; }
        public String getRecommendedCommunication() { return recommendedCommunication; }
        public boolean isEscalationRequired() { return escalationRequired; }
        public String getContextReasoning() { return contextReasoning; }
    }

    public static class PrioritizedContact {
        private User.TrustedContact contact;
        private int priorityRank;
        private String priorityLevel;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private PrioritizedContact prioritized = new PrioritizedContact();
            public Builder contact(User.TrustedContact contact) { prioritized.contact = contact; return this; }
            public Builder priorityRank(int priorityRank) { prioritized.priorityRank = priorityRank; return this; }
            public Builder priorityLevel(String priorityLevel) { prioritized.priorityLevel = priorityLevel; return this; }
            public PrioritizedContact build() { return prioritized; }
        }

        // Getters
        public User.TrustedContact getContact() { return contact; }
        public int getPriorityRank() { return priorityRank; }
        public String getPriorityLevel() { return priorityLevel; }
    }

    public static class ContactPrioritizationResult {
        private List<PrioritizedContact> prioritizedContacts;
        private List<PrioritizedContact> highPriorityContacts;
        private int totalContacts;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ContactPrioritizationResult result = new ContactPrioritizationResult();
            public Builder prioritizedContacts(List<PrioritizedContact> prioritizedContacts) { result.prioritizedContacts = prioritizedContacts; return this; }
            public Builder highPriorityContacts(List<PrioritizedContact> highPriorityContacts) { result.highPriorityContacts = highPriorityContacts; return this; }
            public Builder totalContacts(int totalContacts) { result.totalContacts = totalContacts; return this; }
            public ContactPrioritizationResult build() { return result; }
        }

        // Getters
        public List<PrioritizedContact> getPrioritizedContacts() { return prioritizedContacts; }
        public List<PrioritizedContact> getHighPriorityContacts() { return highPriorityContacts; }
        public int getTotalContacts() { return totalContacts; }
    }

    public static class NotificationStrategy {
        private String strategyType;
        private String notificationTiming;
        private String retryStrategy;
        private int maxContactsToNotify;
        private String strategyReasoning;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private NotificationStrategy strategy = new NotificationStrategy();
            public Builder strategyType(String strategyType) { strategy.strategyType = strategyType; return this; }
            public Builder notificationTiming(String notificationTiming) { strategy.notificationTiming = notificationTiming; return this; }
            public Builder retryStrategy(String retryStrategy) { strategy.retryStrategy = retryStrategy; return this; }
            public Builder maxContactsToNotify(int maxContactsToNotify) { strategy.maxContactsToNotify = maxContactsToNotify; return this; }
            public Builder strategyReasoning(String strategyReasoning) { strategy.strategyReasoning = strategyReasoning; return this; }
            public NotificationStrategy build() { return strategy; }
        }

        // Getters
        public String getStrategyType() { return strategyType; }
        public String getNotificationTiming() { return notificationTiming; }
        public String getRetryStrategy() { return retryStrategy; }
        public int getMaxContactsToNotify() { return maxContactsToNotify; }
        public String getStrategyReasoning() { return strategyReasoning; }
    }

    public static class ContactNotificationPlan {
        private String planId;
        private String contactId;
        private String contactName;
        private String contactRelationship;
        private String priorityLevel;
        private String notificationMethod;
        private int notificationTiming;
        private String retryStrategy;
        private String messageContent;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ContactNotificationPlan plan = new ContactNotificationPlan();
            public Builder planId(String planId) { plan.planId = planId; return this; }
            public Builder contactId(String contactId) { plan.contactId = contactId; return this; }
            public Builder contactName(String contactName) { plan.contactName = contactName; return this; }
            public Builder contactRelationship(String contactRelationship) { plan.contactRelationship = contactRelationship; return this; }
            public Builder priorityLevel(String priorityLevel) { plan.priorityLevel = priorityLevel; return this; }
            public Builder notificationMethod(String notificationMethod) { plan.notificationMethod = notificationMethod; return this; }
            public Builder notificationTiming(int notificationTiming) { plan.notificationTiming = notificationTiming; return this; }
            public Builder retryStrategy(String retryStrategy) { plan.retryStrategy = retryStrategy; return this; }
            public Builder messageContent(String messageContent) { plan.messageContent = messageContent; return this; }
            public ContactNotificationPlan build() { return plan; }
        }

        // Getters
        public String getPlanId() { return planId; }
        public String getContactId() { return contactId; }
        public String getContactName() { return contactName; }
        public String getContactRelationship() { return contactRelationship; }
        public String getPriorityLevel() { return priorityLevel; }
        public String getNotificationMethod() { return notificationMethod; }
        public int getNotificationTiming() { return notificationTiming; }
        public String getRetryStrategy() { return retryStrategy; }
        public String getMessageContent() { return messageContent; }
    }

    public static class ContactNotificationDecisionResult {
        private String decisionId;
        private String userId;
        private NotificationContextAnalysis contextAnalysis;
        private ContactPrioritizationResult contactPrioritization;
        private NotificationStrategy notificationStrategy;
        private List<ContactNotificationPlan> notificationPlans;
        private int totalContactsToNotify;
        private Instant decisionTimestamp;
        private boolean success;
        private String errorMessage;

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private ContactNotificationDecisionResult result = new ContactNotificationDecisionResult();
            public Builder decisionId(String decisionId) { result.decisionId = decisionId; return this; }
            public Builder userId(String userId) { result.userId = userId; return this; }
            public Builder contextAnalysis(NotificationContextAnalysis contextAnalysis) { result.contextAnalysis = contextAnalysis; return this; }
            public Builder contactPrioritization(ContactPrioritizationResult contactPrioritization) { result.contactPrioritization = contactPrioritization; return this; }
            public Builder notificationStrategy(NotificationStrategy notificationStrategy) { result.notificationStrategy = notificationStrategy; return this; }
            public Builder notificationPlans(List<ContactNotificationPlan> notificationPlans) { result.notificationPlans = notificationPlans; return this; }
            public Builder totalContactsToNotify(int totalContactsToNotify) { result.totalContactsToNotify = totalContactsToNotify; return this; }
            public Builder decisionTimestamp(Instant decisionTimestamp) { result.decisionTimestamp = decisionTimestamp; return this; }
            public Builder success(boolean success) { result.success = success; return this; }
            public Builder errorMessage(String errorMessage) { result.errorMessage = errorMessage; return this; }
            public ContactNotificationDecisionResult build() { return result; }
        }

        // Getters
        public String getDecisionId() { return decisionId; }
        public String getUserId() { return userId; }
        public NotificationContextAnalysis getContextAnalysis() { return contextAnalysis; }
        public ContactPrioritizationResult getContactPrioritization() { return contactPrioritization; }
        public NotificationStrategy getNotificationStrategy() { return notificationStrategy; }
        public List<ContactNotificationPlan> getNotificationPlans() { return notificationPlans; }
        public int getTotalContactsToNotify() { return totalContactsToNotify; }
        public Instant getDecisionTimestamp() { return decisionTimestamp; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}