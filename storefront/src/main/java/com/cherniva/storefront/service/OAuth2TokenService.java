package com.cherniva.storefront.service;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OAuth2TokenService {
    
    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    
    public OAuth2TokenService(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }
    
    public Mono<String> getAccessToken() {
        return authorizedClientManager.authorize(
                OAuth2AuthorizeRequest
                        .withClientRegistrationId("payment-service")
                        .principal("system")
                        .build()
                )
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue);
    }
} 