package com.cloud_storage.user.dto;

import com.cloud_storage.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserReadDto {

    int Id;

    String username;

    String password;

    Role role;
}
