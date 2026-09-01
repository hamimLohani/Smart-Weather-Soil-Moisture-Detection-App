package com.agrosense.model;

import java.time.LocalDateTime;

public class Site {
    private int id;
    private int customerId;
    private String name;
    private UseCaseProfile useCaseProfile;
    private LocalDateTime createdAt;

    public Site() {}

    public Site(int id, int customerId, String name, UseCaseProfile useCaseProfile, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.name = name;
        this.useCaseProfile = useCaseProfile;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UseCaseProfile getUseCaseProfile() { return useCaseProfile; }
    public void setUseCaseProfile(UseCaseProfile useCaseProfile) { this.useCaseProfile = useCaseProfile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name + " [" + useCaseProfile + "]"; }
}
