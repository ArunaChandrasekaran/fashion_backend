package com.fashion.controller;

import com.fashion.dto.AuthDtos.MessageResponse;
import com.fashion.dto.CartDtos.AddCartItemRequest;
import com.fashion.dto.CartDtos.CartResponse;
import com.fashion.dto.CartDtos.UpdateCartItemRequest;
import com.fashion.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    public CartResponse add(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse update(@PathVariable Long itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse remove(@PathVariable Long itemId) {
        return cartService.removeItem(itemId);
    }

    @DeleteMapping
    public MessageResponse clear() {
        cartService.clear();
        return new MessageResponse("Bag cleared");
    }
}
