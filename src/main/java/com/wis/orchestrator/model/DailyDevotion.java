package com.wis.orchestrator.model;

import java.time.Instant;

/**
 * Represents a single day's devotional content within a 7-day plan.
 * Minimal model for orchestrator - full version in message-handler.
 */
public class DailyDevotion {

    private Integer dayNumber;
    private String verseReference;
    private String verseText;
    private String reflection;
    private String journalPrompt;
    private Instant sentAt;
    private String status;
    private String twilioSid;

    // Getters and Setters
    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getVerseReference() {
        return verseReference;
    }

    public void setVerseReference(String verseReference) {
        this.verseReference = verseReference;
    }

    public String getVerseText() {
        return verseText;
    }

    public void setVerseText(String verseText) {
        this.verseText = verseText;
    }

    public String getReflection() {
        return reflection;
    }

    public void setReflection(String reflection) {
        this.reflection = reflection;
    }

    public String getJournalPrompt() {
        return journalPrompt;
    }

    public void setJournalPrompt(String journalPrompt) {
        this.journalPrompt = journalPrompt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTwilioSid() {
        return twilioSid;
    }

    public void setTwilioSid(String twilioSid) {
        this.twilioSid = twilioSid;
    }
}
