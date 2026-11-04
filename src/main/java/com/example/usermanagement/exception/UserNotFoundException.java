package com.example.usermanagement.exception;

public class UserNotFoundException extends ApiException {

    private final Long userId;

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, "User with id " + userId + " not found");
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
