package com.fashion;

import com.fashion.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void calculatesDiscountedLineTotal() {
        BigDecimal total = Money.lineTotal(new BigDecimal("18999.00"), new BigDecimal("10.00"), 2);
        assertThat(total).isEqualByComparingTo("34198.20");
    }

    @Test
    void neverUsesMoreThanTwoDecimals() {
        assertThat(Money.discountAmount(new BigDecimal("7599.00"), new BigDecimal("7.00"), 1).scale()).isEqualTo(2);
    }
}
