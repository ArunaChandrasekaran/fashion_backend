package com.fashion.repository;

import com.fashion.entity.Product;
import com.fashion.entity.ProductStatus;
import com.fashion.entity.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> publicCatalog(String q, Long categoryId, String size, String color,
                                                       BigDecimal minPrice, BigDecimal maxPrice, Boolean available) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("sku")), like)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if ((size != null && !size.isBlank()) || (color != null && !color.isBlank()) || Boolean.TRUE.equals(available)) {
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.INNER);
                predicates.add(cb.isTrue(variants.get("active")));
                if (size != null && !size.isBlank()) {
                    predicates.add(cb.equal(cb.lower(variants.get("size")), size.toLowerCase()));
                }
                if (color != null && !color.isBlank()) {
                    predicates.add(cb.equal(cb.lower(variants.get("color")), color.toLowerCase()));
                }
                if (Boolean.TRUE.equals(available)) {
                    predicates.add(cb.greaterThan(variants.get("stockQuantity"), 0));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Product> adminCatalog(String q, Long categoryId, ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Product> orderByPopularity() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                Subquery<Long> sold = query.subquery(Long.class);
                Root<com.fashion.entity.OrderItem> item = sold.from(com.fashion.entity.OrderItem.class);
                sold.select(cb.coalesce(cb.sum(item.get("quantity")), 0L));
                sold.where(cb.equal(item.get("product").get("id"), root.get("id")));
                query.orderBy(cb.desc(sold), cb.desc(root.get("createdAt")));
            }
            return cb.conjunction();
        };
    }
}
