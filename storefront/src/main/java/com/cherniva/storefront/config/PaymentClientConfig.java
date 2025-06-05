package com.cherniva.storefront.config;

import com.cherniva.storefront.client.ApiClient;
import com.cherniva.storefront.client.api.PaymentApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentClientConfig {

    @Value("${payment.service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient apiClient() {
        ApiClient apiClient = new ApiClient(WebClient.create());
        apiClient.setBasePath(paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient apiClient) {
        return new PaymentApi(apiClient);
    }
} 