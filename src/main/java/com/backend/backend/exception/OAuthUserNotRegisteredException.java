package com.backend.backend.exception;

public class OAuthUserNotRegisteredException extends RuntimeException {

    public OAuthUserNotRegisteredException() {
        super("User not invited. Please contact your administrator.");
    }
}
