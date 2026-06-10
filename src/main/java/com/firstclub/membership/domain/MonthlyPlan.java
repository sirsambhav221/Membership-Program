package com.firstclub.membership.domain;

public final class MonthlyPlan implements Plan {
    @Override
    public PlanCode getCode() {
        return PlanCode.MONTHLY;
    }

    @Override
    public String getName() {
        return "Monthly Plan";
    }

    @Override
    public Money getPrice() {
        return Money.inr("199");
    }

    @Override
    public int getDurationInMonths() {
        return 1;
    }
}
