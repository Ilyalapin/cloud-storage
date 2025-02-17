package com.cloud_storage.common.util;

import com.cloud_storage.dto.ObjectReadDto;

public class PrefixGenerationUtil {

    public static String generatePath(String path, ObjectReadDto folderDto) {
        if (path == null || path.isEmpty()) {
            return folderDto.getName();
        }
        return path;
    }


//    public static String generate(String path, boolean isFile) {
//        String result = "/" + path;
//        if (path == null || path.isEmpty() || path.equals("/")) {
//            return "/";
//        }
//        if (!isFile) {
////            path += "/";
//            result += "/";
//        }
////        return path;
//        return result;
//    }


//    public static List<String> generateFromDirectory(String path) {
//        if (path.isEmpty()) {
//            return List.of(path);
//        }
//
//        List<String> links = new ArrayList<>();
//
//        for (int i = 0; i < path.length(); i++) {
//            if (path.charAt(i) == '/') {
//                links.add(path.substring(0, i));
//            }
//        }
//        return links;
//    }

    public static String generateFolderNameforView(String obJectName) {

        if (obJectName == null || obJectName.isEmpty()) {
            return "folder";
        }
        int lastSlashIndex = obJectName.indexOf("/");

        String folderName = obJectName.substring(lastSlashIndex + 1);

        return folderName.replaceAll("/$", "");
    }
}
