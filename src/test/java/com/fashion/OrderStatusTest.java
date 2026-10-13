package com.fashion;

import com.fashion.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void customerMayCancelEarlyStatusesOnly() {
        assertThat(OrderStatus.PLACED.customerCanCancel()).isTrue();
        assertThat(OrderStatus.CONFIRMED.customerCanCancel()).isTrue();
        assertThat(OrderStatus.PROCESSING.customerCanCancel()).isFalse();
        assertThat(OrderStatus.SHIPPED.customerCanCancel()).isFalse();
        assertThat(OrderStatus.DELIVERED.customerCanCancel()).isFalse();
    }

    @Test
    void rejectsInvalidTransitions() {
        assertThat(OrderStatus.PLACED.allowedTransitions()).containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        assertThat(OrderStatus.SHIPPED.allowedTransitions()).containsExactly(OrderStatus.DELIVERED);
        assertThat(OrderStatus.DELIVERED.allowedTransitions()).isEmpty();
        assertThat(OrderStatus.CANCELLED.allowedTransitions()).isEmpty();
        assertThat(OrderStatus.PROCESSING.allowedTransitions()).doesNotContain(OrderStatus.PLACED);
    }
}
