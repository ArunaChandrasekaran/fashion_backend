package com.fashion.service;

import com.fashion.config.FashionProperties;
import com.fashion.dto.CartDtos.CartTotals;
import com.fashion.util.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

    private final FashionProperties properties;

    public PricingService(FashionProperties properties) {
        this.properties = properties;
    }

    public CartTotals totals(BigDecimal subtotal, BigDecimal discountAmount, boolean empty) {
        BigDecimal scaledSubtotal = Money.scale(subtotal);
        BigDecimal scaledDiscount = Money.scale(discountAmount);
        if (empty) {
            return new CartTotals(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, false);
        }
        BigDecimal afterDiscount = Money.scale(scaledSubtotal.subtract(scaledDiscount));
        boolean free = afterDiscount.compareTo(properties.getShipping().getThreshold()) >= 0;
        BigDecimal shipping = free ? Money.ZERO : Money.scale(properties.getShipping().getFee());
        return new CartTotals(scaledSubtotal, scaledDiscount, shipping, Money.scale(afterDiscount.add(shipping)), free);
    }
}
