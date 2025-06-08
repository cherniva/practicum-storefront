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

    @BeforeEach
    void setUp() {
        paymentController = new PaymentControllerImpl(new BigDecimal(balance));
        webTestClient = WebTestClient.bindToController(paymentController).build();
    }

    @Test
    void getBalance_ShouldReturnInitialBalance() {
        webTestClient.get()
                .uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(balance);
    }

    @Test
    void processPayment_WithValidAmount_ShouldSucceed() {
        double amount = 100.0;
        double expBalance = balance - amount;
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance);

        // Verify balance was updated
        webTestClient.get()
                .uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance);
    }

    @Test
    void processPayment_WithNegativeAmount_ShouldFail() {
        webTestClient.post()
                .uri("/payment?amount=-100.0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void processPayment_WithZeroAmount_ShouldFail() {
        webTestClient.post()
                .uri("/payment?amount=0.0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void processPayment_WithInsufficientBalance_ShouldFail() {
        webTestClient.post()
                .uri("/payment?amount=20000.0")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void processPayment_WithMultiplePayments_ShouldMaintainCorrectBalance() {
        double amount = 100.0;
        double expBalance1 = balance - amount;
        double expBalance2 = balance - 2*amount;
        // First payment
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance1);

        // Second payment
        webTestClient.post()
                .uri("/payment?amount=%.1f".formatted(amount))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance2);

        // Verify final balance
        webTestClient.get()
                .uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Double.class)
                .isEqualTo(expBalance2);
    }
} 