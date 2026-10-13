package com.fashion.security;

import com.fashion.entity.AppUser;
import com.fashion.exception.ApiException;
import com.fashion.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof FashionUserDetails details)) {
            throw ApiException.unauthorized("Please sign in to continue");
        }
        return details.getId();
    }

    public AppUser requireUser() {
        return userRepository.findById(requireUserId())
                .orElseThrow(() -> ApiException.unauthorized("Please sign in to continue"));
    }
}
