package com.orderflow.auth.shared.controller;


import com.orderflow.auth.shared.dto.AuthResponseDto;
import com.orderflow.auth.shared.dto.LoginRequestDto;
import com.orderflow.auth.shared.dto.RegisterRequestDto;
import com.orderflow.auth.shared.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponseDto register(@RequestBody RegisterRequestDto request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto request) {
        return authService.login(request);
    }
}
