package br.edu.uniesp.dearfuture.internal.auth.controller;

import br.edu.uniesp.dearfuture.internal.auth.dto.*;
import br.edu.uniesp.dearfuture.internal.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) { return service.me(UUID.fromString(authentication.getName())); }
}
