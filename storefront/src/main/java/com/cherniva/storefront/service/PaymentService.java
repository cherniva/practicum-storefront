package com.cherniva.storefront.service;

import com.cherniva.storefront.client.api.PaymentApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentService {
    private final PaymentApi paymentApi;

    public PaymentService(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    public Mono<Double> getBalance() {
        log.info("PaymentService: Calling getBalance()");
        return paymentApi.getBalance()
                .doOnNext(balance -> log.info("PaymentService: Received balance: {}", balance))
                .doOnError(error -> log.error("PaymentService: Error getting balance", error));
    }

    public Mono<Double> processPayment(Double amount) {
        log.info("PaymentService: Calling processPayment with amount: {}", amount);
        return paymentApi.processPayment(amount)
                .doOnNext(newBalance -> log.info("PaymentService: Payment processed, new balance: {}", newBalance))
                .doOnError(error -> log.error("PaymentService: Error processing payment", error));
    }
} 