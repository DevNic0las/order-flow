package com.orderflow.auth.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 255, message = "Password must be at most 255 characters")
        String password,

        @NotBlank(message = "Username is required")
        @Size(max = 255, message = "Username must be at most 255 characters")
        String username

) {
}
