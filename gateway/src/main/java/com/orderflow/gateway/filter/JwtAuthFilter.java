package com.orderflow.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;


@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {


  @Value("${jwt.secret}")
  private String secret;

  private static final List<String> PUBLIC_PATHS = List.of(
          "/auth/login", "/auth/register", "/auth/verifycode", "/auth/resend-code",
          "/login", "/register", "/verifycode", "/resend-code", "/dashboard", "/catalog", "/buy",
          "/logout"
  );

  private static final List<String> PUBLIC_PATH_FRAGMENTS = List.of(
          "/swagger-ui", "/v3/api-docs", "/css"
  );

  private SecretKey key() {
    return Keys.hmacShaKeyFor(secret.getBytes());
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::equals)
            || PUBLIC_PATH_FRAGMENTS.stream().anyMatch(fragment -> {
      int idx = path.indexOf(fragment);
      if (idx < 0) return false;
      int endIdx = idx + fragment.length();
      return endIdx == path.length() || path.charAt(endIdx) == '/';
    });

    if (isPublic) {
      return chain.filter(exchange);
    }
    String header = exchange.getRequest().getHeaders().getFirst("Authorization");

    if (header == null || !header.startsWith("Bearer ")) {
      return unauthorized(exchange);
    }

    String token = header.substring(7);

    try {
      Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
      return chain.filter(exchange); // token válido, repassa
    } catch (JwtException e) {
      return unauthorized(exchange);
    }
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return -1;
  }

}
