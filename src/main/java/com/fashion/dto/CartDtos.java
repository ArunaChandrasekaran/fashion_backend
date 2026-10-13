package com.fashion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddCartItemRequest(
            @NotNull Long variantId,
            @Min(1) int quantity
    ) {
    }

    public record UpdateCartItemRequest(
            @Min(1) int quantity
    ) {
    }

    public record CartItemResponse(
            Long id,
            Long productId,
            Long variantId,
            String productName,
            String imageUrl,
            String size,
            String color,
            String sku,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            BigDecimal lineTotal,
            int quantity,
            int availableStock,
            boolean available,
            String warning
    ) {
    }

    public record CartTotals(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingCharge,
            BigDecimal finalTotal,
            boolean freeShipping
    ) {
    }

    public record CartResponse(
            Long id,
            List<CartItemResponse> items,
            CartTotals totals,
            List<String> notices
    ) {
    }
}
