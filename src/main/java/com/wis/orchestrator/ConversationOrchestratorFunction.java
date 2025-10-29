package com.wis.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.wis.orchestrator.model.CustomerRegisteredEvent;
import com.wis.orchestrator.model.WelcomeMessage;
import com.wis.orchestrator.service.ConversationService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Azure Function that processes CustomerRegistered events from Service Bus
 * and initiates the onboarding workflow by sending a welcome message.
 */
public class ConversationOrchestratorFunction {

    private static final Logger logger = Logger.getLogger(ConversationOrchestratorFunction.class.getName());
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;

    public ConversationOrchestratorFunction() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.conversationService = new ConversationService();
    }

    /**
     * Service Bus trigger function that processes CustomerRegistered events.
     * Triggers when a new customer completes registration, initiating the onboarding workflow.
     *
     * @param message Service Bus message containing CustomerRegistered event
     * @param messageId Message ID from Service Bus
     * @param enqueuedTime Time the message was enqueued
     * @param outputMessage Output binding for message-send-queue
     * @param context Function execution context
     */
    @FunctionName("ProcessCustomerRegistered")
    public void processCustomerRegistered(
            @ServiceBusTopicTrigger(
                    name = "message",
                    topicName = "customer-events",
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

        logger.log(Level.INFO, "Processing CustomerRegistered event. MessageId: {0}, EnqueuedTime: {1}",
                new Object[]{messageId, enqueuedTime});

        try {
            // Deserialize event
            CustomerRegisteredEvent event = objectMapper.readValue(message, CustomerRegisteredEvent.class);

            logger.log(Level.INFO, "CustomerRegistered event received for customer: {0}, phone: {1}",
                    new Object[]{event.getData().getCustomerId(), event.getData().getPhone()});

            // Validate event data
            if (event.getData() == null ||
                    event.getData().getCustomerId() == null ||
                    event.getData().getPhone() == null) {
                logger.log(Level.WARNING, "Invalid event data. Skipping processing.");
                return;
            }

            // Create welcome message
            WelcomeMessage welcomeMessage = WelcomeMessage.builder()
                    .messageId(java.util.UUID.randomUUID().toString())
                    .customerId(event.getData().getCustomerId())
                    .conversationId("conv-" + event.getData().getCustomerId())
                    .to(event.getData().getPhone())
                    .messageType("onboarding_welcome")
                    .priority("high")
                    .body(buildWelcomeMessageText())
                    .metadata(WelcomeMessage.Metadata.builder()
                            .registrationEventId(event.getEventId())
                            .registrationStage(event.getData().getRegistrationStage())
                            .attempt(1)
                            .maxRetries(3)
                            .build())
                    .build();

            // Serialize and send to message queue
            String welcomeMessageJson = objectMapper.writeValueAsString(welcomeMessage);
            outputMessage.setValue(welcomeMessageJson);

            logger.log(Level.INFO, "Welcome message queued successfully for customer: {0}",
                    event.getData().getCustomerId());

            // Update conversation state (optional - can be done by message handler)
            conversationService.initializeConversationState(
                    event.getData().getCustomerId(),
                    event.getData().getPhone()
            );

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing CustomerRegistered event: " + e.getMessage(), e);
            // Throw exception to trigger Service Bus retry logic
            throw new RuntimeException("Failed to process CustomerRegistered event", e);
        }
    }

    /**
     * Builds the welcome message text that asks for the user's season of life.
     *
     * @return Welcome message body
     */
    private String buildWelcomeMessageText() {
        return "Welcome to Words in Season! " +
                "We're here to walk with you through life's seasons. " +
                "\n\n" +
                "Tell us: What season of life are you in right now? " +
                "(For example: facing a challenge, celebrating a victory, seeking direction, etc.)";
    }

    /**
     * HTTP trigger function for manual testing and health checks.
     *
     * @param request HTTP request
     * @param context Function execution context
     * @return HTTP response
     */
    @FunctionName("HealthCheck")
    public HttpResponseMessage healthCheck(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "health")
            HttpRequestMessage<String> request,
            final ExecutionContext context) {

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"status\":\"healthy\",\"service\":\"wis-conversation-orchestrator\"}")
                .build();
    }
}