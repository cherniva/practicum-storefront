package com.cherniva.storefront.config;

import com.cherniva.storefront.client.ApiClient;
import com.cherniva.storefront.client.api.PaymentApi;
import com.cherniva.storefront.service.OAuth2TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class PaymentClientConfig {

    @Value("${payment.service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public WebClient paymentWebClient(OAuth2TokenService oAuth2TokenService) {
        return WebClient.builder()
                .filter((request, next) -> {
                    log.info("Payment WebClient filter: Processing request to {}", request.url());
                    return oAuth2TokenService.getAccessToken()
                        .doOnNext(token -> log.info("Payment WebClient filter: Got token: {}", token.substring(0, Math.min(20, token.length())) + "..."))
                        .flatMap(token -> {
                            // Create a new request with the Authorization header
                            HttpHeaders newHeaders = new HttpHeaders();
                            newHeaders.putAll(request.headers());
                            newHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                            
                            log.info("Payment WebClient filter: Added Authorization header");
                            
                            // Create a new request with the modified headers
                            ClientRequest newRequest = ClientRequest.from(request)
                                .headers(headers -> headers.putAll(newHeaders))
                                .build();
                            
                            return next.exchange(newRequest);
                        })
                        .doOnError(error -> log.error("Payment WebClient filter: Error getting token or making request", error))
                        .onErrorResume(error -> {
                            log.error("Payment WebClient filter: Resuming with error", error);
                            return next.exchange(request);
                        });
                })
                .build();
    }

    @Bean
    public ApiClient apiClient(WebClient paymentWebClient) {
        ApiClient apiClient = new ApiClient(paymentWebClient);
        apiClient.setBasePath(paymentServiceUrl);
        log.info("Payment ApiClient configured with base path: {}", paymentServiceUrl);
        return apiClient;
    }

    @Bean
    public PaymentApi paymentApi(ApiClient apiClient) {
        return new PaymentApi(apiClient);
    }
} 