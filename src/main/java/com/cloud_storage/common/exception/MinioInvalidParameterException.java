package com.cloud_storage.common.exception;

public class MinioInvalidParameterException extends RuntimeException{
    public MinioInvalidParameterException(String message) {
        super(message);
    }
}
