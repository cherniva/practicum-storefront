package com.cherniva.storefront.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public ReactiveAuthenticationManager authenticationManager(
            com.cherniva.storefront.service.CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager manager = 
            new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        manager.setPasswordEncoder(passwordEncoder);
        
        // Test password encoding
        String testPassword = "password123";
        String encodedPassword = passwordEncoder.encode(testPassword);
        boolean matches = passwordEncoder.matches(testPassword, encodedPassword);
        System.out.println("[SecurityConfig] Password test - Original: " + testPassword + ", Encoded: " + encodedPassword + ", Matches: " + matches);
        
        // Test with the actual password from database
        String dbPassword = "$2a$10$TuHQiSu41dRxXr2tQDNuDO8T4BBe6TpxSBoTIHKpHp8ac2RL5RML2";
        boolean dbMatches = passwordEncoder.matches(testPassword, dbPassword);
        System.out.println("[SecurityConfig] Database password test - Matches: " + dbMatches);
        
        return manager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager authenticationManager) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/login", "/debug/user").permitAll()
                        .anyExchange().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((webFilterExchange, authentication) -> 
                            redirectToLoginWithLogoutParam(webFilterExchange)
                        )
                )
                .securityContextRepository(new WebSessionServerSecurityContextRepository())
                .build();
    }
    
    private Mono<Void> redirectToLoginWithLogoutParam(WebFilterExchange webFilterExchange) {
        ServerWebExchange exchange = webFilterExchange.getExchange();
        String loginUrl = "/login?logout";
        exchange.getResponse().getHeaders().add("Location", loginUrl);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        return exchange.getResponse().setComplete();
    }
}
