package com.orderflow.auth.shared.service;

import com.orderflow.auth.shared.domain.Role;
import com.orderflow.auth.shared.domain.User;
import com.orderflow.auth.shared.dto.AuthResponseDto;
import com.orderflow.auth.shared.dto.LoginRequestDto;
import com.orderflow.auth.shared.dto.RegisterRequestDto;
import com.orderflow.auth.shared.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponseDto register(RegisterRequestDto request) {
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();
        userRepository.save(user);
        return new AuthResponseDto(jwtService.generateToken(user));
    }

    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow();
        return new AuthResponseDto(jwtService.generateToken(user));
    }
}
