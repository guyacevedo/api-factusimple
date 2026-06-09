package com.factusimple.auth.controller;

import com.factusimple.auth.dto.AuthDtos.ActivateRequest;
import com.factusimple.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.factusimple.auth.dto.AuthDtos.LoginRequest;
import com.factusimple.auth.dto.AuthDtos.RefreshRequest;
import com.factusimple.auth.dto.AuthDtos.RegisterRequest;
import com.factusimple.auth.dto.AuthDtos.ResetPasswordRequest;
import com.factusimple.auth.dto.AuthDtos.TokenResponse;
import com.factusimple.auth.service.AuthService;
import com.factusimple.infrastructure.exception.ApiResponse;
import com.factusimple.infrastructure.security.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @Valid @RequestBody RegisterRequest request) {
        String activationToken = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                Map.of("activationToken", activationToken),
                "Usuario registrado. Active la cuenta para iniciar sesión."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada"));
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Object>> activate(
            @Valid @RequestBody ActivateRequest request) {
        authService.activate(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cuenta activada"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null,
                "Si el correo existe, se enviarán instrucciones de recuperación."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contraseña restablecida"));
    }
}
