package com.fashion.service;

import com.fashion.dto.PageResponse;
import com.fashion.dto.ProductDtos.CategoryResponse;
import com.fashion.dto.ProductDtos.ImageRequest;
import com.fashion.dto.ProductDtos.ProductCardResponse;
import com.fashion.dto.ProductDtos.ProductDetailResponse;
import com.fashion.dto.ProductDtos.ProductRequest;
import com.fashion.dto.ProductDtos.VariantRequest;
import com.fashion.entity.Category;
import com.fashion.entity.Product;
import com.fashion.entity.ProductImage;
import com.fashion.entity.ProductStatus;
import com.fashion.entity.ProductVariant;
import com.fashion.exception.ApiException;
import com.fashion.repository.CategoryRepository;
import com.fashion.repository.OrderItemRepository;
import com.fashion.repository.ProductRepository;
import com.fashion.repository.ProductSpecifications;
import com.fashion.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final DtoMapper mapper;

    public ProductService(ProductRepository productRepository,
                          ProductVariantRepository variantRepository,
                          CategoryRepository categoryRepository,
                          OrderItemRepository orderItemRepository,
                          DtoMapper mapper) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(mapper::toCategory).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductCardResponse> publicSearch(String q, Long categoryId, String size, String color,
                                                          BigDecimal minPrice, BigDecimal maxPrice, Boolean available,
                                                          String sort, int page, int sizePage) {
        Specification<Product> spec = ProductSpecifications.publicCatalog(q, categoryId, size, color, minPrice, maxPrice, available);
        if ("popularity".equalsIgnoreCase(sort)) {
            List<Product> all = productRepository.findAll(spec);
            Map<Long, Long> sold = orderItemRepository.soldByProduct().stream()
                    .collect(Collectors.toMap(
                            row -> ((Number) row[0]).longValue(),
                            row -> ((Number) row[1]).longValue()));
            all.sort((a, b) -> Long.compare(
                    sold.getOrDefault(b.getId(), 0L),
                    sold.getOrDefault(a.getId(), 0L)));
            int pageIndex = Math.max(page, 0);
            int pageLen = Math.min(Math.max(sizePage, 1), 48);
            int from = pageIndex * pageLen;
            List<Product> slice = from >= all.size() ? List.of() : all.subList(from, Math.min(from + pageLen, all.size()));
            slice.forEach(this::touchCatalog);
            Page<Product> result = new PageImpl<>(slice, PageRequest.of(pageIndex, pageLen), all.size());
            return PageResponse.from(result.map(mapper::toCard));
        }
        Pageable pageable = pageableFor(sort, page, sizePage);
        Page<Product> result = productRepository.findAll(spec, pageable);
        result.getContent().forEach(this::touchCatalog);
        return PageResponse.from(result.map(mapper::toCard));
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse publicDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw ApiException.notFound("Product not found");
        }
        touchCatalog(product);
        return mapper.toDetail(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDetailResponse> adminSearch(String q, Long categoryId, ProductStatus status, String sort, int page, int size) {
        Page<Product> result = productRepository.findAll(
                ProductSpecifications.adminCatalog(q, categoryId, status),
                pageableFor(sort, page, size));
        result.getContent().forEach(this::touchCatalog);
        return PageResponse.from(result.map(mapper::toDetail));
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse adminDetail(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        touchCatalog(product);
        return mapper.toDetail(product);
    }

    @Transactional
    public ProductDetailResponse create(ProductRequest request) {
        validateCatalog(request, null);
        Product product = new Product();
        apply(product, request);
        return mapper.toDetail(productRepository.save(product));
    }

    @Transactional
    public ProductDetailResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        validateCatalog(request, id);
        apply(product, request);
        return mapper.toDetail(product);
    }

    @Transactional
    public ProductDetailResponse updateStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setStatus(status);
        return mapper.toDetail(product);
    }

    @Transactional
    public void archive(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        if (orderItemRepository.existsByProductId(id)) {
            product.setStatus(ProductStatus.ARCHIVED);
            product.getVariants().forEach(v -> v.setActive(false));
            return;
        }
        productRepository.delete(product);
    }

    private void apply(Product product, ProductRequest request) {
        if (request.discountPercent().compareTo(new BigDecimal("100")) > 0) {
            throw ApiException.validation("Discount cannot exceed 100 percent");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.validation("Unknown category"));
        product.setName(request.name().trim());
        product.setDescription(request.description().trim());
        product.setCategory(category);
        product.setPrice(request.price());
        product.setDiscountPercent(request.discountPercent());
        product.setSku(request.sku().trim().toUpperCase());
        product.setStatus(request.status() == null ? ProductStatus.ACTIVE : request.status());

        product.getImages().clear();
        int index = 0;
        for (ImageRequest imageRequest : request.images()) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setUrl(imageRequest.url().trim());
            image.setAltText(imageRequest.altText());
            image.setSortOrder(imageRequest.sortOrder() > 0 ? imageRequest.sortOrder() : index);
            image.setPrimary(imageRequest.primary() || index == 0);
            product.getImages().add(image);
            index++;
        }
        if (product.getImages().stream().noneMatch(ProductImage::isPrimary) && !product.getImages().isEmpty()) {
            product.getImages().getFirst().setPrimary(true);
        }

        Map<Long, ProductVariant> existing = product.getVariants().stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Set<Long> retained = new HashSet<>();
        for (VariantRequest variantRequest : request.variants()) {
            ProductVariant variant;
            if (variantRequest.id() != null) {
                variant = existing.get(variantRequest.id());
                if (variant == null) {
                    throw ApiException.validation("Unknown variant " + variantRequest.id());
                }
                retained.add(variant.getId());
            } else {
                variant = new ProductVariant();
                variant.setProduct(product);
                product.getVariants().add(variant);
            }
            variant.setSize(variantRequest.size().trim().toUpperCase());
            variant.setColor(variantRequest.color().trim());
            variant.setSku(variantRequest.sku().trim().toUpperCase());
            variant.setStockQuantity(variantRequest.stockQuantity());
            variant.setActive(variantRequest.active());
        }
        product.getVariants().removeIf(v -> v.getId() != null && !retained.contains(v.getId()));
    }

    private void validateCatalog(ProductRequest request, Long productId) {
        String sku = request.sku().trim();
        boolean skuTaken = productId == null
                ? productRepository.existsBySkuIgnoreCase(sku)
                : productRepository.existsBySkuIgnoreCaseAndIdNot(sku, productId);
        if (skuTaken) {
            throw ApiException.conflict("Product SKU must be unique");
        }
        Set<String> variantSkus = new HashSet<>();
        Set<String> sizeColor = new HashSet<>();
        for (VariantRequest variant : request.variants()) {
            if (!variantSkus.add(variant.sku().trim().toUpperCase())) {
                throw ApiException.validation("Variant SKUs must be unique");
            }
            if (!sizeColor.add(variant.size().trim().toUpperCase() + "|" + variant.color().trim().toLowerCase())) {
                throw ApiException.validation("Each size and color combination must be unique");
            }
            boolean taken = variant.id() == null
                    ? variantRepository.existsBySkuIgnoreCase(variant.sku())
                    : variantRepository.existsBySkuIgnoreCaseAndIdNot(variant.sku(), variant.id());
            if (taken) {
                throw ApiException.conflict("Variant SKU " + variant.sku() + " is already in use");
            }
        }
    }

    private Pageable pageableFor(String sort, int page, int size) {
        Sort springSort = switch (sort == null ? "newest" : sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48), springSort);
    }

    private void touchCatalog(Product product) {
        product.getCategory().getName();
        product.getImages().size();
        product.getVariants().size();
    }
}
