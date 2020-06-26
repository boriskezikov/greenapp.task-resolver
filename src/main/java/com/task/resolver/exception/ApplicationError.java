package com.task.resolver.exception;

import reactor.core.publisher.Mono;

public enum ApplicationError {

    ENTRY_NOT_FOUND_BY_TASK_ID(400, "Entry not found error");

    public final int status;
    public final String description;

    ApplicationError(int status, String description) {
        this.status = status;
        this.description = description;
    }

    public ApplicationErrorException exception(String body) {
        return new ApplicationErrorException(this, body);
    }

    public <T> Mono<T> exceptionMono(String body) {
        return Mono.error(exception(body));
    }

    public static class ApplicationErrorException extends HttpCodeException {

        public ApplicationErrorException(ApplicationError error, String args) {
            super(error.status, error.description + ": ".concat(args));
        }
    }
}
