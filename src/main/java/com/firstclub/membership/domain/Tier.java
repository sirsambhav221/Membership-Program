package com.firstclub.membership.domain;

import java.util.List;
import java.util.Set;

public enum Tier {
    SILVER(
            1,
            Money.inr("499"),
            5,
            Money.inr("100"),
            Set.of("GROCERY", "FASHION"),
            false,
            false
    ),
    GOLD(
            2,
            Money.inr("299"),
            10,
            Money.inr("250"),
            Set.of("GROCERY", "FASHION", "ELECTRONICS"),
            true,
            false
    ),
    PLATINUM(
            3,
            Money.ZERO,
            15,
            Money.inr("500"),
            Set.of("GROCERY", "FASHION", "ELECTRONICS"),
            true,
            true
    );

    private final int rank;
    private final Money freeDeliveryMinimum;
    private final int discountPercentage;
    private final Money maxDiscount;
    private final Set<String> discountCategories;
    private final boolean exclusiveDeals;
    private final boolean prioritySupport;

    Tier(
            int rank,
            Money freeDeliveryMinimum,
            int discountPercentage,
            Money maxDiscount,
            Set<String> discountCategories,
            boolean exclusiveDeals,
            boolean prioritySupport
    ) {
        this.rank = rank;
        this.freeDeliveryMinimum = freeDeliveryMinimum;
        this.discountPercentage = discountPercentage;
        this.maxDiscount = maxDiscount;
        this.discountCategories = discountCategories;
        this.exclusiveDeals = exclusiveDeals;
        this.prioritySupport = prioritySupport;
    }

    public int getRank() {
        return rank;
    }

    public List<String> getBenefits() {
        String deliveryText = freeDeliveryMinimum.amount().signum() == 0
                ? "Free delivery on all eligible orders"
                : "Free delivery above " + freeDeliveryMinimum.amount() + " " + freeDeliveryMinimum.currency();

        return List.of(
                deliveryText,
                discountPercentage + "% discount on " + discountCategories + " up to " + maxDiscount.amount() + " " + maxDiscount.currency(),
                exclusiveDeals ? "Exclusive deals and early sale access" : "Standard member deals",
                prioritySupport ? "Priority support" : "Standard support"
        );
    }

    public boolean isFreeDeliveryApplicable(Money orderAmount, boolean deliveryEligible) {
        return deliveryEligible && orderAmount.greaterThanOrEqualTo(freeDeliveryMinimum);
    }

    public Money calculateDiscount(Money orderAmount, String category) {
        String normalizedCategory = category == null ? "GENERAL" : category.toUpperCase();
        if (!discountCategories.contains(normalizedCategory)) {
            return Money.ZERO;
        }
        return orderAmount.percentage(discountPercentage).min(maxDiscount);
    }

    public boolean hasExclusiveDeals() {
        return exclusiveDeals;
    }

    public boolean hasPrioritySupport() {
        return prioritySupport;
    }
}
