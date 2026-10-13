package com.fashion.repository;

import com.fashion.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT v FROM ProductVariant v
            JOIN FETCH v.product p
            WHERE v.active = true AND p.status = com.fashion.entity.ProductStatus.ACTIVE
              AND v.stockQuantity <= :threshold
            ORDER BY v.stockQuantity ASC
            """)
    List<ProductVariant> findLowStock(@Param("threshold") int threshold);

    long countByProductId(Long productId);
}
