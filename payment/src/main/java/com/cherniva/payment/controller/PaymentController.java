package com.cherniva.payment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final AtomicReference<BigDecimal> balance;

    public PaymentController(@Value("${payment.balance:10000}") BigDecimal initialBalance) {
        this.balance = new AtomicReference<>(initialBalance);
    }

    @GetMapping("/balance")
    public Mono<BigDecimal> getBalance() {
        return Mono.just(balance.get());
    }

    @PostMapping("/payment")
    public Mono<BigDecimal> pay(@RequestParam BigDecimal amount) {
        return Mono.fromCallable(() -> {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be positive");
            }

            // Thread-safe balance update
            return balance.updateAndGet(currentBalance -> {
                BigDecimal newBalance = currentBalance.subtract(amount);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
                }
                return newBalance;
            });
        });
    }
}
