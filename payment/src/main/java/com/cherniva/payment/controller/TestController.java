package com.cherniva.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {
    
    @GetMapping("/test")
    public Mono<String> test() {
        return Mono.just("Test endpoint working! Check the logs above.");
    }
} 