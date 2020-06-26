package com.task.resolver.exception;

public enum AuthenticationError {

    UNAUTHORIZED(401, "Unauthorized");

    public final int status;
    public final String description;

    AuthenticationError(int status, String description) {
        this.status = status;
        this.description = description;
    }

    public AuthenticationErrorException exception() {
        return new AuthenticationErrorException(this);
    }

    public static class AuthenticationErrorException extends HttpCodeException {

        public AuthenticationErrorException(AuthenticationError error) {
            super(error.status, error.description);
        }
    }
}
