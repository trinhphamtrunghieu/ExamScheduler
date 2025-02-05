package com.doan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity (only for development)
				.authorizeHttpRequests(auth -> auth
//                                              .requestMatchers("/Register", "/SignIn").permitAll() // Allow public access
//                                              .anyRequest().authenticated() // Require authentication for other endpoints
								.anyRequest().permitAll()
				)
				.httpBasic(httpBasic -> {}); // Enable basic authentication for testing purposes

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // Use BCrypt for password hashing
	}
}
