package com.fashion.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PLACED -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean customerCanCancel() {
        return this == PLACED || this == CONFIRMED;
    }

    public boolean adminCanCancel() {
        return this == PLACED || this == CONFIRMED || this == PROCESSING;
    }

    public boolean countsTowardRevenue() {
        return this != CANCELLED;
    }
}
