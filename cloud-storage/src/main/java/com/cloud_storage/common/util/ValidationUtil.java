package com.cloud_storage.common.util;

import com.cloud_storage.common.exception.InvalidParameterException;

public class ValidationUtil {
    private static final String AT_LEAST_ONE_LETTER_OR_DIGIT = "^(?=.*[a-zA-Zа-яА-Я0-9])";
    private static final String ONLY_LETTERS_OR_DIGITS_AND_GAPS = "[a-zA-Zа-яА-Я0-9 _-]+$";


    public static void validate(String name){

        if(name == null || name.isEmpty()){
            throw new InvalidParameterException("Missing parameter: name");
        }
        if(!name.matches(AT_LEAST_ONE_LETTER_OR_DIGIT + ONLY_LETTERS_OR_DIGITS_AND_GAPS)) {
            throw new InvalidParameterException("Name must contains at least one letter or digit");
        }
    }
}
