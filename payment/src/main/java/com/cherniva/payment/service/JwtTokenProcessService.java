package com.cherniva.payment.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JwtTokenProcessService {

    public Flux<GrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        System.out.println("=== JWT Token Analysis ===");
        System.out.println("Subject: " + jwt.getSubject());
        System.out.println("Issuer: " + jwt.getIssuer());
        System.out.println("All Claims: " + jwt.getClaims());
        
        List<String> roles = new ArrayList<>();
        
        // Try to get roles from different possible locations in Keycloak JWT
        extractDirectRoles(jwt, roles);
        extractRealmAccessRoles(jwt, roles);
        extractResourceAccessRoles(jwt, roles);
        
        // If no roles found, use a default role
        if (roles.isEmpty()) {
            System.out.println("No roles found in token, using default role");
            roles.add("USER");
        }
        
        System.out.println("Final Roles: " + roles);
        System.out.println("========================\n");

        return Flux.fromIterable(roles)
                .map(SimpleGrantedAuthority::new);
    }

    private void extractDirectRoles(Jwt jwt, List<String> roles) {
        List<String> directRoles = jwt.getClaim("roles");
        if (directRoles != null && !directRoles.isEmpty()) {
            roles.addAll(directRoles);
        }
    }

    private void extractRealmAccessRoles(Jwt jwt, List<String> roles) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                roles.addAll(realmRoles);
            }
        }
    }

    private void extractResourceAccessRoles(Jwt jwt, List<String> roles) {
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
    }
} 