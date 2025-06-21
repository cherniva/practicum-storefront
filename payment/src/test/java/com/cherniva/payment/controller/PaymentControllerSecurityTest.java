package com.cherniva.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8082/realms/master"
})
class PaymentControllerSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void shouldReturn200WhenValidTokenProvided() {
        // Create a mock JWT
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("sub", "user123")
                .claim("iss", "http://localhost:8082/realms/master")
                .claim("roles", List.of("USER"))
                .build();

        // Mock the JWT decoder
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer valid-token")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class);
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() {
        webTestClient.get()
                .uri("/api/balance")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenInvalidTokenProvided() {
        // Use JwtValidationException instead of JwtException to get proper 401 response
        when(jwtDecoder.decode(anyString()))
                .thenReturn(Mono.error(new JwtValidationException("Invalid token",
                        List.of(new OAuth2Error("invalid_token", "Token is invalid", null)))));

        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer invalid-token")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenMalformedTokenProvided() {
        // Test malformed token
        when(jwtDecoder.decode(anyString()))
                .thenReturn(Mono.error(new JwtValidationException("Token validation failed",
                        List.of(new OAuth2Error("invalid_token", "Token is malformed", null)))));

        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer malformed-token")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenExpiredTokenProvided() {
        // Test with expired token
        when(jwtDecoder.decode(anyString()))
                .thenReturn(Mono.error(new JwtValidationException("Token expired",
                        List.of(new OAuth2Error("invalid_token", "Token has expired", null)))));

        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer expired-token")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn500WhenAuthenticationServiceFails() {
        // JwtException gets wrapped in AuthenticationServiceException -> 500
        when(jwtDecoder.decode(anyString()))
                .thenReturn(Mono.error(new JwtException("Service unavailable")));

        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer service-error-token")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void shouldReturn401WithEmptyBearerToken() {
        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer ")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

