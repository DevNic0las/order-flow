package com.orderflow.notification.config;

import com.orderflow.notification.infrastructure.BrevoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient brevoWebClient(WebClient.Builder builder, BrevoProperties brevoProperties) {
    return builder.baseUrl(brevoProperties.baseUrl()).build();
  }
}