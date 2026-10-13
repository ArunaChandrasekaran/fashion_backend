package com.fashion.repository;

import com.fashion.entity.AppUser;
import com.fashion.entity.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM AppUser u
            JOIN u.roles r
            WHERE r.name = :role
              AND (:q = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<AppUser> searchCustomers(@Param("role") RoleName role, @Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(u) FROM AppUser u JOIN u.roles r WHERE r.name = com.fashion.entity.RoleName.USER")
    long countCustomers();
}
