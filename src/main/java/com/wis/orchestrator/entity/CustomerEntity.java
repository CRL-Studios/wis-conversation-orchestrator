package com.wis.orchestrator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * Minimal customer entity for orchestrator.
 * Full version managed in message-handler.
 */
@Container(containerName = "customers")
public class CustomerEntity {

    @Id
    @PartitionKey
    private String id; // Customers use id as partition key

    private String currentPhone;
    private String activePlanId;
    private String status;
    private MessagingState messagingState;

    public static class MessagingState {
        private Instant nextPlanMessageScheduledFor;
        private String timezone;
        private String preferredTimeOfDay;

        public Instant getNextPlanMessageScheduledFor() {
            return nextPlanMessageScheduledFor;
        }

        public void setNextPlanMessageScheduledFor(Instant nextPlanMessageScheduledFor) {
            this.nextPlanMessageScheduledFor = nextPlanMessageScheduledFor;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getPreferredTimeOfDay() {
            return preferredTimeOfDay;
        }

        public void setPreferredTimeOfDay(String preferredTimeOfDay) {
            this.preferredTimeOfDay = preferredTimeOfDay;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurrentPhone() {
        return currentPhone;
    }

    public void setCurrentPhone(String currentPhone) {
        this.currentPhone = currentPhone;
    }

    public String getActivePlanId() {
        return activePlanId;
    }

    public void setActivePlanId(String activePlanId) {
        this.activePlanId = activePlanId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public MessagingState getMessagingState() {
        return messagingState;
    }

    public void setMessagingState(MessagingState messagingState) {
        this.messagingState = messagingState;
    }
}
