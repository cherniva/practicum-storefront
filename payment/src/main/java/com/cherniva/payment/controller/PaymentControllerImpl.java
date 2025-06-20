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
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Controller
@Slf4j
public class PaymentControllerImpl implements PaymentApi, BalanceApi {
    // Global balance (used as default for new users)
    private final AtomicReference<BigDecimal> globalBalance;
    
    // User-specific balances
    private final ConcurrentHashMap<Long, AtomicReference<BigDecimal>> userBalances;

    public PaymentControllerImpl(@Value("${payment.balance:10000}") BigDecimal initialBalance) {
        this.globalBalance = new AtomicReference<>(initialBalance);
        this.userBalances = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<ResponseEntity<Double>> getBalance(ServerWebExchange exchange) {
        String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            log.warn("Unauthorized access attempt: No user ID provided");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        
        try {
            Long userId = Long.parseLong(userIdHeader);
            log.info("Getting balance for user: {}", userId);
            
            AtomicReference<BigDecimal> userBalance = userBalances.computeIfAbsent(userId, 
                k -> {
                    log.info("Creating new balance for user: {} with default amount: {}", userId, globalBalance.get());
                    return new AtomicReference<>(globalBalance.get());
                });
            
            return Mono.just(ResponseEntity.ok(userBalance.get().doubleValue()));
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID in header: {}", userIdHeader);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user ID format");
        }
    }

    @Override
    public Mono<ResponseEntity<Double>> processPayment(Double amount, ServerWebExchange exchange) {
        if (amount == null || amount <= 0.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Payment amount must be greater than 0.01");
        }

        String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            log.warn("Unauthorized payment attempt: No user ID provided");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        
        try {
            Long userId = Long.parseLong(userIdHeader);
            log.info("Processing payment for user: {}, amount: {}", userId, amount);
            
            AtomicReference<BigDecimal> userBalance = userBalances.computeIfAbsent(userId, 
                k -> {
                    log.info("Creating new balance for user: {} with default amount: {}", userId, globalBalance.get());
                    return new AtomicReference<>(globalBalance.get());
                });
            
            return Mono.fromCallable(() -> 
                userBalance.updateAndGet(currentBalance -> {
                    BigDecimal newBalance = currentBalance.subtract(BigDecimal.valueOf(amount));
                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Insufficient balance for user " + userId + ". Current: " + currentBalance + ", Required: " + amount);
                    }
                    return newBalance;
                })
            ).map(newBalance -> {
                log.info("Payment processed for user: {}, new balance: {}", userId, newBalance);
                return ResponseEntity.ok(newBalance.doubleValue());
            }).onErrorResume(ResponseStatusException.class, Mono::error);
            
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID in header: {}", userIdHeader);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user ID format");
        }
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<Error>> handleValidationExceptions(jakarta.validation.ConstraintViolationException ex) {
        Error error = new Error()
            .message(ex.getMessage());
        return Mono.just(ResponseEntity.badRequest().body(error));
    }
} 