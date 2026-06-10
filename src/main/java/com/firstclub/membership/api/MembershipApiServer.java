package com.firstclub.membership.api;

import com.firstclub.membership.domain.BenefitSummary;
import com.firstclub.membership.domain.Money;
import com.firstclub.membership.domain.Plan;
import com.firstclub.membership.domain.PlanCode;
import com.firstclub.membership.domain.Subscription;
import com.firstclub.membership.domain.Tier;
import com.firstclub.membership.domain.User;
import com.firstclub.membership.service.MembershipException;
import com.firstclub.membership.service.MembershipService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MembershipApiServer {
    private final HttpServer server;
    private final MembershipService membershipService;

    private MembershipApiServer(HttpServer server, MembershipService membershipService) {
        this.server = server;
        this.membershipService = membershipService;
    }

    public static MembershipApiServer create(int port, MembershipService membershipService) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        MembershipApiServer apiServer = new MembershipApiServer(server, membershipService);
        server.createContext("/", apiServer::handle);
        return apiServer;
    }

    public void start() {
        server.start();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            send(exchange, 200, route(exchange));
        } catch (MembershipException ex) {
            send(exchange, ex.getStatusCode(), Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            send(exchange, 400, Map.of("error", ex.getMessage()));
        }
    }

    private Object route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        List<String> path = path(exchange);

        if ("GET".equals(method) && path.equals(List.of("health"))) {
            return Map.of("status", "UP");
        }
        if ("GET".equals(method) && path.equals(List.of("membership", "plans"))) {
            return Map.of("plans", membershipService.getPlans().stream().map(this::planJson).toList());
        }
        if ("GET".equals(method) && path.equals(List.of("membership", "tiers"))) {
            return Map.of("tiers", membershipService.getTiers().stream().map(this::tierJson).toList());
        }
        if ("POST".equals(method) && path.equals(List.of("users"))) {
            return createUser(readBody(exchange));
        }

        if (path.size() >= 2 && "users".equals(path.get(0))) {
            String userId = path.get(1);
            return routeUser(method, path, userId, readBodyIfNeeded(exchange));
        }

        throw new MembershipException(404, "Endpoint not found");
    }

    private Object routeUser(String method, List<String> path, String userId, Map<String, Object> body) {
        if ("POST".equals(method) && path.equals(List.of("users", userId, "membership", "subscribe"))) {
            Subscription subscription = membershipService.subscribe(userId, PlanCode.valueOf(required(body, "planCode").toUpperCase()));
            return Map.of("subscription", subscriptionJson(subscription));
        }
        if ("GET".equals(method) && path.equals(List.of("users", userId, "membership"))) {
            return membershipJson(userId);
        }
        if ("POST".equals(method) && path.equals(List.of("users", userId, "orders"))) {
            Subscription subscription = membershipService.recordOrder(userId, money(body.get("amount")));
            return membershipJson(userId, subscription);
        }
        if ("POST".equals(method) && path.equals(List.of("users", userId, "cohorts"))) {
            Subscription subscription = membershipService.updateUserCohorts(userId, strings(body.get("cohortIds")));
            return membershipJson(userId, subscription);
        }
        if ("POST".equals(method) && path.equals(List.of("users", userId, "membership", "benefits", "evaluate"))) {
            BenefitSummary summary = membershipService.evaluateBenefits(
                    userId,
                    money(body.get("orderAmount")),
                    optional(body, "category", "GENERAL"),
                    Boolean.parseBoolean(optional(body, "deliveryEligible", "true"))
            );
            return benefitJson(summary);
        }
        if ("POST".equals(method) && path.equals(List.of("users", userId, "membership", "cancel"))) {
            return Map.of("subscription", subscriptionJson(membershipService.cancel(userId)));
        }
        throw new MembershipException(404, "Endpoint not found");
    }

    private Object createUser(Map<String, Object> body) {
        User user = membershipService.createUser(
                required(body, "userId"),
                required(body, "userName"),
                optional(body, "email", ""),
                strings(body.get("cohortIds"))
        );
        return Map.of("user", userJson(user));
    }

    private Object membershipJson(String userId) {
        return membershipJson(userId, membershipService.getSubscription(userId));
    }

    private Object membershipJson(String userId, Subscription subscription) {
        User user = membershipService.getUser(userId);
        return orderedMap(
                "user", userJson(user),
                "subscription", subscriptionJson(subscription)
        );
    }

    private Map<String, Object> userJson(User user) {
        return orderedMap(
                "userId", user.getUserId(),
                "userName", user.getUserName(),
                "email", user.getEmail(),
                "cohortIds", user.getCohortIds(),
                "orderCount", user.getOrderCount(),
                "currentMonthOrderValue", moneyJson(user.getCurrentMonthOrderValue())
        );
    }

    private Map<String, Object> subscriptionJson(Subscription subscription) {
        return orderedMap(
                "subscriptionId", subscription.getSubscriptionId(),
                "userId", subscription.getUserId(),
                "plan", planJson(subscription.getPlan()),
                "tier", subscription.getTier(),
                "status", subscription.getStatus(),
                "startDate", subscription.getStartDate().toString(),
                "expiryDate", subscription.getExpiryDate().toString()
        );
    }

    private Map<String, Object> planJson(Plan plan) {
        return orderedMap(
                "code", plan.getCode(),
                "name", plan.getName(),
                "price", moneyJson(plan.getPrice()),
                "durationInMonths", plan.getDurationInMonths()
        );
    }

    private Map<String, Object> tierJson(Tier tier) {
        return orderedMap(
                "name", tier,
                "rank", tier.getRank(),
                "benefits", tier.getBenefits()
        );
    }

    private Map<String, Object> benefitJson(BenefitSummary summary) {
        return orderedMap(
                "tier", summary.tier(),
                "freeDelivery", summary.freeDelivery(),
                "discount", moneyJson(summary.discount()),
                "exclusiveDeals", summary.exclusiveDeals(),
                "prioritySupport", summary.prioritySupport(),
                "appliedBenefits", summary.appliedBenefits()
        );
    }

    private Map<String, Object> moneyJson(Money money) {
        return orderedMap("amount", money.amount(), "currency", money.currency());
    }

    private Map<String, Object> readBodyIfNeeded(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            return Map.of();
        }
        return readBody(exchange);
    }

    private Map<String, Object> readBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return Json.parseObject(body);
    }

    private List<String> path(HttpExchange exchange) {
        return Arrays.stream(exchange.getRequestURI().getPath().split("/"))
                .filter(part -> !part.isBlank())
                .toList();
    }

    private String required(Map<String, Object> body, String key) {
        String value = optional(body, key, null);
        if (value == null || value.isBlank()) {
            throw new MembershipException(400, key + " is required");
        }
        return value;
    }

    private String optional(Map<String, Object> body, String key, String defaultValue) {
        Object value = body.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private Money money(Object value) {
        if (value == null) {
            throw new MembershipException(400, "amount is required");
        }
        if (value instanceof BigDecimal decimal) {
            return Money.inr(decimal);
        }
        return Money.inr(new BigDecimal(String.valueOf(value)));
    }

    private Set<String> strings(Object value) {
        if (value == null) {
            return Set.of();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Arrays.stream(String.valueOf(value).split(","))
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.toSet());
    }

    private Map<String, Object> orderedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private void send(HttpExchange exchange, int statusCode, Object response) throws IOException {
        byte[] bytes = Json.stringify(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
