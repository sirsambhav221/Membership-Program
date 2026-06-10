package com.firstclub.membership.domain;

public final class YearlyPlan implements Plan {
    @Override
    public PlanCode getCode() {
        return PlanCode.YEARLY;
    }

    @Override
    public String getName() {
        return "Yearly Plan";
    }

    @Override
    public Money getPrice() {
        return Money.inr("1999");
    }

    @Override
    public int getDurationInMonths() {
        return 12;
    }
}
