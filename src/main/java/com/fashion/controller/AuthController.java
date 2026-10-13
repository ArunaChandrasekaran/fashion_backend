package com.fashion.controller;

import com.fashion.dto.AuthDtos.ChangePasswordRequest;
import com.fashion.dto.AuthDtos.CsrfResponse;
import com.fashion.dto.AuthDtos.LoginRequest;
import com.fashion.dto.AuthDtos.MessageResponse;
import com.fashion.dto.AuthDtos.RegisterRequest;
import com.fashion.dto.AuthDtos.UserSummary;
import com.fashion.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserSummary login(@Valid @RequestBody LoginRequest request,
                             HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @GetMapping("/me")
    public UserSummary me() {
        return authService.me();
    }

    @PatchMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return new MessageResponse("Password updated");
    }
}
