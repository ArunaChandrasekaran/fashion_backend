package com.fashion.service;

import com.fashion.dto.AuthDtos.ChangePasswordRequest;
import com.fashion.dto.AuthDtos.LoginRequest;
import com.fashion.dto.AuthDtos.RegisterRequest;
import com.fashion.dto.AuthDtos.UserSummary;
import com.fashion.entity.AppUser;
import com.fashion.entity.Role;
import com.fashion.entity.RoleName;
import com.fashion.exception.ApiException;
import com.fashion.repository.RoleRepository;
import com.fashion.repository.UserRepository;
import com.fashion.security.FashionUserDetails;
import com.fashion.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final DtoMapper mapper;
    private final SecurityUtils securityUtils;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       DtoMapper mapper,
                       SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.mapper = mapper;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public UserSummary register(RegisterRequest request) {
        validatePasswordPair(request.password(), request.confirmPassword());
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> ApiException.badRequest("USER role is not configured"));
        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.getRoles().add(userRole);
        return mapper.toUserSummary(userRepository.save(user));
    }

    @Transactional
    public UserSummary login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        httpRequest.getSession(true);
        httpRequest.changeSessionId();
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        FashionUserDetails details = (FashionUserDetails) authentication.getPrincipal();
        AppUser user = userRepository.findById(details.getId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return mapper.toUserSummary(user);
    }

    @Transactional(readOnly = true)
    public UserSummary me() {
        return mapper.toUserSummary(securityUtils.requireUser());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        validatePasswordPair(request.newPassword(), request.confirmPassword());
        AppUser user = securityUtils.requireUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.validation("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private void validatePasswordPair(String password, String confirm) {
        if (!password.equals(confirm)) {
            throw ApiException.validation("Password confirmation does not match");
        }
        if (!password.matches("(?=.*[A-Za-z])(?=.*\\d).{8,}")) {
            throw ApiException.validation("Password must include at least one letter and one number");
        }
    }
}
