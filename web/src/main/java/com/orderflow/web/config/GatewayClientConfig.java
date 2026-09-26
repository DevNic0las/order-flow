package com.orderflow.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GatewayClientConfig {

    @Bean
    public RestClient gatewayRestClient(
            RestClient.Builder builder,
            @Value("${gateway.base-url}") String gatewayBaseUrl
    ) {
        return builder
                .baseUrl(gatewayBaseUrl)
                .build();
    }
}
