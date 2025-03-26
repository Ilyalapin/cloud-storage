package com.cloud_storage.integrationTest.config;

import com.cloud_storage.user.repository.UserRepository;
import com.cloud_storage.user.service.UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class UserServiceTestConfig {
    @Bean
    public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new UserService(userRepository, passwordEncoder);
    }
}
