package com.cloud_storage.common.util;

import com.cloud_storage.common.exception.MinioInvalidParameterException;
import com.cloud_storage.common.exception.UserIdConflictException;
import com.cloud_storage.minio.dto.ObjectRenameDto;

public class ValidationUtil {

    public static void validate(String name) {
        final String STARTS_WITH_LETTER_OR_DIGIT = "^[a-zA-Zа-яА-Я0-9]";
        final String VALID_CHARACTERS = "[a-zA-Zа-яА-Я0-9 _\\./-]*$";

        if (name == null || name.isEmpty()) {
            throw new MinioInvalidParameterException("Missing parameter: name");
        }
        if (!name.matches(STARTS_WITH_LETTER_OR_DIGIT + VALID_CHARACTERS)) {
            throw new MinioInvalidParameterException("Name must contains at least one letter or digit");
        }
    }


    public static void validate(ObjectRenameDto renameDto) {

        String oldName = renameDto.getOldName();
        String newName = renameDto.getNewName();

        if (renameDto.getIsDir().equals(String.valueOf(true))) {
            validate(newName);
        } else {
            validate(newName);
            int oldIndex = oldName.lastIndexOf(".");
            int newIndex = newName.lastIndexOf(".");

            if (newIndex == -1) {
                throw new MinioInvalidParameterException("Add the file extension!");
            }
            String oldExtension = oldName.substring(oldIndex + 1).toLowerCase();
            String newExtension = newName.substring(newIndex + 1).toLowerCase();

            if (!oldExtension.equals(newExtension)) {
                throw new MinioInvalidParameterException("File extensions must match!");
            }
        }
    }


    public static void validate(int userId, int idFromPath) {
        if(userId != idFromPath){
            throw new UserIdConflictException("Access error for a given ID");
        }
    }
}
