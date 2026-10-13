package com.fashion.repository;

import com.fashion.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);

    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0) FROM OrderItem i
            JOIN i.order o
            WHERE i.product.id = :productId
              AND o.status <> com.fashion.entity.OrderStatus.CANCELLED
            """)
    long soldQuantity(@Param("productId") Long productId);

    @Query("""
            SELECT i.product.id, COALESCE(SUM(i.quantity), 0)
            FROM OrderItem i JOIN i.order o
            WHERE o.status <> com.fashion.entity.OrderStatus.CANCELLED
            GROUP BY i.product.id
            """)
    java.util.List<Object[]> soldByProduct();
}
