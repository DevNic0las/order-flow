package com.orderflow.web.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.web.dto.AuthResponseDto;
import com.orderflow.web.dto.LoginRequestDto;
import com.orderflow.web.dto.OrderViewDto;
import com.orderflow.web.dto.ProductViewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayClient {

    private static final ParameterizedTypeReference<List<OrderViewDto>> ORDER_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<List<ProductViewDto>> PRODUCT_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient gatewayRestClient;

    /**
     * @return o JWT em caso de sucesso, null quando as credenciais são inválidas (401),
     *         ou lança GatewayIntegrationException para qualquer outra falha (404, 500, timeout...).
     */
    public String login(String email, String password) {
        try {
            AuthResponseDto response = gatewayRestClient.post()
                    .uri("/auth/login")
                    .body(new com.orderflow.web.dto.LoginRequestDto(email, password))
                    .retrieve()
                    .body(AuthResponseDto.class);

            if (response == null || response.token() == null || response.token().isBlank()) {
                log.error("Login via gateway returned empty token body");
                throw new GatewayIntegrationException("Resposta vazia do serviço de autenticação");
            }
            return response.token();
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.info("Login via gateway rejected: 401 Unauthorized");
            return null;
        } catch (HttpClientErrorException ex) {
            log.error("Login via gateway failed with unexpected client error: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new GatewayIntegrationException("Erro ao conectar com o serviço de autenticação", ex);
        } catch (RestClientException ex) {
            log.error("Login via gateway failed with connection/serialization error: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao conectar com o serviço de autenticação", ex);
        }
    }

    public List<OrderViewDto> getOrders(String jwt) {
        try {
            return gatewayRestClient.get()
                    .uri("/orders/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(ORDER_LIST_TYPE);
        } catch (RestClientException ex) {
            log.error("GET /orders via gateway failed: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao consultar pedidos no gateway", ex);
        }
    }

    public List<ProductViewDto> getProducts(String jwt) {
        try {
            return gatewayRestClient.get()
                    .uri("/inventory/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(PRODUCT_LIST_TYPE);
        } catch (RestClientException ex) {
            log.error("GET /inventory/products via gateway failed: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao consultar estoque no gateway", ex);
        }
    }

    public static class GatewayIntegrationException extends RuntimeException {
        public GatewayIntegrationException(String message) {
            super(message);
        }

        public GatewayIntegrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
