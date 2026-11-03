package com.spendmart.banking_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${obp.api.base-url}")
    private String obpBaseUrl;

    @Value("${bank-auth.service.url}")
    private String bankAuthServiceUrl;

    @Value("${bankapi.base-url}")
    private String bankApiBaseUrl;

    @Bean("obpRestClient")
    public RestClient obpRestClient(RestClient.Builder builder) {
        return builder.baseUrl(obpBaseUrl).build();
    }

    @Bean("bankAuthRestClient")
    public RestClient bankAuthRestClient(RestClient.Builder builder) {
        return builder.baseUrl(bankAuthServiceUrl).build();
    }

    @Bean("bankApiRestClient")
    public RestClient bankApiRestClient(RestClient.Builder builder) {
        return builder.baseUrl(bankApiBaseUrl).build();
    }
}
