package com.cloud_storage.minio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectReadDto implements Serializable {

    private String name;

    private boolean isDir;

    private String path;

    private String size;


    public ObjectReadDto(String name, boolean isDir, String path) {
        this.name = name;
        this.isDir = isDir;
        this.path = path;
    }
}
