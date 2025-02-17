package com.cloud_storage.common.util;

import com.cloud_storage.dto.UserCreateDto;
import com.cloud_storage.dto.UserReadDto;
import com.cloud_storage.entity.User;
import org.modelmapper.ModelMapper;

public class MapingUtil {
    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    public static User convertToEntity(UserCreateDto userDto) {
        return MODEL_MAPPER.map(userDto, User.class);
    }


    public static UserReadDto convertToDto(User user) {
        return MODEL_MAPPER.map(user, UserReadDto.class);
    }
}
