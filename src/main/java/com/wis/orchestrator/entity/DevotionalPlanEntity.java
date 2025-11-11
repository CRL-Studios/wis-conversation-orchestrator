package com.wis.orchestrator.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import com.wis.orchestrator.model.DailyDevotion;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.List;

/**
 * Entity representing a 7-day devotional plan stored in Cosmos DB.
 * Read-only for orchestrator - managed by message-handler.
 */
@Container(containerName = "devotionalPlans")
public class DevotionalPlanEntity {

    @Id
    private String id;

    @PartitionKey
    private String customerId;

    private Integer planNumber;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private String lifeSeason;
    private List<String> themes;
    private Integer currentDay;
    private List<DailyDevotion> days;
    private String timezone;
    private String preferredTimeOfDay;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean checkInSent;
    private Integer ttl;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Integer getPlanNumber() {
        return planNumber;
    }

    public void setPlanNumber(Integer planNumber) {
        this.planNumber = planNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getLifeSeason() {
        return lifeSeason;
    }

    public void setLifeSeason(String lifeSeason) {
        this.lifeSeason = lifeSeason;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public Integer getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(Integer currentDay) {
        this.currentDay = currentDay;
    }

    public List<DailyDevotion> getDays() {
        return days;
    }

    public void setDays(List<DailyDevotion> days) {
        this.days = days;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getCheckInSent() {
        return checkInSent;
    }

    public void setCheckInSent(Boolean checkInSent) {
        this.checkInSent = checkInSent;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }
}
