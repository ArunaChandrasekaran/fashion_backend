package com.fashion.repository;

import com.fashion.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    void deleteByCartUserId(Long userId);
}
