package com.cloud_storage.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/image/**").permitAll()
                        .requestMatchers("/storage/guest-page", "/user/sign-up", "/user").permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/user/sign-in")
                        .defaultSuccessUrl("/storage", true)
                        .failureUrl("/user/sign-in?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/sign-out")
                        .logoutSuccessUrl("/storage/guest-page")
                        .deleteCookies("JSESSIONID")
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
