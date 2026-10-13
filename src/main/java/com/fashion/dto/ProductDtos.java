package com.fashion.dto;

import com.fashion.entity.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CategoryResponse(Long id, String name, String slug, String description) {
    }

    public record ImageRequest(
            @NotBlank @Size(max = 1000) String url,
            @Size(max = 200) String altText,
            int sortOrder,
            boolean primary
    ) {
    }

    public record VariantRequest(
            Long id,
            @NotBlank @Size(max = 20) String size,
            @NotBlank @Size(max = 40) String color,
            @NotBlank @Size(max = 64) String sku,
            @Min(0) int stockQuantity,
            boolean active
    ) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank String description,
            @NotNull Long categoryId,
            @NotNull @DecimalMin(value = "0.00") BigDecimal price,
            @NotNull @DecimalMin(value = "0.00") BigDecimal discountPercent,
            @NotBlank @Size(max = 64) String sku,
            ProductStatus status,
            @Valid @NotEmpty List<ImageRequest> images,
            @Valid @NotEmpty List<VariantRequest> variants
    ) {
    }

    public record ImageResponse(Long id, String url, String altText, int sortOrder, boolean primary) {
    }

    public record VariantResponse(Long id, String size, String color, String sku, int stockQuantity, boolean active) {
    }

    public record ProductCardResponse(
            Long id,
            String name,
            String category,
            String categorySlug,
            BigDecimal price,
            BigDecimal discountPercent,
            BigDecimal salePrice,
            String primaryImage,
            List<String> sizes,
            List<String> colors,
            boolean inStock
    ) {
    }

    public record ProductDetailResponse(
            Long id,
            String name,
            String description,
            CategoryResponse category,
            BigDecimal price,
            BigDecimal discountPercent,
            BigDecimal salePrice,
            String sku,
            ProductStatus status,
            Instant createdAt,
            List<ImageResponse> images,
            List<VariantResponse> variants
    ) {
    }

    public record ProductStatusRequest(@NotNull ProductStatus status) {
    }
}
