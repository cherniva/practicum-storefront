package com.cherniva.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8082/realms/master"
})
class PaymentControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void shouldAuthenticateWithValidToken() {
        // Create JWT with realm_access roles (Keycloak format)
        Map<String, Object> realmAccess = Map.of("roles", List.of("USER", "ADMIN"));

        Jwt jwt = Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .claim("sub", "test-user")
                .claim("iss", "http://localhost:8082/realms/master")
                .claim("realm_access", realmAccess)
                .claim("aud", "payment-test")
                .build();

        when(jwtDecoder.decode("valid-token")).thenReturn(Mono.just(jwt));

        webTestClient.get()
                .uri("/api/balance")
                .header("Authorization", "Bearer valid-token")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .consumeWith(response -> {
                    assertThat(response.getResponseBody()).isNotNull();
                });
    }

    @Test
    void shouldReturn401WithoutToken() {
        webTestClient.get()
                .uri("/api/balance")
                .header("X-User-ID", "123")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
