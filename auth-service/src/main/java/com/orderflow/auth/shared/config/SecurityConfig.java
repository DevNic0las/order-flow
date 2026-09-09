package com.orderflow.auth.shared.config;

import com.orderflow.authsecurity.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig{
  private final UserDetailsService userDetailsService;
  @Bean
  public SecurityFilterChain filterChain(
          HttpSecurity http,
          JwtAuthFilter jwtAuthFilter
  ) throws Exception {

    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            // allow both with and without servlet context-path
                            "/login",
                            "/register",
                            "/auth/login",
                            "/auth/register"
                    ).permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            )
            .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  @Bean
  public AuthenticationManager authenticationManager(
          org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config
  ) throws Exception {
    return config.getAuthenticationManager();
  }
}
