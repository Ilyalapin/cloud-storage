package com.cloud_storage.common.util;

import com.cloud_storage.minio.dto.ObjectReadDto;

public class PrefixGenerationUtil {

    public static String generateIfPathIsEmpty(String path, ObjectReadDto folderDto) {
        if (path == null || path.isEmpty()) {
            return folderDto.getName();
        }
        return path;
    }


    public static String generatePathForBreadCrumbs(String obJectName, ObjectReadDto folderDto) {

        if (obJectName == null || obJectName.isEmpty()) {
            return folderDto.getName();
        }
        int firstSlashIndex = obJectName.indexOf("/");

        return obJectName.substring(firstSlashIndex + 1);
    }


    public static String generatePathBackForBreadCrumbs(String obJectName) {
        if (obJectName.isEmpty()) return "";
        obJectName = obJectName.substring(0, obJectName.lastIndexOf('/'));
        return obJectName.substring(0, obJectName.lastIndexOf('/') + 1);
    }


    public static String generateFolderName(String obJectName) {

        if (obJectName == null || obJectName.isEmpty()) {
            return "folder";
        }
        obJectName = obJectName.replaceAll("/$", "");

        int lastSlashIndex = obJectName.lastIndexOf("/");
        String folderName = obJectName.substring(lastSlashIndex + 1);

        return folderName.replaceAll("/$", "");
    }


    public static String removePrefixForZipDirectory(String fullPath) {
        if (fullPath == null || fullPath.isEmpty()) {
            return "folder";
        }

        int firstSlashIndex = fullPath.indexOf("/");
        String intermediatePath = fullPath.substring(firstSlashIndex + 1);

        int secondSlashIndex = intermediatePath.indexOf("/");
        return intermediatePath.substring(secondSlashIndex + 1);
    }


    public static String generateNewPathForCopyObject(String path) {

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int lastSlashIndex = path.lastIndexOf("/");
        String basePath = path.substring(0, lastSlashIndex);

        return basePath + "/";
    }

    public static int getUserIdFromPath(String path, ObjectReadDto rootFolder) {
        if (path == null || path.isEmpty()) {
            path = rootFolder.getName();
        }
        int firstDashIndex = path.indexOf("-");
        int secondDashIndex = path.indexOf("-", firstDashIndex + 1);

        return Integer.parseInt(path.substring(firstDashIndex + 1, secondDashIndex));
    }
}
