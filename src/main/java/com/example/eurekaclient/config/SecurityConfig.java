package com.example.eurekaclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.authorization.AuthorizationManagers.anyOf;
import static org.springframework.security.web.access.IpAddressAuthorizationManager.hasIpAddress;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/instances/**").access(anyOf(hasIpAddress("127.0.0.1"), hasIpAddress("::1")))
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
