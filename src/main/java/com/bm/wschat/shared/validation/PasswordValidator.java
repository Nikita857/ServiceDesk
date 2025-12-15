package com.bm.wschat.shared.validation;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])" +         // at least 1 digit
        "(?=.*[a-z])" +          // at least 1 lowercase letter
        "(?=.*[A-Z])" +          // at least 1 uppercase letter
        "(?=.*[!@#&()–[{}]:;',?/*~$^+=<>])" + // at least 1 special character
        "(?=\\S+$).{8,}$"       // no whitespace, at least 8 characters
    );

    public boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public String getValidationMessage() {
        return "Пароль должен содержать минимум 8 символов, как минимум 1 цифру, " +
               "одну букву ниженего регистра, одну верхнего регистра, один спец символ, и не содержать пробелов.";
    }
}