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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación", description = "Registro, inicio de sesión y gestión de credenciales")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Registrar un nuevo usuario",
            description = "Crea el usuario (plan FREE, cuenta inactiva) y devuelve el token de activación.")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @Valid @RequestBody RegisterRequest request) {
        String activationToken = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                Map.of("activationToken", activationToken),
                "Usuario registrado. Active la cuenta para iniciar sesión."));
    }

    @Operation(summary = "Iniciar sesión",
            description = "Devuelve access y refresh token. Protegido contra fuerza bruta (lockout + rate limit).")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "Renovar tokens",
            description = "Rota el par de tokens; revoca el refresh token anterior.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @Operation(summary = "Cerrar sesión",
            description = "Revoca los tokens vigentes del usuario. Requiere autenticación.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Sesión cerrada"));
    }

    @Operation(summary = "Activar cuenta", description = "Activa la cuenta con el token de activación.")
    @SecurityRequirements
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Object>> activate(
            @Valid @RequestBody ActivateRequest request) {
        authService.activate(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Cuenta activada"));
    }

    @Operation(summary = "Solicitar recuperación de contraseña",
            description = "Responde de forma uniforme; no revela si el correo existe.")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null,
                "Si el correo existe, se enviarán instrucciones de recuperación."));
    }

    @Operation(summary = "Restablecer contraseña",
            description = "Cambia la contraseña usando el token (un solo uso) recibido por correo.")
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contraseña restablecida"));
    }
}
