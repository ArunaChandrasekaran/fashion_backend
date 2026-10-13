package com.fashion.controller.admin;

import com.fashion.dto.AuthDtos.MessageResponse;
import com.fashion.dto.PageResponse;
import com.fashion.dto.ProductDtos.ProductDetailResponse;
import com.fashion.dto.ProductDtos.ProductRequest;
import com.fashion.dto.ProductDtos.ProductStatusRequest;
import com.fashion.entity.ProductStatus;
import com.fashion.service.ImageStorageService;
import com.fashion.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorageService;

    public AdminProductController(ProductService productService, ImageStorageService imageStorageService) {
        this.productService = productService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public PageResponse<ProductDetailResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return productService.adminSearch(q, categoryId, status, sort, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.adminDetail(id);
    }

    @PatchMapping("/{id}")
    public ProductDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductDetailResponse status(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        return productService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public MessageResponse archive(@PathVariable Long id) {
        productService.archive(id);
        return new MessageResponse("Product archived or removed");
    }

    @PostMapping("/images")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        return Map.of("url", imageStorageService.store(file));
    }
}
