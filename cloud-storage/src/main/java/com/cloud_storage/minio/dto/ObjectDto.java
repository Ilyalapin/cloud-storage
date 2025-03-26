package com.cloud_storage.minio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectDto {

    private String name;

    private String path;
}
