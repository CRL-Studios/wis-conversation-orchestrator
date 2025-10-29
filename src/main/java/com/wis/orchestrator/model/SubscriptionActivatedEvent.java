package com.wis.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Event received when a subscription is activated after successful payment.
 * Published by wis-subscriptions service.
 */
public class SubscriptionActivatedEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("eventTime")
    private Instant eventTime;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("data")
    private SubscriptionData data;

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public SubscriptionData getData() {
        return data;
    }

    public void setData(SubscriptionData data) {
        this.data = data;
    }

    /**
     * Subscription activation data.
     */
    public static class SubscriptionData {

        @JsonProperty("customerId")
        private String customerId;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("subscriptionId")
        private String subscriptionId;

        @JsonProperty("stripePriceId")
        private String stripePriceId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("activatedAt")
        private Instant activatedAt;

        // Getters and Setters
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

        public String getSubscriptionId() {
            return subscriptionId;
        }

        public void setSubscriptionId(String subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        public String getStripePriceId() {
            return stripePriceId;
        }

        public void setStripePriceId(String stripePriceId) {
            this.stripePriceId = stripePriceId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getActivatedAt() {
            return activatedAt;
        }

        public void setActivatedAt(Instant activatedAt) {
            this.activatedAt = activatedAt;
        }
    }
}