package com.orderflow.auth.shared.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.auth.shared.dto.LoginRequestDto;
import com.orderflow.auth.shared.dto.RegisterRequestDto;
import com.orderflow.auth.shared.dto.RegisterRequestEmailDto;
import com.orderflow.auth.shared.dto.RegisterResponseDto;
import com.orderflow.auth.shared.exception.ApiExceptionHandler;
import com.orderflow.auth.shared.exception.EmailAlreadyExistsException;
import com.orderflow.auth.shared.exception.InvalidCredentialsException;
import com.orderflow.auth.shared.exception.InvalidRequestException;
import com.orderflow.auth.shared.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    void shouldRegisterUserWithValidPayload() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");
        RegisterResponseDto response = new RegisterResponseDto("verification-token");

        when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("verification-token"));
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidPayload = "{\"email\":\"\",\"password\":\"\",\"username\":\"\"}";

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto("john@example.com", "wrong-pass");

        when(authService.login(any(LoginRequestDto.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already registered"));

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void shouldReturnBadRequestForVerificationPayloadErrors() throws Exception {
        RegisterRequestEmailDto request = new RegisterRequestEmailDto("", "123");

        mockMvc.perform(post("/verifycode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnBadRequestWhenServiceRejectsRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("john@example.com", "secret123", "john");

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new InvalidRequestException("Email, username and password are required"));

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email, username and password are required"));
    }
}
