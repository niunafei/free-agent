package com.webmodel.agent.controller;

import com.webmodel.agent.application.AuthService;
import com.webmodel.agent.common.Result;
import com.webmodel.agent.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return Result.success(authService.register(req.username(), req.password(), req.displayName()));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req.username(), req.password()));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(authService.me(UserContext.getUserId()));
    }

    public record RegisterRequest(String username, String password, String displayName) {}
    public record LoginRequest(String username, String password) {}
}