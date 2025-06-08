package com.cherniva.payment.controller;

import com.cherniva.payment.api.PaymentApi;
import com.cherniva.payment.api.BalanceApi;
import com.cherniva.payment.model.Error;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
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
        if (amount == null || amount <= 0.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Payment amount must be greater than 0.01");
        }

        return Mono.fromCallable(() -> 
            balance.updateAndGet(currentBalance -> {
                BigDecimal newBalance = currentBalance.subtract(BigDecimal.valueOf(amount));
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Insufficient balance. Current: " + currentBalance + ", Required: " + amount);
                }
                return newBalance;
            })
        ).map(newBalance -> ResponseEntity.ok(newBalance.doubleValue()))
          .onErrorResume(ResponseStatusException.class, Mono::error);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<Error>> handleValidationExceptions(jakarta.validation.ConstraintViolationException ex) {
        Error error = new Error()
            .message(ex.getMessage());
        return Mono.just(ResponseEntity.badRequest().body(error));
    }
} 