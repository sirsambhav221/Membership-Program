package com.firstclub.membership.domain;

public final class QuarterlyPlan implements Plan {
    @Override
    public PlanCode getCode() {
        return PlanCode.QUARTERLY;
    }

    @Override
    public String getName() {
        return "Quarterly Plan";
    }

    @Override
    public Money getPrice() {
        return Money.inr("549");
    }

    @Override
    public int getDurationInMonths() {
        return 3;
    }
}
