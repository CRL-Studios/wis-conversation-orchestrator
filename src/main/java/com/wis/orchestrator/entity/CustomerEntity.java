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
    private Profile profile;
    private MessagingState messagingState;
    private BetaProgram betaProgram;

    public static class Profile {
        private String firstName;
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

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

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public MessagingState getMessagingState() {
        return messagingState;
    }

    public void setMessagingState(MessagingState messagingState) {
        this.messagingState = messagingState;
    }

    public BetaProgram getBetaProgram() {
        return betaProgram;
    }

    public void setBetaProgram(BetaProgram betaProgram) {
        this.betaProgram = betaProgram;
    }

    /**
     * Beta program enrollment information.
     */
    public static class BetaProgram {
        private String betaCode;
        private Instant enrolledAt;
        private Instant checkoutSurveySentAt;
        private Instant day7SurveySentAt;
        private String checkoutSurveyUrl;
        private String day7SurveyUrl;

        public String getBetaCode() {
            return betaCode;
        }

        public void setBetaCode(String betaCode) {
            this.betaCode = betaCode;
        }

        public Instant getEnrolledAt() {
            return enrolledAt;
        }

        public void setEnrolledAt(Instant enrolledAt) {
            this.enrolledAt = enrolledAt;
        }

        public Instant getCheckoutSurveySentAt() {
            return checkoutSurveySentAt;
        }

        public void setCheckoutSurveySentAt(Instant checkoutSurveySentAt) {
            this.checkoutSurveySentAt = checkoutSurveySentAt;
        }

        public Instant getDay7SurveySentAt() {
            return day7SurveySentAt;
        }

        public void setDay7SurveySentAt(Instant day7SurveySentAt) {
            this.day7SurveySentAt = day7SurveySentAt;
        }

        public String getCheckoutSurveyUrl() {
            return checkoutSurveyUrl;
        }

        public void setCheckoutSurveyUrl(String checkoutSurveyUrl) {
            this.checkoutSurveyUrl = checkoutSurveyUrl;
        }

        public String getDay7SurveyUrl() {
            return day7SurveyUrl;
        }

        public void setDay7SurveyUrl(String day7SurveyUrl) {
            this.day7SurveyUrl = day7SurveyUrl;
        }
    }
}
