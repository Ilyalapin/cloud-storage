package com.cloud_storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectReadDto {

    private String name;

    private boolean isDir;

    private String path;

    private Long size;

    public ObjectReadDto(String name, boolean isDir, String path) {
        this.name = name;
        this.isDir = isDir;
        this.path = path;
    }
}
