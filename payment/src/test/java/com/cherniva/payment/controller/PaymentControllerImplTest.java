package com.cherniva.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

@WebFluxTest(PaymentControllerImpl.class)
class PaymentControllerImplTest {

    private WebTestClient webTestClient;
    private PaymentControllerImpl paymentController;
    private double balance = 10000.0;
    private static final String VALID_USER_ID = "123";

    @BeforeEach
    void setUp() {
        paymentController = new PaymentControllerImpl(new BigDecimal(balance));
        webTestClient = WebTestClient.bindToController(paymentController).build();
    }

    // X-User-ID Header Tests (existing functionality)
    @Test
    void getBalance_WithValidUserId_ShouldReturnInitialBalance() {
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(balance);
    }

    @Test
    void processPayment_WithValidAmountAndUserId_ShouldSucceed() {
        double amount = 100.0;
        double expBalance = balance - amount;
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance);

        // Verify balance was updated
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance);
    }

    @Test
    void processPayment_WithNegativeAmountAndUserId_ShouldFail() {
        webTestClient.post()
                .uri("/payment?amount=-100.0")
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void processPayment_WithZeroAmountAndUserId_ShouldFail() {
        webTestClient.post()
                .uri("/payment?amount=0.0")
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void processPayment_WithInsufficientBalanceAndUserId_ShouldFail() {
        webTestClient.post()
                .uri("/payment?amount=20000.0")
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void processPayment_WithMultiplePaymentsAndUserId_ShouldMaintainCorrectBalance() {
        double amount = 100.0;
        double expBalance1 = balance - amount;
        double expBalance2 = balance - 2*amount;
        // First payment
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance1);

        // Second payment
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance2);

        // Verify final balance
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", VALID_USER_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance2);
    }

    @Test
    void processPayment_WithMultipleUsers_ShouldMaintainSeparateBalances() {
        String user1 = "123";
        String user2 = "456";
        double amount = 100.0;
        
        // User 1 makes a payment
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .header("X-User-ID", user1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(balance - amount);

        // User 2 should still have initial balance
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", user2)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(balance);
    }

    // X-User-ID Header Validation Tests
    @Test
    void getBalance_WithoutUserId_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/balance")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_WithEmptyUserId_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", "")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_WithWhitespaceUserId_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", "   ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_WithInvalidUserIdFormat_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/balance")
                .header("X-User-ID", "invalid-user-id")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void processPayment_WithoutUserId_ShouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/payment?amount=100.0")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void processPayment_WithEmptyUserId_ShouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/payment?amount=100.0")
                .header("X-User-ID", "")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void processPayment_WithWhitespaceUserId_ShouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/payment?amount=100.0")
                .header("X-User-ID", "   ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void processPayment_WithInvalidUserIdFormat_ShouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/payment?amount=100.0")
                .header("X-User-ID", "invalid-user-id")
                .exchange()
                .expectStatus().isUnauthorized();
    }
} 