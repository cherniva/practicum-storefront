package com.cherniva.payment.config;

import com.cherniva.payment.service.JwtTokenProcessService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtTokenProcessService jwtTokenProcessService;

    public SecurityConfig(JwtTokenProcessService jwtTokenProcessService) {
        this.jwtTokenProcessService = jwtTokenProcessService;
    }

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity security) throws Exception {
        return security
                .authorizeExchange(requests -> requests
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(serverSpec -> serverSpec
                        .jwt(jwtSpec -> {
                            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
                            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtTokenProcessService::extractAuthoritiesFromJwt);
                            jwtSpec.jwtAuthenticationConverter(jwtAuthenticationConverter);
                        })
                )
                .build();
    }
}
