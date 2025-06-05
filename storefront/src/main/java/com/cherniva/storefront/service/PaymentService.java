package com.cherniva.storefront.service;

import com.cherniva.storefront.client.api.PaymentApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class PaymentService {
    private final PaymentApi paymentApi;

    public PaymentService(PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    public Mono<Double> getBalance() {
        return paymentApi.getBalance();
    }

    public Mono<Double> processPayment(Double amount) {
        return paymentApi.processPayment(amount);
    }
} 