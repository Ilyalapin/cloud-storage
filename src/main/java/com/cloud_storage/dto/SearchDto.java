package com.cloud_storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchDto {

    private String name;

    private boolean isDir;

    private String path;

    private String size;

    private boolean isSearchResult = false;
}
