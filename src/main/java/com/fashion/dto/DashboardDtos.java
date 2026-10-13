package com.fashion.dto;

import com.fashion.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record KpiResponse(
            BigDecimal totalRevenue,
            long totalOrders,
            long totalCustomers,
            long activeProducts,
            long pendingOrders,
            long lowStockProducts
    ) {
    }

    public record SalesPoint(String bucket, BigDecimal total, long orders) {
    }

    public record LowStockItem(Long variantId, String productName, String sku, String size, String color, int stockQuantity) {
    }

    public record RecentOrder(
            Long id,
            String orderNumber,
            String customerName,
            OrderStatus status,
            BigDecimal finalTotal,
            Instant createdAt
    ) {
    }

    public record DashboardResponse(
            KpiResponse kpis,
            List<SalesPoint> sales,
            List<RecentOrder> recentOrders,
            List<LowStockItem> lowStock
    ) {
    }
}
