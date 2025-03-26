package com.cloud_storage.common.util;

import com.cloud_storage.common.exception.InvalidParameterException;
import com.cloud_storage.minio.dto.ObjectRenameDto;

public class ValidationUtil {
    private static final String STARTS_WITH_LETTER_OR_DIGIT = "^[a-zA-Zа-яА-Я0-9]";
    private static final String VALID_CHARACTERS = "[a-zA-Zа-яА-Я0-9 _\\./-]*$";

    public static void validate(String name) {

        if (name == null || name.isEmpty()) {
            throw new InvalidParameterException("Missing parameter: name");
        }
        if (!name.matches(STARTS_WITH_LETTER_OR_DIGIT + VALID_CHARACTERS)) {
            throw new InvalidParameterException("Name must contains at least one letter or digit");
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
                throw new InvalidParameterException("Add the file extension!");
            }
            String oldExtension = oldName.substring(oldIndex + 1).toLowerCase();
            String newExtension = newName.substring(newIndex + 1).toLowerCase();

            if (!oldExtension.equals(newExtension)) {
                throw new InvalidParameterException("File extensions must match!");
            }
        }
    }
}
