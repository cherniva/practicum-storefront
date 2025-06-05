package com.cherniva.payment.controller;

import com.cherniva.payment.api.PaymentApi;
import com.cherniva.payment.api.BalanceApi;
import com.cherniva.payment.model.Error;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@Controller
public class PaymentControllerImpl implements PaymentApi, BalanceApi {
    private final AtomicReference<BigDecimal> balance;

    public PaymentControllerImpl(@Value("${payment.balance:10000}") BigDecimal initialBalance) {
        this.balance = new AtomicReference<>(initialBalance);
    }

    @Override
    public Mono<ResponseEntity<Double>> getBalance(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(balance.get().doubleValue()));
    }

    @Override
    public Mono<ResponseEntity<Double>> processPayment(Double amount, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> {
            if (amount <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive");
            }

            return balance.updateAndGet(currentBalance -> {
                BigDecimal newBalance = currentBalance.subtract(BigDecimal.valueOf(amount));
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
                }
                return newBalance;
            });
        }).map(newBalance -> ResponseEntity.ok(newBalance.doubleValue()));
    }
} 