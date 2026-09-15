package com.orderflow.auth.shared.controller;


import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.dto.*;
import com.orderflow.auth.shared.service.AuthService;
import com.orderflow.auth.shared.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }
    @PostMapping("/verifycode")
    public ResponseEntity<AuthResponseDto> verify(
            @RequestBody RegisterRequestEmailDto request
    ) {
        return ResponseEntity.ok(authService.verify(request));
    }

}



