package com.wis.orchestrator;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.wis.orchestrator.model.ScheduledMessageRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Timer-triggered Azure Function that checks for scheduled messages.
 * Runs every 5 minutes to check which users need devotionals or check-ins.
 */
public class SchedulerFunction {

    private static final Logger logger = Logger.getLogger(SchedulerFunction.class.getName());
    private final ObjectMapper objectMapper;

    public SchedulerFunction() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Timer function that runs every 5 minutes to check for scheduled messages.
     * Queries Cosmos DB for customers whose nextDevotionalScheduledFor or
     * nextCheckInScheduledFor is <= current time.
     *
     * @param timerInfo Timer trigger info
     * @param cosmosDbCustomers Input binding from Cosmos DB
     * @param outputMessages Output binding to message-send-queue
     * @param context Function execution context
     */
    @FunctionName("MessageScheduler")
    public void messageScheduler(
            @TimerTrigger(
                    name = "timerInfo",
                    schedule = "0 */5 * * * *") // Every 5 minutes
            String timerInfo,
            @CosmosDBInput(
                    name = "cosmosDbCustomers",
                    databaseName = "WIS-Platform",
                    containerName = "customers",
                    connection = "CosmosDBConnection",
                    sqlQuery = "SELECT * FROM c WHERE " +
                            "(c.messagingState.nextDevotionalScheduledFor <= GetCurrentDateTime() " +
                            "OR c.messagingState.nextCheckInScheduledFor <= GetCurrentDateTime()) " +
                            "AND c.messagingState.conversationState = 'active'")
            String[] cosmosDbCustomers,
            @ServiceBusQueueOutput(
                    name = "outputMessages",
                    queueName = "message-send-queue",
                    connection = "ServiceBusConnection")
            OutputBinding<String[]> outputMessages,
            final ExecutionContext context) {

        Instant now = Instant.now();
        logger.log(Level.INFO, "MessageScheduler triggered at: {0}", now);

        if (cosmosDbCustomers == null || cosmosDbCustomers.length == 0) {
            logger.log(Level.INFO, "No customers with scheduled messages found.");
            return;
        }

        logger.log(Level.INFO, "Found {0} customers with scheduled messages", cosmosDbCustomers.length);

        try {
            // Process each customer
            for (String customerJson : cosmosDbCustomers) {
                processCustomerSchedule(customerJson, now, outputMessages);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing scheduled messages: " + e.getMessage(), e);
            throw new RuntimeException("Failed to process scheduled messages", e);
        }
    }

    /**
     * Processes a single customer's schedule and queues appropriate messages.
     *
     * @param customerJson JSON string of customer data
     * @param now Current timestamp
     * @param outputMessages Output binding for message queue
     */
    private void processCustomerSchedule(String customerJson, Instant now, OutputBinding<String[]> outputMessages) {
        try {
            // Parse customer data
            CustomerScheduleData customer = objectMapper.readValue(customerJson, CustomerScheduleData.class);

            // Check if devotional is due
            if (customer.messagingState != null &&
                    customer.messagingState.nextDevotionalScheduledFor != null &&
                    customer.messagingState.nextDevotionalScheduledFor.isBefore(now)) {

                logger.log(Level.INFO, "Queueing devotional for customer: {0}", customer.id);
                queueDevotionalMessage(customer, outputMessages);
            }

            // Check if check-in is due
            if (customer.messagingState != null &&
                    customer.messagingState.nextCheckInScheduledFor != null &&
                    customer.messagingState.nextCheckInScheduledFor.isBefore(now)) {

                logger.log(Level.INFO, "Queueing check-in for customer: {0}", customer.id);
                queueCheckInMessage(customer, outputMessages);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing customer schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Queues a devotional message for the customer.
     */
    private void queueDevotionalMessage(CustomerScheduleData customer, OutputBinding<String[]> outputMessages) {
        try {
            ScheduledMessageRequest message = ScheduledMessageRequest.builder()
                    .messageId(java.util.UUID.randomUUID().toString())
                    .customerId(customer.id)
                    .phoneNumber(customer.currentPhone)
                    .messageType("daily_devotional")
                    .priority("NORMAL")
                    .themes(customer.messagingState.extractedThemes)
                    .lifeSeason(customer.messagingState.currentLifeSeason)
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);

            // Add to output binding
            String[] messages = {messageJson};
            outputMessages.setValue(messages);

            logger.log(Level.INFO, "Devotional message queued for customer: {0}", customer.id);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to queue devotional message: " + e.getMessage(), e);
        }
    }

    /**
     * Queues a check-in message asking about the user's current season.
     */
    private void queueCheckInMessage(CustomerScheduleData customer, OutputBinding<String[]> outputMessages) {
        try {
            ScheduledMessageRequest message = ScheduledMessageRequest.builder()
                    .messageId(java.util.UUID.randomUUID().toString())
                    .customerId(customer.id)
                    .phoneNumber(customer.currentPhone)
                    .messageType("season_check_in")
                    .priority("NORMAL")
                    .message("Hi! It's been a while. How are things going? " +
                          "Has your season of life changed since we last talked? " +
                          "Feel free to share what's on your heart.")
                    .build();

            String messageJson = objectMapper.writeValueAsString(message);

            // Add to output binding
            String[] messages = {messageJson};
            outputMessages.setValue(messages);

            logger.log(Level.INFO, "Check-in message queued for customer: {0}", customer.id);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to queue check-in message: " + e.getMessage(), e);
        }
    }

    /**
     * Simple DTO for customer schedule data from Cosmos DB.
     */
    private static class CustomerScheduleData {
        public String id;
        public String currentPhone;
        public ProfileData profile;
        public MessagingStateData messagingState;

        private static class ProfileData {
            public PreferencesData preferences;
        }

        private static class PreferencesData {
            public String timezone;
            public String preferredTimeOfDay;
            public Integer checkInIntervalDays;
            public Integer devotionalIntervalDays;
        }

        private static class MessagingStateData {
            public String currentLifeSeason;
            public java.util.List<String> extractedThemes;
            public Instant lastSeasonUpdateAt;
            public Instant nextCheckInScheduledFor;
            public Instant lastDevotionalSentAt;
            public Instant nextDevotionalScheduledFor;
            public String conversationState;
        }
    }
}
