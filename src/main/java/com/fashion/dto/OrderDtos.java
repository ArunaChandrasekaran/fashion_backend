package com.fashion.dto;

import com.fashion.entity.OrderStatus;
import com.fashion.entity.PaymentMethod;
import com.fashion.entity.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record ShippingSnapshot(
            String fullName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country
    ) {
    }

    public record CheckoutRequest(
            Long addressId,
            @Valid AddressInput address,
            @NotNull PaymentMethod paymentMethod
    ) {
    }

    public record AddressInput(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 20) String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 20) String postalCode,
            @Size(max = 80) String country
    ) {
    }

    public record OrderItemResponse(
            Long id,
            String productName,
            String sku,
            String size,
            String color,
            String imageUrl,
            BigDecimal unitPrice,
            BigDecimal discountPercent,
            int quantity,
            BigDecimal lineTotal
    ) {
    }

    public record StatusHistoryResponse(
            OrderStatus fromStatus,
            OrderStatus toStatus,
            Instant createdAt,
            String notes
    ) {
    }

    public record PaymentResponse(
            PaymentMethod method,
            PaymentStatus status,
            BigDecimal amount,
            String provider
    ) {
    }

    public record OrderSummaryResponse(
            Long id,
            String orderNumber,
            OrderStatus status,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            BigDecimal finalTotal,
            Instant createdAt,
            boolean cancellable
    ) {
    }

    public record OrderDetailResponse(
            Long id,
            String orderNumber,
            OrderStatus status,
            PaymentResponse payment,
            ShippingSnapshot shipping,
            List<OrderItemResponse> items,
            List<StatusHistoryResponse> timeline,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingCharge,
            BigDecimal finalTotal,
            Instant createdAt,
            boolean cancellable,
            String customerName,
            String customerEmail
    ) {
    }

    public record StatusUpdateRequest(
            @NotNull OrderStatus status,
            @Size(max = 500) String notes
    ) {
    }
}
