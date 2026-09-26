package com.orderflow.authsecurity;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.util.List;

@Service
public class JwtTokenValidator {
  @Value("${jwt.secret}")
  private String secret;

  private SecretKey key() {
    return Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String extractUsername(String token) {
    return Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean isTokenValid(String token) {
    try {
      Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public List<String> extractRoles(String token) {
    Claims claims = Jwts.parser().verifyWith(key()).build()
            .parseSignedClaims(token).getPayload();
    List<String> roles = claims.get("roles", List.class);
    return roles != null ? roles : List.of();
  }
}
