package com.cloud_storage.common.exception;

public class UserIdConflictException extends RuntimeException {
    public UserIdConflictException(String message) {
        super(message);
    }
}
