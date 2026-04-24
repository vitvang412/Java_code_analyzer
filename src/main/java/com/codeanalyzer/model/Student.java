package com.codeanalyzer.model;

import java.time.LocalDateTime;

public class Student {
    private int id;
    private String username;
    private PlatformType platform;
    private String displayName;
    private LocalDateTime addedAt;
    private LocalDateTime lastCrawledAt;
    private boolean active;

    public Student() {}

    public Student(String username, PlatformType platform) {
        this.username = username;
        this.platform = platform;
        this.addedAt = LocalDateTime.now();
        this.active = true;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public PlatformType getPlatform() { return platform; }
    public void setPlatform(PlatformType platform) { this.platform = platform; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public LocalDateTime getLastCrawledAt() { return lastCrawledAt; }
    public void setLastCrawledAt(LocalDateTime lastCrawledAt) { this.lastCrawledAt = lastCrawledAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return username + " [" + (platform != null ? platform.getDisplayName() : "?") + "]";
    }
}
