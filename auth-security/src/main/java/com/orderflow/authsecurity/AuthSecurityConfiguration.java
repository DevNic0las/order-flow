package com.orderflow.authsecurity;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthSecurityConfiguration {
  @Bean
  public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(
          JwtAuthFilter filter) {

    FilterRegistrationBean<JwtAuthFilter> registration =
            new FilterRegistrationBean<>(filter);

    registration.setEnabled(false);

    return registration;
  }
}
