package com.cloud_storage.user.service;

import com.cloud_storage.common.util.MapingUtil;
import com.cloud_storage.common.exception.UserInvalidParameterException;
import com.cloud_storage.common.exception.UserAlreadyExistException;
import com.cloud_storage.common.exception.UserNotFoundException;
import com.cloud_storage.user.dto.LoginDto;
import com.cloud_storage.user.dto.UserCreateDto;
import com.cloud_storage.user.dto.UserReadDto;
import com.cloud_storage.entity.Role;
import com.cloud_storage.entity.User;
import com.cloud_storage.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(
                        () -> new UserNotFoundException("User with name: " + username + " not found")
                );
    }


    @Transactional
    public UserReadDto save(LoginDto loginDto) {
        UserCreateDto userCreateDto = new UserCreateDto(
                loginDto.getUsername(),
                encoder.encode(loginDto.getPassword()),
                Role.USER
        );
        if (userCreateDto.getUsername() == null || userCreateDto.getPassword() == null) {
            throw new UserInvalidParameterException("Invalid username or password");
        }
        try {
            User user = userRepository.save(MapingUtil.convertToEntity(userCreateDto));
            return MapingUtil.convertToDto(user);
        }catch (RuntimeException e) {
            throw new UserAlreadyExistException("User with login " + userCreateDto.getUsername() + " already exists");
        }
    }


    @Transactional
    public void delete(String username) {
        User user = findByUsername(username);
        userRepository.delete(user);
        log.info("User deleted successfully.");
    }
}
