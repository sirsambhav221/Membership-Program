package com.firstclub.membership.domain;

import java.util.HashSet;
import java.util.Set;

public final class User {
    private final String userId;
    private final String userName;
    private final String email;
    private final Set<String> cohortIds = new HashSet<>();
    private int orderCount;
    private Money currentMonthOrderValue = Money.ZERO;

    public User(String userId, String userName, String email, Set<String> cohortIds) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("userName is required");
        }
        this.userId = userId;
        this.userName = userName;
        this.email = email == null ? "" : email;
        updateCohorts(cohortIds);
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getCohortIds() {
        return Set.copyOf(cohortIds);
    }

    public int getOrderCount() {
        return orderCount;
    }

    public Money getCurrentMonthOrderValue() {
        return currentMonthOrderValue;
    }

    public void recordOrder(Money amount) {
        orderCount++;
        currentMonthOrderValue = Money.inr(currentMonthOrderValue.amount().add(amount.amount()));
    }

    public void updateCohorts(Set<String> newCohorts) {
        cohortIds.clear();
        if (newCohorts == null) {
            return;
        }
        for (String cohort : newCohorts) {
            if (cohort != null && !cohort.isBlank()) {
                cohortIds.add(cohort.trim().toUpperCase());
            }
        }
    }
}
