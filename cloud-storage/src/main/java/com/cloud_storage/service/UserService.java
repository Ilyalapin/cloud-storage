package com.cloud_storage.service;

import com.cloud_storage.common.MapingUtil;
import com.cloud_storage.dto.LoginDto;
import com.cloud_storage.dto.UserCreateDto;
import com.cloud_storage.entity.Role;
import com.cloud_storage.entity.User;
import com.cloud_storage.exception.UserAlreadeExistException;
import com.cloud_storage.exception.UserNotFoundException;
import com.cloud_storage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;


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
    public UserCreateDto create(LoginDto loginDto) {
        UserCreateDto userDto = new UserCreateDto();
        userDto.setUsername(loginDto.getUsername());
        userDto.setPassword(encoder.encode(loginDto.getPassword()));
        userDto.setRole(Role.USER);

        try {
            userRepository.save(MapingUtil.convertToEntity(userDto));
            return userDto;
        } catch (ConstraintViolationException e) {
            throw new UserAlreadeExistException("User with login " + userDto.getUsername() + " already exists");
        }
    }

    @Transactional
    public boolean delete(int id) {
        return userRepository.findById(id)
                .map(entity -> {
                    userRepository.delete(entity);
//                            userRepository.flush();
                    return true;
                })
                .orElse(false);


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
