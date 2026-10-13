package com.fashion.controller;

import com.fashion.dto.PageResponse;
import com.fashion.dto.ProductDtos.CategoryResponse;
import com.fashion.dto.ProductDtos.ProductCardResponse;
import com.fashion.dto.ProductDtos.ProductDetailResponse;
import com.fashion.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return productService.categories();
    }

    @GetMapping("/api/products")
    public PageResponse<ProductCardResponse> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize
    ) {
        return productService.publicSearch(q, categoryId, size, color, minPrice, maxPrice, available, sort, page, pageSize);
    }

    @GetMapping("/api/products/{id}")
    public ProductDetailResponse product(@PathVariable Long id) {
        return productService.publicDetail(id);
    }
}
