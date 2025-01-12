package com.cloud_storage.service;

import com.cloud_storage.common.MapingUtil;
import com.cloud_storage.common.exception.UserAlreadeExistException;
import com.cloud_storage.common.exception.UserNotFoundException;
import com.cloud_storage.dto.LoginDto;
import com.cloud_storage.dto.UserCreateDto;
import com.cloud_storage.entity.Role;
import com.cloud_storage.entity.User;
import com.cloud_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
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
    public UserCreateDto save(LoginDto loginDto) {
        UserCreateDto userDto = new UserCreateDto(
                loginDto.getUsername(),
                encoder.encode(loginDto.getPassword()),
                Role.USER
        );
        try {
            userRepository.save(MapingUtil.convertToEntity(userDto));
            log.info("User saved successfully.");
            return userDto;
        } catch (RuntimeException e) {
            throw new UserAlreadeExistException("User with login " + userDto.getUsername() + " already exists");
        }
    }


    @Transactional
    public void delete(int id) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + id+ " not found"));
        userRepository.delete(entity);
        log.info("User deleted successfully.");
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singleton(new SimpleGrantedAuthority(user.getRole().name()))
                ))
                .orElseThrow(()->new UsernameNotFoundException("User with name: "+username+" not found"));

    }
}
