package com.council.tenderapp.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.council.tenderapp.reg.security.TokenAuthenticationFilter;
import com.council.tenderapp.operator.reset.ResetCoordinator;
import com.council.tenderapp.operator.reset.ResetCoordinationFilter;
import com.council.tenderapp.reg.services.SessionService;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity 
@EnableMethodSecurity //Allows use of @PreAuthorize in controller methods for specific users
public class SecurityConfig 
{
    @Value("${app.cors.allowed-origins:http://localhost:8081}")
    private String[] allowedOrigins;

    private final SessionService sessionService;

    public SecurityConfig(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Bean
    public ResetCoordinator resetCoordinator() { return new ResetCoordinator(); }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.asList(allowedOrigins));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        source.registerCorsConfiguration("/api/**", config);
        
        return source;
    }

    @Bean 
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ResetCoordinator resetCoordinator) throws Exception {
        http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/menu-items-imgs/*.jpg").permitAll()
            .requestMatchers(HttpMethod.GET, "/menu-photos/*").permitAll()
            .requestMatchers(HttpMethod.GET, "/health").permitAll()
            .requestMatchers("/api/auth/**", "/error").permitAll()
            .requestMatchers("/api/operator/**").hasRole("OPERATOR")
            .anyRequest().authenticated()
        )
        //forces 401 if authentication is invalid
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        )
        .addFilterBefore(new ResetCoordinationFilter(resetCoordinator), UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(new TokenAuthenticationFilter(sessionService), ResetCoordinationFilter.class);

        return http.build();
    }
}
