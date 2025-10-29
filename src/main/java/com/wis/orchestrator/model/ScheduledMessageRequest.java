package com.wis.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Message request for scheduled devotionals or check-ins.
 * Sent to message-send-queue for processing by wis-message-handler.
 */
public class ScheduledMessageRequest {

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("messageType")
    private String messageType; // "daily_devotional", "season_check_in"

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("message")
    private String message; // Pre-generated message (for check-ins), or null (for devotionals - AI generates)

    @JsonProperty("themes")
    private List<String> themes; // User's themes for AI personalization

    @JsonProperty("lifeSeason")
    private String lifeSeason; // User's current life season for context

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ScheduledMessageRequest message = new ScheduledMessageRequest();

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

        public Builder message(String messageText) {
            message.message = messageText;
            return this;
        }

        public Builder themes(List<String> themes) {
            message.themes = themes;
            return this;
        }

        public Builder lifeSeason(String lifeSeason) {
            message.lifeSeason = lifeSeason;
            return this;
        }

        public ScheduledMessageRequest build() {
            return message;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public String getLifeSeason() {
        return lifeSeason;
    }

    public void setLifeSeason(String lifeSeason) {
        this.lifeSeason = lifeSeason;
    }
}