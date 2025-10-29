package com.wis.orchestrator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Event received from Service Bus when a customer completes registration.
 */
public class CustomerRegisteredEvent {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("eventTime")
    private Instant eventTime;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("data")
    private CustomerData data;

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

    public CustomerData getData() {
        return data;
    }

    public void setData(CustomerData data) {
        this.data = data;
    }

    public static class CustomerData {

        @JsonProperty("customerId")
        private String customerId;

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("registrationStage")
        private String registrationStage;

        @JsonProperty("createdAt")
        private Instant createdAt;

        // Getters and Setters
        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getRegistrationStage() {
            return registrationStage;
        }

        public void setRegistrationStage(String registrationStage) {
            this.registrationStage = registrationStage;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
}
