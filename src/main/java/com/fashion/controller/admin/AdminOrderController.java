package com.fashion.controller.admin;

import com.fashion.dto.OrderDtos.OrderDetailResponse;
import com.fashion.dto.OrderDtos.OrderSummaryResponse;
import com.fashion.dto.OrderDtos.StatusUpdateRequest;
import com.fashion.dto.PageResponse;
import com.fashion.entity.OrderStatus;
import com.fashion.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return orderService.adminSearch(q, status, from, to, page, size);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse detail(@PathVariable Long id) {
        return orderService.adminDetail(id);
    }

    @PatchMapping("/{id}/status")
    public OrderDetailResponse status(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return orderService.adminUpdateStatus(id, request.status(), request.notes());
    }
}
