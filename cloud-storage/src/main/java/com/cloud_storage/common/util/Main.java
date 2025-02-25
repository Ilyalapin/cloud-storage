package com.cloud_storage.common.util;

import com.cloud_storage.dto.ObjectReadDto;

public class Main {
    public static void main(String[] args) {
        String path ="user-9-files/storage/";
        String folderName="qwew";
        ObjectReadDto objectReadDto=new ObjectReadDto();
        String result = PrefixGenerationUtil.generateForDeleteObject(path,folderName,objectReadDto);
        System.out.println(result);
    }
}
