package com.cloud_storage.common;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ErrorResponseDto {
    Integer code;

    String message;
}
