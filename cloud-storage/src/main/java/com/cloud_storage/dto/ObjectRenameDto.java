package com.cloud_storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectRenameDto {

    private String oldName;

    private String newName;

    private String path;

    private String isDir;
}
