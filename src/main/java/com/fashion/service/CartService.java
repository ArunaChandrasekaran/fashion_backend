package com.fashion.service;

import com.fashion.dto.CartDtos.AddCartItemRequest;
import com.fashion.dto.CartDtos.CartItemResponse;
import com.fashion.dto.CartDtos.CartResponse;
import com.fashion.dto.CartDtos.CartTotals;
import com.fashion.dto.CartDtos.UpdateCartItemRequest;
import com.fashion.entity.AppUser;
import com.fashion.entity.Cart;
import com.fashion.entity.CartItem;
import com.fashion.entity.Product;
import com.fashion.entity.ProductStatus;
import com.fashion.entity.ProductVariant;
import com.fashion.exception.ApiException;
import com.fashion.repository.CartItemRepository;
import com.fashion.repository.CartRepository;
import com.fashion.repository.ProductVariantRepository;
import com.fashion.security.SecurityUtils;
import com.fashion.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final SecurityUtils securityUtils;
    private final DtoMapper mapper;
    private final PricingService pricingService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductVariantRepository variantRepository,
                       SecurityUtils securityUtils,
                       DtoMapper mapper,
                       PricingService pricingService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.securityUtils = securityUtils;
        this.mapper = mapper;
        this.pricingService = pricingService;
    }

    @Transactional
    public CartResponse getCart() {
        Cart cart = requireCart(false);
        return revalidate(cart);
    }

    @Transactional
    public CartResponse addItem(AddCartItemRequest request) {
        Cart cart = requireCart(true);
        ProductVariant variant = variantRepository.findById(request.variantId())
                .orElseThrow(() -> ApiException.notFound("Selected size and colour are not available"));
        assertPurchasable(variant);
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(variant.getId()))
                .findFirst()
                .orElse(null);
        int nextQty = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        if (nextQty > variant.getStockQuantity()) {
            throw ApiException.validation("Only " + variant.getStockQuantity() + " piece(s) are available");
        }
        if (existing == null) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setVariant(variant);
            item.setQuantity(request.quantity());
            cart.getItems().add(item);
        } else {
            existing.setQuantity(nextQty);
        }
        cart.touch();
        cartRepository.save(cart);
        return revalidate(cart);
    }

    @Transactional
    public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
        AppUser user = securityUtils.requireUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Cart item not found"));
        ProductVariant variant = item.getVariant();
        if (request.quantity() > variant.getStockQuantity()) {
            throw ApiException.validation("Only " + variant.getStockQuantity() + " piece(s) are available");
        }
        item.setQuantity(request.quantity());
        item.getCart().touch();
        return revalidate(item.getCart());
    }

    @Transactional
    public CartResponse removeItem(Long itemId) {
        AppUser user = securityUtils.requireUser();
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Cart item not found"));
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        cart.touch();
        return revalidate(cart);
    }

    @Transactional
    public CartResponse clear() {
        Cart cart = requireCart(false);
        cart.getItems().clear();
        cart.touch();
        return revalidate(cart);
    }

    @Transactional
    public Cart requireCart(boolean create) {
        AppUser user = securityUtils.requireUser();
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            if (!create) {
                Cart empty = new Cart();
                empty.setUser(user);
                return cartRepository.save(empty);
            }
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    public CartResponse revalidate(Cart cart) {
        cart.getItems().size();
        List<String> notices = new ArrayList<>();
        Iterator<CartItem> iterator = cart.getItems().iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();
            product.getImages().size();
            if (!variant.isActive() || product.getStatus() != ProductStatus.ACTIVE) {
                iterator.remove();
                notices.add(product.getName() + " is no longer available and was removed from your bag.");
                continue;
            }
            if (variant.getStockQuantity() <= 0) {
                iterator.remove();
                notices.add(product.getName() + " is out of stock and was removed from your bag.");
                continue;
            }
            if (item.getQuantity() > variant.getStockQuantity()) {
                item.setQuantity(variant.getStockQuantity());
                notices.add(product.getName() + " quantity was reduced to available stock.");
            }
        }
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> mapper.toCartItem(item, null))
                .toList();
        BigDecimal subtotal = Money.ZERO;
        BigDecimal discount = Money.ZERO;
        for (CartItem item : cart.getItems()) {
            Product product = item.getVariant().getProduct();
            subtotal = subtotal.add(Money.scale(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
            discount = discount.add(Money.discountAmount(product.getPrice(), product.getDiscountPercent(), item.getQuantity()));
        }
        CartTotals totals = pricingService.totals(subtotal, discount, items.isEmpty());
        return new CartResponse(cart.getId(), items, totals, notices);
    }

    private void assertPurchasable(ProductVariant variant) {
        Product product = variant.getProduct();
        if (!variant.isActive() || product.getStatus() != ProductStatus.ACTIVE) {
            throw ApiException.validation("This dress is not available for purchase");
        }
        if (variant.getStockQuantity() <= 0) {
            throw ApiException.validation("The selected size and colour are out of stock");
        }
    }
}
