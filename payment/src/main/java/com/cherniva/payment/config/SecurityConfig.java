package com.cherniva.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity security) throws Exception {
        return security
                .authorizeExchange(requests -> requests
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(serverSpec -> serverSpec
                        .jwt(jwtSpec -> {
                            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
                            jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
                                System.out.println("=== JWT Token Analysis ===");
                                System.out.println("Subject: " + jwt.getSubject());
                                System.out.println("Issuer: " + jwt.getIssuer());
                                System.out.println("All Claims: " + jwt.getClaims());
                                
                                List<String> roles = new ArrayList<>();
                                
                                // Try to get roles from different possible locations in Keycloak JWT
                                List<String> directRoles = jwt.getClaim("roles");
                                if (directRoles != null && !directRoles.isEmpty()) {
                                    roles.addAll(directRoles);
                                }
                                
                                // Check realm_access.roles (Keycloak standard)
                                Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                                if (realmAccess != null) {
                                    List<String> realmRoles = (List<String>) realmAccess.get("roles");
                                    if (realmRoles != null) {
                                        roles.addAll(realmRoles);
                                    }
                                }
                                
                                // Check resource_access (client-specific roles)
                                Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
                                if (resourceAccess != null) {
                                    // Check for your specific client
                                    Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("payment-test");
                                    if (clientAccess != null) {
                                        List<String> clientRoles = (List<String>) clientAccess.get("roles");
                                        if (clientRoles != null) {
                                            roles.addAll(clientRoles);
                                        }
                                    }
                                }
                                
                                // If no roles found, use a default role
                                if (roles.isEmpty()) {
                                    System.out.println("No roles found in token, using default role");
                                    roles.add("USER");
                                }
                                
                                System.out.println("Final Roles: " + roles);
                                System.out.println("========================\n");

                                // Возвращаемый тип — Flux
                                return Flux.fromIterable(roles)
                                        .map(SimpleGrantedAuthority::new);
                            });

                            jwtSpec.jwtAuthenticationConverter(jwtAuthenticationConverter);
                        })
                )
                .build();
    }
}
