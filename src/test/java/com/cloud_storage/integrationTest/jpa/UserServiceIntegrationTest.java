package com.cloud_storage.integrationTest.jpa;

import com.cloud_storage.common.exception.UserAlreadyExistException;
import com.cloud_storage.common.exception.NotFoundException;
import com.cloud_storage.user.dto.LoginDto;
import com.cloud_storage.user.dto.UserReadDto;
import com.cloud_storage.entity.User;
import com.cloud_storage.integrationTest.config.UserServiceTestConfig;
import com.cloud_storage.user.repository.UserRepository;
import com.cloud_storage.user.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@ContextConfiguration(classes = UserServiceTestConfig.class)
public class UserServiceIntegrationTest {
    private final String username = "test1";
    private final String password = "123";
    private final LoginDto loginDto = new LoginDto(username, password);
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserService userService;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres"
    );

    @BeforeEach
    public void setUp() {
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn(password);
    }

    @Test
    void findByUserNameSuccessfully() {

        UserReadDto userReadDto = userService.save(loginDto);
        User user = userService.findByUsername(userReadDto.getUsername());

        Assertions.assertNotNull(user);
        Assertions.assertEquals(userReadDto.getUsername(), user.getUsername());
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUserNotFound() {
        String nonExistUsername = "someName";
        Assertions.assertThrows(NotFoundException.class, () -> userService.findByUsername(nonExistUsername));
    }

    @Test
    void saveUserSuccessfully() {
        UserReadDto user = userService.save(loginDto);

        Assertions.assertNotNull(user);
        Assertions.assertEquals(loginDto.getUsername(), user.getUsername());
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUserAlreadyExists() {
        userService.save(loginDto);
        Assertions.assertThrows(UserAlreadyExistException.class, () -> userService.save(loginDto));
    }

    @Test
    void deleteUserSuccessfully() {
        UserReadDto user = userService.save(loginDto);
        userService.delete(username);
        Assertions.assertThrows(NotFoundException.class, () -> userService.findByUsername(user.getUsername()));
    }
}
