package com.fashion.repository;

import com.fashion.entity.Product;
import com.fashion.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    long countByStatus(ProductStatus status);

    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.images
            LEFT JOIN FETCH p.variants
            LEFT JOIN FETCH p.category
            WHERE p.id = :id
            """)
    Optional<Product> findDetailedById(@Param("id") Long id);
}
