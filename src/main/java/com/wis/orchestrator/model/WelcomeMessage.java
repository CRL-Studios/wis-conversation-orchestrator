package com.wis.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Welcome message payload sent to the message-send-queue.
 * This message will be processed by the message handler to send the first SMS.
 */
public class WelcomeMessage {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("conversationId")
    private String conversationId;

    @JsonProperty("to")
    private String to;

    @JsonProperty("body")
    private String body;

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("metadata")
    private Metadata metadata;

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private WelcomeMessage message = new WelcomeMessage();

        public Builder messageId(String messageId) {
            message.messageId = messageId;
            return this;
        }

        public Builder customerId(String customerId) {
            message.customerId = customerId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            message.conversationId = conversationId;
            return this;
        }

        public Builder to(String to) {
            message.to = to;
            return this;
        }

        public Builder body(String body) {
            message.body = body;
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

        public Builder metadata(Metadata metadata) {
            message.metadata = metadata;
            return this;
        }

        public WelcomeMessage build() {
            return message;
        }
    }

    public static class Metadata {
        @JsonProperty("registrationEventId")
        private String registrationEventId;

        @JsonProperty("registrationStage")
        private String registrationStage;

        @JsonProperty("attempt")
        private int attempt;

        @JsonProperty("maxRetries")
        private int maxRetries;

        public static MetadataBuilder builder() {
            return new MetadataBuilder();
        }

        public static class MetadataBuilder {
            private Metadata metadata = new Metadata();

            public MetadataBuilder registrationEventId(String registrationEventId) {
                metadata.registrationEventId = registrationEventId;
                return this;
            }

            public MetadataBuilder registrationStage(String registrationStage) {
                metadata.registrationStage = registrationStage;
                return this;
            }

            public MetadataBuilder attempt(int attempt) {
                metadata.attempt = attempt;
                return this;
            }

            public MetadataBuilder maxRetries(int maxRetries) {
                metadata.maxRetries = maxRetries;
                return this;
            }

            public Metadata build() {
                return metadata;
            }
        }

        // Getters and Setters
        public String getRegistrationEventId() {
            return registrationEventId;
        }

        public void setRegistrationEventId(String registrationEventId) {
            this.registrationEventId = registrationEventId;
        }

        public String getRegistrationStage() {
            return registrationStage;
        }

        public void setRegistrationStage(String registrationStage) {
            this.registrationStage = registrationStage;
        }

        public int getAttempt() {
            return attempt;
        }

        public void setAttempt(int attempt) {
            this.attempt = attempt;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}