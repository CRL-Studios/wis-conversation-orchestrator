package com.wis.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.wis.orchestrator.entity.DevotionalPlanEntity;
import com.wis.orchestrator.entity.CustomerEntity;
import com.wis.orchestrator.model.DailyDevotion;
import com.wis.orchestrator.service.CosmosDBService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Azure Functions that manage 7-day devotional plan delivery and completion.
 * Processes daily messages from active plans and handles plan completion workflow.
 */
public class DevotionalPlanFunction {

    private static final Logger logger = Logger.getLogger(DevotionalPlanFunction.class.getName());
    private final ObjectMapper objectMapper;

    public DevotionalPlanFunction() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Timer function that runs every 5 minutes to process 7-day devotional plans.
     * Queries Cosmos DB for customers with active plans where the next message is due,
     * loads their plan, and queues the appropriate day's devotional content.
     *
     * @param timerInfo Timer trigger info
     * @param customersWithPlans Input binding from Cosmos DB - customers with active plans
     * @param outputMessages Output binding to message-send-queue
     * @param context Function execution context
     */
    @FunctionName("ProcessDevotionalPlanDay")
    public void processDevotionalPlanDay(
            @TimerTrigger(
                    name = "timerInfo",
                    schedule = "0 */5 * * * *") // Every 5 minutes
            String timerInfo,
            @CosmosDBInput(
                    name = "customersWithPlans",
                    databaseName = "WIS-Platform",
                    containerName = "customers",
                    connection = "CosmosDBConnection",
                    sqlQuery = "SELECT * FROM c WHERE " +
                            "c.activePlanId != null " +
                            "AND c.messagingState.nextPlanMessageScheduledFor <= GetCurrentDateTime() " +
                            "AND c.status = 'active'")
            String[] customersWithPlans,
            @ServiceBusQueueOutput(
                    name = "outputMessages",
                    queueName = "message-send-queue",
                    connection = "ServiceBusConnection")
            OutputBinding<String[]> outputMessages,
            final ExecutionContext context) {

        Instant now = Instant.now();
        logger.log(Level.INFO, "ProcessDevotionalPlanDay triggered at: {0}", now);

        if (customersWithPlans == null || customersWithPlans.length == 0) {
            logger.log(Level.INFO, "No customers with active plans due for messages");
            return;
        }

        logger.log(Level.INFO, "Found {0} customers with active plans due for messages",
                new Object[]{customersWithPlans.length});

        try {
            // Process each customer's plan
            for (String customerJson : customersWithPlans) {
                processCustomerPlan(customerJson, now, outputMessages);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing devotional plan messages: " + e.getMessage(), e);
            throw new RuntimeException("Failed to process devotional plan messages", e);
        }
    }

    /**
     * Processes a single customer's devotional plan and queues the appropriate day's message.
     *
     * @param customerJson JSON string of customer data
     * @param now Current timestamp
     * @param outputMessages Output binding for message queue
     */
    private void processCustomerPlan(String customerJson, Instant now, OutputBinding<String[]> outputMessages) {
        try {
            // Parse customer data
            CustomerPlanData customer = objectMapper.readValue(customerJson, CustomerPlanData.class);

            if (customer.activePlanId == null || customer.activePlanId.isEmpty()) {
                logger.log(Level.WARNING, "Customer {0} has null activePlanId, skipping",
                        new Object[]{customer.id});
                return;
            }

            logger.log(Level.INFO, "Processing plan {0} for customer: {1}",
                    new Object[]{customer.activePlanId, customer.id});

            // Load DevotionalPlanEntity from Cosmos DB
            CosmosDBService cosmosDB = CosmosDBService.getInstance();
            Optional<DevotionalPlanEntity> planOpt = cosmosDB.findPlanByIdAndCustomerId(
                    customer.activePlanId, customer.id);

            if (planOpt.isEmpty()) {
                logger.log(Level.WARNING, "Plan {0} not found for customer {1}",
                        new Object[]{customer.activePlanId, customer.id});
                return;
            }

            DevotionalPlanEntity plan = planOpt.get();

            if (!"active".equals(plan.getStatus())) {
                logger.log(Level.WARNING, "Plan {0} is not active (status: {1})",
                        new Object[]{plan.getId(), plan.getStatus()});
                return;
            }

            if (plan.getCurrentDay() == null || plan.getDays() == null || plan.getDays().isEmpty()) {
                logger.log(Level.WARNING, "Plan {0} has invalid data (currentDay or days missing)",
                        plan.getId());
                return;
            }

            int currentDay = plan.getCurrentDay();
            if (currentDay < 1 || currentDay > plan.getDays().size()) {
                logger.log(Level.WARNING, "Plan {0} has invalid currentDay: {1}",
                        new Object[]{plan.getId(), currentDay});
                return;
            }

            DailyDevotion dailyDevotion = plan.getDays().get(currentDay - 1);

            // Format the message with day counter
            String messageText = formatDailyDevotionMessage(
                    dailyDevotion.getVerseReference(),
                    dailyDevotion.getVerseText(),
                    dailyDevotion.getReflection(),
                    dailyDevotion.getJournalPrompt(),
                    currentDay
            );

            // Create message with plan metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("planId", plan.getId());
            metadata.put("dayNumber", currentDay);
            metadata.put("messageType", "daily_plan_devotion");

            DevotionalPlanMessage message = DevotionalPlanMessage.builder()
                    .messageId(java.util.UUID.randomUUID().toString())
                    .customerId(customer.id)
                    .phoneNumber(customer.currentPhone)
                    .messageType("daily_plan_devotion")
                    .priority(currentDay == 1 ? "HIGH" : "NORMAL")
                    .message(messageText)
                    .metadata(metadata)
                    .build();

            // Queue the message
            String messageJson = objectMapper.writeValueAsString(message);
            String[] messages = {messageJson};
            outputMessages.setValue(messages);

            logger.log(Level.INFO, "Queued Day {0} message for plan {1}, customer {2}",
                    new Object[]{currentDay, plan.getId(), customer.id});

            // Update next message time (24 hours later, respecting timezone)
            updateNextMessageTime(customer, currentDay);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing customer plan: " + e.getMessage(), e);
        }
    }

    /**
     * Formats a daily devotion message with day counter.
     * Format matches message-handler's formatDailyDevotionMessage template.
     *
     * @param verseReference Scripture reference
     * @param verseText Full verse text
     * @param reflection Pastoral reflection
     * @param journalPrompt Journal prompt for application
     * @param dayNumber Current day (1-7)
     * @return Formatted message text
     */
    private String formatDailyDevotionMessage(String verseReference, String verseText,
                                              String reflection, String journalPrompt, int dayNumber) {
        return String.format("📖 Day %d of 7\n\n", dayNumber) +
                "\"" + verseText + "\"\n" +
                "— " + verseReference + "\n\n" +
                reflection + "\n\n" +
                "📝 Journal Prompt: " + journalPrompt;
    }

    /**
     * Updates the customer's nextPlanMessageScheduledFor to schedule the next day's message.
     * Calculates 24 hours from now, respecting the user's timezone and preferred time.
     *
     * @param customer Customer data with messaging state
     * @param currentDay Current day that was just sent
     */
    private void updateNextMessageTime(CustomerPlanData customer, int currentDay) {
        try {
            // Load full customer entity to update
            CosmosDBService cosmosDB = CosmosDBService.getInstance();
            Optional<CustomerEntity> customerOpt = cosmosDB.findCustomerById(customer.id);

            if (customerOpt.isEmpty()) {
                logger.log(Level.WARNING, "Customer {0} not found when updating next message time",
                        customer.id);
                return;
            }

            CustomerEntity customerEntity = customerOpt.get();

            // Schedule next message for 24 hours from now
            // TODO: Add timezone-aware scheduling logic here
            // For now, simple 24-hour increment
            Instant nextMessageTime = Instant.now().plus(24, ChronoUnit.HOURS);

            if (customerEntity.getMessagingState() == null) {
                customerEntity.setMessagingState(new CustomerEntity.MessagingState());
            }

            customerEntity.getMessagingState().setNextPlanMessageScheduledFor(nextMessageTime);

            cosmosDB.updateCustomer(customerEntity);

            logger.log(Level.INFO, "Scheduled next message for customer {0} at {1}",
                    new Object[]{customer.id, nextMessageTime});

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating next message time for customer {0}: {1}",
                    new Object[]{customer.id, e.getMessage()});
            // Don't throw - message was already queued successfully
        }
    }

    /**
     * Timer function that checks for completed 7-day plans and sends weekly check-in prompts.
     * Runs every 5 minutes to find customers whose Day 7 was sent in the last 24 hours
     * and don't yet have a check-in scheduled.
     *
     * @param timerInfo Timer trigger info
     * @param completedPlans Input binding from Cosmos DB - recently completed plans
     * @param outputMessages Output binding to message-send-queue
     * @param context Function execution context
     */
    @FunctionName("ProcessPlanCompletion")
    public void processPlanCompletion(
            @TimerTrigger(
                    name = "timerInfo",
                    schedule = "0 */5 * * * *") // Every 5 minutes
            String timerInfo,
            @CosmosDBInput(
                    name = "completedPlans",
                    databaseName = "WIS-Platform",
                    containerName = "devotionalPlans",
                    connection = "CosmosDBConnection",
                    sqlQuery = "SELECT * FROM c WHERE " +
                            "c.status = 'completed' " +
                            "AND NOT IS_DEFINED(c.checkInSent)")
            String[] completedPlans,
            @ServiceBusQueueOutput(
                    name = "outputMessages",
                    queueName = "message-send-queue",
                    connection = "ServiceBusConnection")
            OutputBinding<String[]> outputMessages,
            final ExecutionContext context) {

        Instant now = Instant.now();
        logger.log(Level.INFO, "ProcessPlanCompletion triggered at: {0}", now);

        if (completedPlans == null || completedPlans.length == 0) {
            logger.log(Level.INFO, "No completed plans needing check-in messages");
            return;
        }

        logger.log(Level.INFO, "Found {0} completed plans needing check-in messages",
                new Object[]{completedPlans.length});

        try {
            for (String planJson : completedPlans) {
                sendWeeklyCheckIn(planJson, outputMessages);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing plan completions: " + e.getMessage(), e);
            throw new RuntimeException("Failed to process plan completions", e);
        }
    }

    /**
     * Queues a weekly check-in request after a 7-day plan completes.
     * The message-handler will load customer data, format the message, and send it.
     *
     * @param planJson Completed plan JSON
     * @param outputMessages Output binding for message queue
     */
    private void sendWeeklyCheckIn(String planJson, OutputBinding<String[]> outputMessages) {
        try {
            CompletedPlanData plan = objectMapper.readValue(planJson, CompletedPlanData.class);

            logger.log(Level.INFO, "Queueing weekly check-in request for customer {0}, plan {1}",
                    new Object[]{plan.customerId, plan.id});

            // Create simple check-in request - message-handler will handle formatting and sending
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("completedPlanId", plan.id);
            metadata.put("expectsResponse", true);

            WeeklyCheckInRequest request = WeeklyCheckInRequest.builder()
                    .messageId(java.util.UUID.randomUUID().toString())
                    .customerId(plan.customerId)
                    .messageType("weekly_check_in")
                    .priority("NORMAL")
                    .metadata(metadata)
                    .build();

            // Queue the request
            String requestJson = objectMapper.writeValueAsString(request);
            String[] messages = {requestJson};
            outputMessages.setValue(messages);

            logger.log(Level.INFO, "Queued weekly check-in request for customer {0}", plan.customerId);

            // Note: checkInSent flag will be marked by message-handler when it actually sends the message
            // This prevents marking it sent if the message fails to deliver

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error queueing weekly check-in: " + e.getMessage(), e);
        }
    }

    /**
     * Simple DTO for customer data with active plan info.
     */
    private static class CustomerPlanData {
        public String id;
        public String currentPhone;
        public String activePlanId;
        public String status;
        public MessagingStateData messagingState;

        private static class MessagingStateData {
            public Instant nextPlanMessageScheduledFor;
            public String timezone;
            public String preferredTimeOfDay;
        }
    }

    /**
     * Simple DTO for completed plan data.
     */
    private static class CompletedPlanData {
        public String id;
        public String customerId;
        public String status;
        public Instant completedAt;
    }

    /**
     * Message structure for devotional plan messages.
     */
    private static class DevotionalPlanMessage {
        public String messageId;
        public String customerId;
        public String phoneNumber;
        public String messageType;
        public String priority;
        public String message;
        public Map<String, Object> metadata;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private DevotionalPlanMessage message = new DevotionalPlanMessage();

            public Builder messageId(String messageId) {
                message.messageId = messageId;
                return this;
            }

            public Builder customerId(String customerId) {
                message.customerId = customerId;
                return this;
            }

            public Builder phoneNumber(String phoneNumber) {
                message.phoneNumber = phoneNumber;
                return this;
            }

            public Builder messageType(String messageType) {
                message.messageType = messageType;
                return this;
            }

            public Builder priority(String priority) {
                message.priority = priority;
                return this;
            }

            public Builder message(String msg) {
                message.message = msg;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                message.metadata = metadata;
                return this;
            }

            public DevotionalPlanMessage build() {
                return message;
            }
        }
    }

    /**
     * Request structure for weekly check-in messages.
     * Message-handler will load customer data, format the message, and send it.
     */
    private static class WeeklyCheckInRequest {
        public String messageId;
        public String customerId;
        public String messageType;  // "weekly_check_in"
        public String priority;
        public Map<String, Object> metadata;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private WeeklyCheckInRequest request = new WeeklyCheckInRequest();

            public Builder messageId(String messageId) {
                request.messageId = messageId;
                return this;
            }

            public Builder customerId(String customerId) {
                request.customerId = customerId;
                return this;
            }

            public Builder messageType(String messageType) {
                request.messageType = messageType;
                return this;
            }

            public Builder priority(String priority) {
                request.priority = priority;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                request.metadata = metadata;
                return this;
            }

            public WeeklyCheckInRequest build() {
                return request;
            }
        }
    }
}
