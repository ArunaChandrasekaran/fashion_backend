package com.fashion.service;

import com.fashion.config.FashionProperties;
import com.fashion.dto.DashboardDtos.DashboardResponse;
import com.fashion.dto.DashboardDtos.KpiResponse;
import com.fashion.dto.DashboardDtos.LowStockItem;
import com.fashion.dto.DashboardDtos.RecentOrder;
import com.fashion.dto.DashboardDtos.SalesPoint;
import com.fashion.entity.Order;
import com.fashion.entity.OrderStatus;
import com.fashion.entity.ProductStatus;
import com.fashion.repository.OrderRepository;
import com.fashion.repository.ProductRepository;
import com.fashion.repository.ProductVariantRepository;
import com.fashion.repository.UserRepository;
import com.fashion.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final FashionProperties properties;

    public DashboardService(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            ProductVariantRepository variantRepository,
                            FashionProperties properties) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public DashboardResponse load(Instant from, Instant to, String granularity) {
        Instant start = from == null ? Instant.now().minusSeconds(30L * 24 * 3600) : from;
        Instant end = to == null ? Instant.now() : to;
        boolean monthly = "month".equalsIgnoreCase(granularity);

        KpiResponse kpis = new KpiResponse(
                Money.scale(orderRepository.sumEligibleRevenue(start, end)),
                orderRepository.count(),
                userRepository.countCustomers(),
                productRepository.countByStatus(ProductStatus.ACTIVE),
                orderRepository.countByStatusIn(EnumSet.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PROCESSING)),
                variantRepository.findLowStock(properties.getLowStockThreshold()).stream()
                        .map(v -> v.getProduct().getId()).distinct().count()
        );

        List<Order> salesOrders = orderRepository.findSalesInRange(start, end);
        Map<String, SalesAccumulator> buckets = new LinkedHashMap<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Order order : salesOrders) {
            String key = monthly
                    ? YearMonth.from(order.getCreatedAt().atZone(ZoneOffset.UTC)).format(monthFmt)
                    : LocalDate.ofInstant(order.getCreatedAt(), ZoneOffset.UTC).format(dayFmt);
            SalesAccumulator acc = buckets.computeIfAbsent(key, ignored -> new SalesAccumulator());
            acc.total = acc.total.add(order.getFinalTotal());
            acc.orders++;
        }
        List<SalesPoint> sales = new ArrayList<>();
        buckets.forEach((key, acc) -> sales.add(new SalesPoint(key, Money.scale(acc.total), acc.orders)));

        List<RecentOrder> recent = orderRepository.findRecentWithCustomer().stream()
                .limit(8)
                .map(o -> new RecentOrder(o.getId(), o.getOrderNumber(), o.getUser().getFullName(), o.getStatus(),
                        Money.scale(o.getFinalTotal()), o.getCreatedAt()))
                .toList();

        List<LowStockItem> lowStock = variantRepository.findLowStock(properties.getLowStockThreshold()).stream()
                .limit(8)
                .map(v -> new LowStockItem(v.getId(), v.getProduct().getName(), v.getSku(), v.getSize(), v.getColor(), v.getStockQuantity()))
                .toList();

        return new DashboardResponse(kpis, sales, recent, lowStock);
    }

    private static final class SalesAccumulator {
        private BigDecimal total = BigDecimal.ZERO;
        private long orders;
    }
}
