package com.cherniva.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

@Configuration
public class LoggingConfig {
    
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter requestLoggingFilter() {
        return (exchange, chain) -> {
            // Log incoming request details
            String method = exchange.getRequest().getMethod().name();
            String path = exchange.getRequest().getPath().value();
            String remoteAddress = exchange.getRequest().getRemoteAddress() != null ? 
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            
            System.out.println("=== Incoming Request ===");
            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            System.out.println("Remote Address: " + remoteAddress);
            System.out.println("Headers: " + exchange.getRequest().getHeaders());
            
            return chain.filter(exchange)
                    .doFinally(signalType -> {
                        System.out.println("=== Request Completed ===");
                        System.out.println("Method: " + method + ", Path: " + path + ", Signal: " + signalType);
                        System.out.println("Response Status: " + exchange.getResponse().getStatusCode());
                        System.out.println("========================\n");
                    });
        };
    }
} 