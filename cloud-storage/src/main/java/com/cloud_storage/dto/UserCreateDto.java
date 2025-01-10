package com.cloud_storage.dto;

import com.cloud_storage.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateDto {

    private Integer Id;

    private String username;

    private String password;

    private Role role;
}
