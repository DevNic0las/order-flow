package com.orderflow.web.service;

import com.orderflow.web.dto.AuthResponseDto;
import com.orderflow.web.dto.LoginRequestDto;
import com.orderflow.web.dto.OrderViewDto;
import com.orderflow.web.dto.ProductViewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayClient {

    private final RestClient gatewayRestClient;

    public String login(String email, String password) {
        try {
            AuthResponseDto response = gatewayRestClient.post()
                    .uri("/auth/login")
                    .body(new LoginRequestDto(email, password))
                    .retrieve()
                    .body(AuthResponseDto.class);

            return response != null ? response.token() : null;
        } catch (RestClientException ex) {
            log.warn("Login via gateway failed: {}", ex.getMessage());
            return null;
        }
    }

    public List<OrderViewDto> getOrders(String jwt) {
        return gatewayRestClient.get()
                .uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .retrieve()
                .body(List.class);
    }

    public List<ProductViewDto> getProducts(String jwt) {
        return gatewayRestClient.get()
                .uri("/inventory/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .retrieve()
                .body(List.class);
    }
}
