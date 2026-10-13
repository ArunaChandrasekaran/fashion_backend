package com.fashion.service;

import com.fashion.dto.AuthDtos.UserSummary;
import com.fashion.dto.CartDtos.CartItemResponse;
import com.fashion.dto.OrderDtos.OrderDetailResponse;
import com.fashion.dto.OrderDtos.OrderItemResponse;
import com.fashion.dto.OrderDtos.OrderSummaryResponse;
import com.fashion.dto.OrderDtos.PaymentResponse;
import com.fashion.dto.OrderDtos.ShippingSnapshot;
import com.fashion.dto.OrderDtos.StatusHistoryResponse;
import com.fashion.dto.ProductDtos.CategoryResponse;
import com.fashion.dto.ProductDtos.ImageResponse;
import com.fashion.dto.ProductDtos.ProductCardResponse;
import com.fashion.dto.ProductDtos.ProductDetailResponse;
import com.fashion.dto.ProductDtos.VariantResponse;
import com.fashion.dto.ProfileDtos.AddressResponse;
import com.fashion.entity.Address;
import com.fashion.entity.AppUser;
import com.fashion.entity.CartItem;
import com.fashion.entity.Category;
import com.fashion.entity.Order;
import com.fashion.entity.OrderItem;
import com.fashion.entity.Product;
import com.fashion.entity.ProductImage;
import com.fashion.entity.ProductStatus;
import com.fashion.entity.ProductVariant;
import com.fashion.entity.Role;
import com.fashion.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DtoMapper {

    public UserSummary toUserSummary(AppUser user) {
        return new UserSummary(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }

    public CategoryResponse toCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription());
    }

    public ProductCardResponse toCard(Product product) {
        List<ProductVariant> active = product.getVariants().stream().filter(ProductVariant::isActive).toList();
        return new ProductCardResponse(
                product.getId(),
                product.getName(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                Money.scale(product.getPrice()),
                Money.scale(product.getDiscountPercent()),
                salePrice(product),
                primaryImage(product),
                active.stream().map(ProductVariant::getSize).distinct().toList(),
                active.stream().map(ProductVariant::getColor).distinct().toList(),
                active.stream().anyMatch(v -> v.getStockQuantity() > 0)
        );
    }

    public ProductDetailResponse toDetail(Product product) {
        List<VariantResponse> variants = product.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getSize).thenComparing(ProductVariant::getColor))
                .map(this::toVariant)
                .toList();
        List<ImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(img -> new ImageResponse(img.getId(), img.getUrl(), img.getAltText(), img.getSortOrder(), img.isPrimary()))
                .toList();
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                toCategory(product.getCategory()),
                Money.scale(product.getPrice()),
                Money.scale(product.getDiscountPercent()),
                salePrice(product),
                product.getSku(),
                product.getStatus(),
                product.getCreatedAt(),
                images,
                variants
        );
    }

    public VariantResponse toVariant(ProductVariant variant) {
        return new VariantResponse(variant.getId(), variant.getSize(), variant.getColor(), variant.getSku(),
                variant.getStockQuantity(), variant.isActive());
    }

    public CartItemResponse toCartItem(CartItem item, String warning) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        boolean available = variant.isActive()
                && product.getStatus() == ProductStatus.ACTIVE
                && variant.getStockQuantity() >= item.getQuantity();
        BigDecimal line = Money.lineTotal(product.getPrice(), product.getDiscountPercent(), item.getQuantity());
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                variant.getId(),
                product.getName(),
                primaryImage(product),
                variant.getSize(),
                variant.getColor(),
                variant.getSku(),
                Money.scale(product.getPrice()),
                Money.scale(product.getDiscountPercent()),
                line,
                item.getQuantity(),
                variant.getStockQuantity(),
                available,
                warning
        );
    }

    public AddressResponse toAddress(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress()
        );
    }

    public OrderSummaryResponse toOrderSummary(Order order, boolean cancellable) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                Money.scale(order.getFinalTotal()),
                order.getCreatedAt(),
                cancellable
        );
    }

    public OrderDetailResponse toOrderDetail(Order order, boolean cancellable) {
        PaymentResponse payment = order.getPayment() == null ? null : new PaymentResponse(
                order.getPayment().getMethod(),
                order.getPayment().getStatus(),
                Money.scale(order.getPayment().getAmount()),
                order.getPayment().getProvider()
        );
        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                payment,
                new ShippingSnapshot(
                        order.getShipFullName(),
                        order.getShipPhone(),
                        order.getShipLine1(),
                        order.getShipLine2(),
                        order.getShipCity(),
                        order.getShipState(),
                        order.getShipPostalCode(),
                        order.getShipCountry()
                ),
                order.getItems().stream().map(this::toOrderItem).toList(),
                order.getStatusHistory().stream()
                        .map(h -> new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(), h.getCreatedAt(), h.getNotes()))
                        .toList(),
                Money.scale(order.getSubtotal()),
                Money.scale(order.getDiscountAmount()),
                Money.scale(order.getShippingCharge()),
                Money.scale(order.getFinalTotal()),
                order.getCreatedAt(),
                cancellable,
                order.getUser().getFullName(),
                order.getUser().getEmail()
        );
    }

    public OrderItemResponse toOrderItem(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getSku(),
                item.getSize(),
                item.getColor(),
                item.getImageUrl(),
                Money.scale(item.getUnitPrice()),
                Money.scale(item.getDiscountPercent()),
                item.getQuantity(),
                Money.scale(item.getLineTotal())
        );
    }

    public String primaryImage(Product product) {
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream().map(ProductImage::getUrl).findFirst().orElse(null));
    }

    public BigDecimal salePrice(Product product) {
        return Money.lineTotal(product.getPrice(), product.getDiscountPercent(), 1);
    }
}
