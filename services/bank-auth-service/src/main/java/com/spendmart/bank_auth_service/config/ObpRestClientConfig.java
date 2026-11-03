package com.spendmart.bank_auth_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ObpRestClientConfig {

    @Value("${obp.api.base-url}")
    private String obpBaseUrl;

    @Value("${bank-api.base-url}")
    private String bankApiBaseUrl;

    @Bean
    public RestClient obpAuthRestClient(RestClient.Builder builder) {
        return builder.baseUrl(obpBaseUrl).build();
    }

    @Bean
    public RestClient bankApiRestClient(RestClient.Builder builder) {
        return builder.baseUrl(bankApiBaseUrl).build();
    }
}
