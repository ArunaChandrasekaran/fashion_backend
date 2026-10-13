package com.fashion.repository;

import com.fashion.entity.Order;
import com.fashion.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);

    @Query("""
            SELECT SUM(o.finalTotal) FROM Order o
            WHERE o.status <> com.fashion.entity.OrderStatus.CANCELLED
              AND o.createdAt >= :from AND o.createdAt < :to
            """)
    BigDecimal sumEligibleRevenue(@Param("from") Instant from, @Param("to") Instant to);

    long countByStatusIn(Collection<OrderStatus> statuses);

    long countByUserId(Long userId);

    @Query("SELECT SUM(o.finalTotal) FROM Order o WHERE o.user.id = :userId AND o.status <> com.fashion.entity.OrderStatus.CANCELLED")
    BigDecimal sumSpentByUser(@Param("userId") Long userId);

    @Query("""
            SELECT o FROM Order o
            WHERE o.createdAt >= :from AND o.createdAt < :to
              AND (:hasStatus = FALSE OR o.status = :status)
              AND (:q = '' OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(o.user.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Order> searchAdmin(@Param("q") String q,
                            @Param("hasStatus") boolean hasStatus,
                            @Param("status") OrderStatus status,
                            @Param("from") Instant from,
                            @Param("to") Instant to,
                            Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.user
            ORDER BY o.createdAt DESC
            """)
    List<Order> findRecentWithCustomer();

    @Query("""
            SELECT o FROM Order o
            WHERE o.status <> com.fashion.entity.OrderStatus.CANCELLED
              AND o.createdAt >= :from AND o.createdAt < :to
            ORDER BY o.createdAt ASC
            """)
    List<Order> findSalesInRange(@Param("from") Instant from, @Param("to") Instant to);
}
