package com.cloud_storage.unitTest.service;

import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.UserAlreadyExistException;
import com.cloud_storage.common.exception.NotFoundException;
import com.cloud_storage.dto.LoginDto;
import com.cloud_storage.dto.UserReadDto;
import com.cloud_storage.entity.Role;
import com.cloud_storage.entity.User;
import com.cloud_storage.repository.UserRepository;
import com.cloud_storage.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder encoder;
    private final String username = "SomeUserName";
    private final String password = "SomePassword";
    private final Role role = Role.USER;
    private final User user = new User(username, password, role);
    private final LoginDto loginDto = new LoginDto(username, password);

    @Test
    void findByUsernameSuccessfully() {
        Mockito.when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        User testUser = userService.findByUsername(username);
        assertEquals(testUser.getUsername(), user.getUsername());
        assertEquals(testUser.getRole(), user.getRole());

        Mockito.verify(userRepository, Mockito.times(1)).findByUsername(username);
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenSearchFails() {
        Mockito.when(userRepository.findByUsername(username)).thenThrow(NotFoundException.class);
        assertThrows(NotFoundException.class, () -> userService.findByUsername(username));
    }

    @Test
    void saveSuccessfully() {


        Mockito.when(encoder.encode(Mockito.any())).thenReturn(password);
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

        UserReadDto result = userService.save(loginDto);

        assertNotNull(result);
        assertEquals(result.getUsername(), username);
        assertEquals(result.getRole(), Role.USER);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
    }

    @Test
    void shouldThrowInvalidParameterExceptionWhenInvalidLogin() {
        LoginDto loginDto = new LoginDto(null, password);
        Assertions.assertThrows(InvalidParameterException.class, () -> userService.save(loginDto));
    }

    @Test
    void shouldThrowUserAlreadyExistExceptionWhenUserAlreadyExist() {
        Mockito.when(encoder.encode(Mockito.any())).thenReturn(password);
        Mockito.doThrow(UserAlreadyExistException.class).when(userRepository).save(Mockito.any(User.class));
        Assertions.assertThrows(UserAlreadyExistException.class, () -> userService.save(loginDto));
    }

    @Test
    void deleteSuccessfully() {
        UserService spyUserService = Mockito.spy(userService);

        Mockito.doReturn(user).when(spyUserService).findByUsername(username);
        spyUserService.delete(username);

        Mockito.verify(userRepository, Mockito.times(1)).delete(user);
    }
}
