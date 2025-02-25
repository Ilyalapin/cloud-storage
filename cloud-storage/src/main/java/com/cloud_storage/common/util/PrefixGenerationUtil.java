package com.cloud_storage.common.util;

import com.cloud_storage.dto.ObjectReadDto;

public class PrefixGenerationUtil {

    public static String generatePath(String path, ObjectReadDto folderDto) {
        if (path == null || path.isEmpty()) {
            return folderDto.getName();
        }
        return path;
    }

    public static String getBackPath(String obJectName) {
        if (obJectName.isEmpty()) return "";
        obJectName = obJectName.substring(0, obJectName.lastIndexOf('/'));
        return obJectName.substring(0, obJectName.lastIndexOf('/') + 1);
    }


    public static String generateFolderNameForView(String obJectName) {

        if (obJectName == null || obJectName.isEmpty()) {
            return "folder";
        }
        obJectName = obJectName.replaceAll("/$", "");

        int lastSlashIndex = obJectName.lastIndexOf("/");
        String folderName = obJectName.substring(lastSlashIndex + 1);

        return folderName.replaceAll("/$", "");
    }


    public static String generateFromDirectory(String obJectName, ObjectReadDto folderDto) {

        if (obJectName == null || obJectName.isEmpty()) {
            return folderDto.getName();
        }
        int firstSlashIndex = obJectName.indexOf("/");

        return obJectName.substring(firstSlashIndex + 1);
    }

    public static String generateForDeleteObject(String path,String obJectName,ObjectReadDto folderDto) {
        if (obJectName == null || obJectName.isEmpty()) {
            return folderDto.getName();
        }
        return path+obJectName+"/";
    }
}
