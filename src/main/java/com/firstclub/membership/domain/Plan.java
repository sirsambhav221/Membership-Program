package com.firstclub.membership.domain;

public interface Plan {
    PlanCode getCode();

    String getName();

    Money getPrice();

    int getDurationInMonths();
}
