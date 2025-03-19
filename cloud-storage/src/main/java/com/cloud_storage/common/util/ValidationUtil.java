package com.cloud_storage.common.util;

import com.cloud_storage.common.exception.InvalidParameterException;

public class ValidationUtil {
    private static final String STARTS_WITH_LETTER_OR_DIGIT = "^[a-zA-Zа-яА-Я0-9]";
    private static final String VALID_CHARACTERS = "[a-zA-Zа-яА-Я0-9 _\\./-]*$";

        public static void validate(String name){

        if(name == null || name.isEmpty()){
            throw new InvalidParameterException("Missing parameter: name");
        }
        if(!name.matches(STARTS_WITH_LETTER_OR_DIGIT + VALID_CHARACTERS)) {
            throw new InvalidParameterException("Name must contains at least one letter or digit");
        }
    }
}
