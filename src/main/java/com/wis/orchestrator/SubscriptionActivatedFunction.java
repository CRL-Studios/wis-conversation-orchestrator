package com.wis.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.wis.orchestrator.entity.CustomerEntity;
import com.wis.orchestrator.model.SubscriptionActivatedEvent;
import com.wis.orchestrator.model.WelcomeMessage;
import com.wis.orchestrator.repository.CustomerRepository;
import com.wis.orchestrator.service.ConversationService;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Azure Function that processes SubscriptionActivated events from Service Bus
 * and sends the welcome message after payment is completed.
 */
public class SubscriptionActivatedFunction {

    private static final Logger logger = Logger.getLogger(SubscriptionActivatedFunction.class.getName());
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final CustomerRepository customerRepository;

    public SubscriptionActivatedFunction(CustomerRepository customerRepository) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.conversationService = new ConversationService();
        this.customerRepository = customerRepository;
    }

    /**
     * Service Bus trigger function that processes SubscriptionActivated events.
     * Triggers when a customer completes payment, sending the welcome message.
     *
     * @param message Service Bus message containing SubscriptionActivated event
     * @param messageId Message ID from Service Bus
     * @param enqueuedTime Time the message was enqueued
     * @param outputMessage Output binding for message-send-queue
     * @param context Function execution context
     */
    @FunctionName("ProcessSubscriptionActivated")
    public void processSubscriptionActivated(
            @ServiceBusTopicTrigger(
                    name = "message",
                    topicName = "subscription-events",
                    subscriptionName = "orchestration-subscription",
                    connection = "ServiceBusConnection")
            String message,
            @BindingName("MessageId") String messageId,
            @BindingName("EnqueuedTimeUtc") String enqueuedTime,
            @ServiceBusQueueOutput(
                    name = "outputMessage",
                    queueName = "message-send-queue",
                    connection = "ServiceBusConnection")
            OutputBinding<String> outputMessage,
            final ExecutionContext context) {

        logger.log(Level.INFO, "Processing SubscriptionActivated event. MessageId: {0}, EnqueuedTime: {1}",
                new Object[]{messageId, enqueuedTime});

        try {
            // Deserialize event
            SubscriptionActivatedEvent event = objectMapper.readValue(message, SubscriptionActivatedEvent.class);

            logger.log(Level.INFO, "SubscriptionActivated event received for customer: {0}, phone: {1}, subscription: {2}",
                    new Object[]{
                            event.getData().getCustomerId(),
                            event.getData().getPhoneNumber(),
                            event.getData().getSubscriptionId()
                    });

            // Validate event data
            if (event.getData() == null ||
                    event.getData().getCustomerId() == null ||
                    event.getData().getPhoneNumber() == null) {
                logger.log(Level.WARNING, "Invalid event data. Skipping processing.");
                return;
            }

            // Fetch customer to get firstName
            String firstName = null;
            try {
                Optional<CustomerEntity> customerOpt = customerRepository.findById(event.getData().getCustomerId());
                if (customerOpt.isPresent()) {
                    CustomerEntity customer = customerOpt.get();
                    if (customer.getProfile() != null) {
                        firstName = customer.getProfile().getFirstName();
                        logger.log(Level.INFO, "Retrieved firstName for customer: {0}", event.getData().getCustomerId());
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not fetch customer profile for firstName. Using default greeting. Error: " + e.getMessage());
            }

            // Create welcome message
            WelcomeMessage welcomeMessage = WelcomeMessage.builder()
                    .messageId(java.util.UUID.randomUUID().toString())
                    .customerId(event.getData().getCustomerId())
                    .conversationId("conv-" + event.getData().getCustomerId())
                    .phoneNumber(event.getData().getPhoneNumber())
                    .messageType("onboarding_welcome")
                    .priority("HIGH")
                    .message(buildWelcomeMessageText(firstName))
                    .metadata(WelcomeMessage.Metadata.builder()
                            .registrationEventId(event.getEventId())
                            .registrationStage("subscription_activated")
                            .attempt(1)
                            .maxRetries(3)
                            .build())
                    .build();

            // Serialize and send to message queue
            String welcomeMessageJson = objectMapper.writeValueAsString(welcomeMessage);
            outputMessage.setValue(welcomeMessageJson);

            logger.log(Level.INFO, "Welcome message queued successfully for customer: {0} after subscription activation",
                    event.getData().getCustomerId());

            // Initialize conversation state
            conversationService.initializeConversationState(
                    event.getData().getCustomerId(),
                    event.getData().getPhoneNumber()
            );

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing SubscriptionActivated event: " + e.getMessage(), e);
            // Throw exception to trigger Service Bus retry logic
            throw new RuntimeException("Failed to process SubscriptionActivated event", e);
        }
    }

    /**
     * Builds the welcome message text that asks for the user's background and season of life.
     *
     * @param firstName User's first name for personalization
     * @return Welcome message body
     */
    private String buildWelcomeMessageText(String firstName) {
        String greeting = (firstName != null && !firstName.isEmpty())
                ? "Hey " + firstName + "! 🌿"
                : "Hey! 🌿";

        return greeting + "\n" +
                "Before we begin, we'd love to get to know you a little better.\n\n" +
                "In 2–3 sentences, tell us about yourself — your background, what you do, " +
                "and anything that helps us understand who you are (your job, stage of life, or passions).\n\n" +
                "This helps us personalize your devotionals even more, so each one truly speaks to " +
                "not only your season but you as a person!\n\n" +
                "Reply STOP to unsubscribe or HELP for help. Msg & data rates may apply.";
    }
}
