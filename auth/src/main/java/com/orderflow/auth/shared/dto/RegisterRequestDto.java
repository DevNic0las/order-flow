package com.orderflow.auth.shared.dto;

import com.orderflow.auth.shared.domain.Role;

public record RegisterRequestDto(

        String email,
        String password,
        String userName


) {
}
