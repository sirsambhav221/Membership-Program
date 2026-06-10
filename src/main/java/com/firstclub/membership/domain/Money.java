package com.firstclub.membership.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, String currency) implements Comparable<Money> {
    public static final String INR = "INR";
    public static final Money ZERO = Money.inr("0");

    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        currency = currency == null || currency.isBlank() ? INR : currency;
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money inr(String amount) {
        return new Money(new BigDecimal(amount), INR);
    }

    public static Money inr(BigDecimal amount) {
        return new Money(amount, INR);
    }

    public Money percentage(int percentage) {
        return Money.inr(amount.multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public Money min(Money other) {
        ensureSameCurrency(other);
        return compareTo(other) <= 0 ? this : other;
    }

    public boolean greaterThanOrEqualTo(Money other) {
        ensureSameCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    private void ensureSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
    }

    @Override
    public int compareTo(Money other) {
        ensureSameCurrency(other);
        return amount.compareTo(other.amount);
    }
}
