package com.cloud_storage.service;

import com.cloud_storage.common.util.MapingUtil;
import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.common.exception.UserAlreadyExistException;
import com.cloud_storage.common.exception.NotFoundException;
import com.cloud_storage.dto.LoginDto;
import com.cloud_storage.dto.UserCreateDto;
import com.cloud_storage.dto.UserReadDto;
import com.cloud_storage.entity.Role;
import com.cloud_storage.entity.User;
import com.cloud_storage.repository.UserRepository;
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
                        () -> new NotFoundException("User with name: " + username + " not found")
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
            throw new InvalidParameterException("Invalid username or password");
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
