package com.orderflow.auth.shared.controller;

import com.orderflow.auth.shared.dto.AuthResponseDto;
import com.orderflow.auth.shared.dto.LoginRequestDto;
import com.orderflow.auth.shared.dto.RegisterRequestDto;
import com.orderflow.auth.shared.dto.RegisterRequestEmailDto;
import com.orderflow.auth.shared.dto.RegisterResponseDto;
import com.orderflow.auth.shared.dto.ResendCodeRequestDto;
import com.orderflow.auth.shared.dto.ResendCodeResponseDto;
import com.orderflow.auth.shared.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verifycode")
    public ResponseEntity<AuthResponseDto> verify(@Valid @RequestBody RegisterRequestEmailDto request) {
        return ResponseEntity.ok(authService.verify(request));
    }


    @PostMapping("/resend-code")
    public ResponseEntity<ResendCodeResponseDto> resendCode(@Valid @RequestBody ResendCodeRequestDto request) {
        return ResponseEntity.ok(authService.resendCode(request));
    }



}
