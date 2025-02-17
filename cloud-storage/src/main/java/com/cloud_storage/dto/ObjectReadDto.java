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

//    private Long size;

}
