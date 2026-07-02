package ua.homesafe.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ua.homesafe.dto.ApiResponse;
import ua.homesafe.dto.AuthDtos;
import ua.homesafe.dto.UserDto;
import ua.homesafe.model.UserEntity;
import ua.homesafe.service.SessionAuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SessionAuthService sessionAuthService;

    public AuthController(SessionAuthService sessionAuthService) {
        this.sessionAuthService = sessionAuthService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        sessionAuthService.register(request);
        return new ApiResponse<>(null, "Реєстрацію створено. Очікуйте схвалення адміністратора.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthPayload> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.of(sessionAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me(Authentication authentication) {
        return ApiResponse.of(UserDto.from((UserEntity) authentication.getPrincipal()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        sessionAuthService.logout(authorization.substring(7));
        return ApiResponse.of(null);
    }
}
