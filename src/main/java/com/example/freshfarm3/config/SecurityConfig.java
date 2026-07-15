package com.example.freshfarm3.config;

import com.example.freshfarm3.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter       jwtAuthFilter;
    private final UserDetailsService  userDetailsService;

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "https://yourapp.com",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5500",
            "http://localhost:5500",
            "http://127.0.0.1:5501",
            "http://localhost:5501"
    );

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/shop/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        .requestMatchers(
                                "/", "/index.html", "/shop.html", "/cart.html",
                                "/checkout.html", "/login.html", "/register.html",
                                "/product-detail.html", "/order-success.html",
                                "/orders.html", "/subscriptions.html", "/forgot-password.html",
                                "/shop/**", "/admin/**", "/agent/**",
                                "/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico"
                        ).permitAll()

                        .requestMatchers("/api/buyers/**").hasRole("BUYER")
                        .requestMatchers("/api/shops/**").hasAnyRole("SHOP", "ADMIN")

                        .requestMatchers("/api/cart/**").hasRole("BUYER")

                        // More specific /api/orders/** rules MUST come before the general one below,
                        // since Spring Security uses first-match-wins.
                        .requestMatchers("/api/orders/shop/**").hasAnyRole("SHOP", "ADMIN")
                        .requestMatchers("/api/orders/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/orders/**").hasAnyRole("BUYER", "ADMIN")

                        .requestMatchers("/api/addresses/**").hasRole("BUYER")

                        .requestMatchers("/api/payments/**").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/delivery/**").hasAnyRole("AGENT", "ADMIN", "BUYER")
                        .requestMatchers(HttpMethod.PUT, "/api/delivery/**").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/delivery/**").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/delivery/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET,  "/api/reviews/my-reviews").hasRole("BUYER")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyRole("BUYER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/coupons/validate").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET,  "/api/coupons").hasAnyRole("ADMIN", "BUYER")
                        .requestMatchers("/api/coupons/create").hasRole("ADMIN")
                        .requestMatchers("/api/coupons/update/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/coupons/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/subscriptions/**").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions/**").hasRole("BUYER")
                        .requestMatchers(HttpMethod.PUT, "/api/subscriptions/**").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/subscriptions/**").hasAnyRole("BUYER", "ADMIN")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}