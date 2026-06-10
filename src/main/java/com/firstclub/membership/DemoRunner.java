package com.firstclub.membership;

import com.firstclub.membership.domain.BenefitSummary;
import com.firstclub.membership.domain.Money;
import com.firstclub.membership.domain.Subscription;
import com.firstclub.membership.domain.SubscriptionStatus;
import com.firstclub.membership.domain.Tier;
import com.firstclub.membership.service.MembershipService;

import java.util.Set;

public final class DemoRunner {
    private DemoRunner() {
    }

    public static void main(String[] args) {
        MembershipService service = new MembershipService();

        service.createUser("U1", "Sambhav", "sambhav@example.com", Set.of());
        Subscription subscription = service.subscribe("U1", com.firstclub.membership.domain.PlanCode.YEARLY);
        assertTier(subscription, Tier.SILVER);

        for (int i = 1; i <= 10; i++) {
            subscription = service.recordOrder("U1", Money.inr("500"));
        }
        assertTier(subscription, Tier.GOLD);

        subscription = service.updateUserCohorts("U1", Set.of("PREMIUM"));
        assertTier(subscription, Tier.PLATINUM);

        BenefitSummary benefits = service.evaluateBenefits("U1", Money.inr("2000"), "ELECTRONICS", true);
        if (!benefits.freeDelivery() || benefits.discount().amount().signum() == 0 || !benefits.prioritySupport()) {
            throw new IllegalStateException("Expected Platinum benefits to apply");
        }

        subscription = service.cancel("U1");
        if (subscription.getStatus() != SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Expected subscription to be cancelled");
        }

        System.out.println("Demo passed: subscribe with Silver, upgrade to Gold, upgrade to Platinum, evaluate benefits, cancel.");
    }

    private static void assertTier(Subscription subscription, Tier expectedTier) {
        if (subscription.getTier() != expectedTier) {
            throw new IllegalStateException("Expected " + expectedTier + " but got " + subscription.getTier());
        }
    }
}
