package com.firstclub.membership.domain;

import java.util.List;

public record BenefitSummary(
        Tier tier,
        boolean freeDelivery,
        Money discount,
        boolean exclusiveDeals,
        boolean prioritySupport,
        List<String> appliedBenefits
) {
}
