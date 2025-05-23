package com.cloud_storage.user.dto;

import com.cloud_storage.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
public class UserCreateDto {

    String username;

    String password;

    Role role;
}

