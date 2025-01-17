package com.cloud_storage.dto;

import com.cloud_storage.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class UserCreateDto {

    String username;

    String password;

    Role role;
}

