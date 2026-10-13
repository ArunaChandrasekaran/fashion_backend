package com.fashion.controller;

import com.fashion.dto.OrderDtos.CheckoutRequest;
import com.fashion.dto.OrderDtos.OrderDetailResponse;
import com.fashion.dto.OrderDtos.OrderSummaryResponse;
import com.fashion.dto.PageResponse;
import com.fashion.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/orders/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse checkout(@Valid @RequestBody CheckoutRequest request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return orderService.checkout(request, idempotencyKey);
    }

    @GetMapping("/api/orders")
    public PageResponse<OrderSummaryResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return orderService.myOrders(page, size);
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderDetailResponse detail(@PathVariable Long orderId) {
        return orderService.myOrder(orderId);
    }

    @PostMapping("/api/orders/{orderId}/cancel")
    public OrderDetailResponse cancel(@PathVariable Long orderId) {
        return orderService.cancelMine(orderId);
    }
}
