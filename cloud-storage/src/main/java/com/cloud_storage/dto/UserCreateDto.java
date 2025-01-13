package com.cloud_storage.dto;

import com.cloud_storage.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {

    private Integer Id;

    private String username;

    private String password;

    private Role role;

    public UserCreateDto(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
