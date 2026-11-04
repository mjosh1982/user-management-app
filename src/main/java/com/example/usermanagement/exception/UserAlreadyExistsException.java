package com.example.usermanagement.exception;

public class UserAlreadyExistsException extends ApiException {

    public UserAlreadyExistsException(String firstName, String lastName) {
        super(ErrorCode.USER_ALREADY_EXISTS,
                "User '" + firstName + " " + lastName + "' already exists");
    }
}
