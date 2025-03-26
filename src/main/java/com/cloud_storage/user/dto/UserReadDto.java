package com.cloud_storage.user.dto;

import com.cloud_storage.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReadDto {

    private int Id;

    private String username;

    private String password;

    private Role role;
}
