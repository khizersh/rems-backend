package com.rem.backend.config;


import com.rem.backend.usermanagement.middleware.JWTFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig  {

    private final JWTFilter jwtFilter;

    public SecurityConfig(JWTFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for JWT-based auth
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless authentication
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/user/login").permitAll() // Make login public
                        .requestMatchers("/api/user/send-reset-link/**").permitAll() // Make login public
                        .requestMatchers("/api/user/change-password/**").permitAll() // Make login public
                        .requestMatchers("/api/user/verify-reset-link/**").permitAll() // Make login public
                        .requestMatchers("/public/**").permitAll() // Any public routes
                        .anyRequest().authenticated() // Protect all other routes
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // Add JWT filter
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS
                .formLogin(form -> form.disable()) // Disable form login
                .httpBasic(basic -> basic.disable()); // Disable HTTP Basic authentication

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",                // Local dev
                "https://portal.propertydhoondo.com"    // Production frontend
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}