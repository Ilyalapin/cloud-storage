package com.cloud_storage.common.exception;

public class MinioFetchException extends RuntimeException {
    public MinioFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
