package com.firstclub.membership.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Subscription {
    private final String subscriptionId;
    private final String userId;
    private final Plan plan;
    private final LocalDateTime startDate;
    private final LocalDateTime expiryDate;
    private Tier tier;
    private SubscriptionStatus status;

    public Subscription(String userId, Plan plan) {
        this.subscriptionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.plan = plan;
        this.tier = Tier.SILVER;
        this.status = SubscriptionStatus.ACTIVE;
        this.startDate = LocalDateTime.now();
        this.expiryDate = startDate.plusMonths(plan.getDurationInMonths());
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getUserId() {
        return userId;
    }

    public Plan getPlan() {
        return plan;
    }

    public Tier getTier() {
        return tier;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && expiryDate.isAfter(LocalDateTime.now());
    }

    public void upgradeTier(Tier newTier) {
        if (newTier.getRank() > tier.getRank()) {
            tier = newTier;
        }
    }

    public void cancel() {
        status = SubscriptionStatus.CANCELLED;
    }
}
