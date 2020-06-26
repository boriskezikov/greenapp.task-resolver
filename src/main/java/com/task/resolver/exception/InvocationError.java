package com.task.resolver.exception;

public enum InvocationError {

    POSTGRES_INVOCATION_ERROR(500, "Error invoking Postgres");

    public final int status;
    public final String description;

    InvocationError(int status, String description) {
        this.status = status;
        this.description = description;
    }

    public InvocationErrorException exception(Throwable e) {
        return new InvocationErrorException(e, this);
    }

    public static class InvocationErrorException extends HttpCodeException {

        public InvocationErrorException(Throwable e, InvocationError error) {
            super(e, error.status, error.description);
        }
    }
}
