package com.firstclub.membership.service;

import com.firstclub.membership.domain.BenefitSummary;
import com.firstclub.membership.domain.Money;
import com.firstclub.membership.domain.MonthlyPlan;
import com.firstclub.membership.domain.Plan;
import com.firstclub.membership.domain.PlanCode;
import com.firstclub.membership.domain.QuarterlyPlan;
import com.firstclub.membership.domain.Subscription;
import com.firstclub.membership.domain.Tier;
import com.firstclub.membership.domain.User;
import com.firstclub.membership.domain.YearlyPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MembershipService {
    private static final int GOLD_MIN_ORDERS = 10;
    private static final Money GOLD_MIN_MONTH_VALUE = Money.inr("5000");
    private static final int PLATINUM_MIN_ORDERS = 30;
    private static final Money PLATINUM_MIN_MONTH_VALUE = Money.inr("15000");
    private static final Set<String> PLATINUM_COHORTS = Set.of("PREMIUM", "VIP", "FIRSTCLUB_PLUS");

    private final Map<PlanCode, Plan> plans = new EnumMap<>(PlanCode.class);
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Subscription> subscriptionsByUserId = new ConcurrentHashMap<>();

    public MembershipService() {
        registerPlan(new MonthlyPlan());
        registerPlan(new QuarterlyPlan());
        registerPlan(new YearlyPlan());
    }

    public List<Plan> getPlans() {
        return plans.values().stream()
                .sorted(Comparator.comparingInt(Plan::getDurationInMonths))
                .toList();
    }

    public List<Tier> getTiers() {
        return List.of(Tier.SILVER, Tier.GOLD, Tier.PLATINUM);
    }

    public synchronized User createUser(String userId, String userName, String email, Set<String> cohortIds) {
        if (users.containsKey(userId)) {
            throw new MembershipException(409, "User already exists");
        }
        User user = new User(userId, userName, email, cohortIds);
        users.put(userId, user);
        return user;
    }

    public synchronized Subscription subscribe(String userId, PlanCode planCode) {
        User user = getUser(userId);
        Subscription existingSubscription = subscriptionsByUserId.get(user.getUserId());
        if (existingSubscription != null && existingSubscription.isActive()) {
            throw new MembershipException(409, "User already has an active subscription");
        }

        Plan plan = plans.get(planCode);
        if (plan == null) {
            throw new MembershipException(400, "Invalid plan code");
        }

        Subscription subscription = new Subscription(userId, plan);
        subscriptionsByUserId.put(userId, subscription);
        return subscription;
    }

    public synchronized Subscription recordOrder(String userId, Money amount) {
        if (amount.amount().signum() <= 0) {
            throw new MembershipException(400, "Order amount must be positive");
        }
        User user = getUser(userId);
        user.recordOrder(amount);
        return upgradeTierIfEligible(user);
    }

    public synchronized Subscription updateUserCohorts(String userId, Set<String> cohortIds) {
        User user = getUser(userId);
        user.updateCohorts(cohortIds);
        return upgradeTierIfEligible(user);
    }

    public Subscription getSubscription(String userId) {
        getUser(userId);
        Subscription subscription = subscriptionsByUserId.get(userId);
        if (subscription == null) {
            throw new MembershipException(404, "Subscription not found");
        }
        return subscription;
    }

    public User getUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new MembershipException(404, "User not found");
        }
        return user;
    }

    public synchronized Subscription cancel(String userId) {
        Subscription subscription = getSubscription(userId);
        subscription.cancel();
        return subscription;
    }

    public BenefitSummary evaluateBenefits(
            String userId,
            Money orderAmount,
            String category,
            boolean deliveryEligible
    ) {
        Subscription subscription = getSubscription(userId);
        if (!subscription.isActive()) {
            throw new MembershipException(409, "Subscription is not active");
        }

        Tier tier = subscription.getTier();
        boolean freeDelivery = tier.isFreeDeliveryApplicable(orderAmount, deliveryEligible);
        Money discount = tier.calculateDiscount(orderAmount, category);

        List<String> appliedBenefits = new ArrayList<>();
        if (freeDelivery) {
            appliedBenefits.add("Free delivery");
        }
        if (discount.amount().signum() > 0) {
            appliedBenefits.add("Discount: " + discount.amount() + " " + discount.currency());
        }
        if (tier.hasExclusiveDeals()) {
            appliedBenefits.add("Exclusive deals");
        }
        if (tier.hasPrioritySupport()) {
            appliedBenefits.add("Priority support");
        }

        return new BenefitSummary(
                tier,
                freeDelivery,
                discount,
                tier.hasExclusiveDeals(),
                tier.hasPrioritySupport(),
                appliedBenefits
        );
    }

    private Subscription upgradeTierIfEligible(User user) {
        Subscription subscription = getSubscription(user.getUserId());
        if (!subscription.isActive()) {
            return subscription;
        }

        Tier bestTier = evaluateTier(user);
        subscription.upgradeTier(bestTier);
        return subscription;
    }

    private Tier evaluateTier(User user) {
        if (isPlatinumEligible(user)) {
            return Tier.PLATINUM;
        }
        if (isGoldEligible(user)) {
            return Tier.GOLD;
        }
        return Tier.SILVER;
    }

    private boolean isGoldEligible(User user) {
        return user.getOrderCount() >= GOLD_MIN_ORDERS
                || user.getCurrentMonthOrderValue().greaterThanOrEqualTo(GOLD_MIN_MONTH_VALUE);
    }

    private boolean isPlatinumEligible(User user) {
        boolean premiumCohort = user.getCohortIds().stream().anyMatch(PLATINUM_COHORTS::contains);
        return user.getOrderCount() >= PLATINUM_MIN_ORDERS
                || user.getCurrentMonthOrderValue().greaterThanOrEqualTo(PLATINUM_MIN_MONTH_VALUE)
                || premiumCohort;
    }

    private void registerPlan(Plan plan) {
        plans.put(plan.getCode(), plan);
    }
}
