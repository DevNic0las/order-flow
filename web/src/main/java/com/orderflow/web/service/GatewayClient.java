package com.orderflow.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.orderflow.web.dto.AuthResponseDto;
import com.orderflow.web.dto.LoginRequestDto;
import com.orderflow.web.dto.OrderViewDto;
import com.orderflow.web.dto.ProductViewDto;
import com.orderflow.web.dto.RegisterRequestDto;
import com.orderflow.web.dto.ResendCodeRequestDto;
import com.orderflow.web.dto.VerifyCodeRequestDto;
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
                    .body(new LoginRequestDto(email, password))
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

    /**
     * Registra a conta.
     *
     * @return o token de verificação em caso de sucesso (201/200),
     *         null em caso de rejeição de dados (400/409 — email/username duplicado, validação),
     *         ou lança GatewayIntegrationException para qualquer outra falha.
     */
    public String register(String name, String email, String password) {
        try {
            RegisterResponse response = gatewayRestClient.post()
                    .uri("/auth/register")
                    .body(new RegisterRequestDto(email, password, name))
                    .retrieve()
                    .body(RegisterResponse.class);

            if (response == null || response.token() == null || response.token().isBlank()) {
                log.error("Register via gateway returned empty token body");
                throw new GatewayIntegrationException("Resposta vazia do serviço de autenticação");
            }
            return response.token();
        } catch (HttpClientErrorException ex) {
            log.warn("Register via gateway rejected: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (RestClientException ex) {
            log.error("Register via gateway failed: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao conectar com o serviço de autenticação", ex);
        }
    }

    /**
     * Confirma a conta com token + código.
     *
     * @return o JWT em sucesso (via AuthResponseDto), null em dados inválidos (400/409),
     *         ou lança GatewayIntegrationException para outras falhas.
     */
    public String verifyCode(String token, String code) {
        try {
            AuthResponseDto response = gatewayRestClient.post()
                    .uri("/auth/verifycode")
                    .body(new VerifyCodeRequestDto(token, code))
                    .retrieve()
                    .body(AuthResponseDto.class);

            if (response == null || response.token() == null || response.token().isBlank()) {
                log.error("Verifycode via gateway returned empty token body");
                throw new GatewayIntegrationException("Resposta vazia do serviço de autenticação");
            }
            return response.token();
        } catch (HttpClientErrorException ex) {
            log.warn("Verifycode via gateway rejected: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (RestClientException ex) {
            log.error("Verifycode via gateway failed: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao conectar com o serviço de autenticação", ex);
        }
    }

    /**
     * Reenvia o código de verificação.
     *
     * @return ResendOutcome com flag de cooldown; ResendOutcome.cooldownSeconds preenchido
     *         quando o auth-service respondeu 429 (cooldown ativo).
     */
    public ResendOutcome resendCode(String token) {
        try {
            gatewayRestClient.post()
                    .uri("/auth/resend-code")
                    .body(new ResendCodeRequestDto(token))
                    .retrieve()
                    .toBodilessEntity();
            return new ResendOutcome(true, null);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            Long retryAfter = extractRetryAfter(ex);
            log.info("Resend-code via gateway: cooldown ativo ({}s)", retryAfter);
            return new ResendOutcome(false, retryAfter);
        } catch (HttpClientErrorException ex) {
            log.warn("Resend-code via gateway rejected: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return new ResendOutcome(false, null);
        } catch (RestClientException ex) {
            log.error("Resend-code via gateway failed: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao conectar com o serviço de autenticação", ex);
        }
    }

    private Long extractRetryAfter(HttpClientErrorException.TooManyRequests ex) {
        String header = ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().getFirst("Retry-After") : null;
        if (header != null) {
            try {
                return Long.parseLong(header.trim());
            } catch (NumberFormatException ignored) {
                // cai no parse do body abaixo
            }
        }
        try {
            JsonNode body = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(ex.getResponseBodyAsString());
            if (body.hasNonNull("retryAfterSeconds")) {
                return body.get("retryAfterSeconds").asLong();
            }
        } catch (Exception ignored) {
            // sem segundos parseáveis
        }
        return null;
    }

    /**
     * Cria um pedido. O customerName é preenchido pelo backend (usuário do JWT).
     *
     * @return OrderCreated com o id do pedido em caso de sucesso (2xx),
     *         null em rejeição de dados (400/403 — validação, estoque insuficiente, role),
     *         ou lança GatewayIntegrationException para outras falhas.
     */
    public OrderCreated createOrder(String jwt, Long productId, Integer quantity) {
        try {
            OrderResponse response = gatewayRestClient.post()
                    .uri("/orders/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .body(new CreateOrderRequest(productId, quantity))
                    .retrieve()
                    .body(OrderResponse.class);

            if (response == null || response.id() == null) {
                log.error("POST /orders via gateway returned empty body");
                throw new GatewayIntegrationException("Resposta vazia do serviço de pedidos");
            }
            return new OrderCreated(response.id(), response.status());
        } catch (HttpClientErrorException ex) {
            log.warn("POST /orders via gateway rejected: status={} body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (RestClientException ex) {
            log.error("POST /orders via gateway failed: {}", ex.getMessage());
            throw new GatewayIntegrationException("Erro ao conectar com o serviço de pedidos", ex);
        }
    }

    public record OrderCreated(Long orderId, String status) {
    }

    public record OrderResponse(Long id, String customerName, Long productId,
                                Integer quantity, String status) {
    }

    public record CreateOrderRequest(Long productId, Integer quantity) {
    }

    public List<OrderViewDto> getOrders(String jwt) {
        try {
            return gatewayRestClient.get()
                    .uri("/orders/orders")
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

    public record ResendOutcome(boolean resent, Long cooldownSeconds) {
    }

    public record RegisterResponse(String token) {
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
