package com.sivayahealth.lims.exception;

import org.springframework.http.HttpStatus;

public class LimsException extends RuntimeException {
    private final HttpStatus status;

    public LimsException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static LimsException notFound(String message) {
        return new LimsException(message, HttpStatus.NOT_FOUND);
    }

    public static LimsException badRequest(String message) {
        return new LimsException(message, HttpStatus.BAD_REQUEST);
    }

    public static LimsException forbidden(String message) {
        return new LimsException(message, HttpStatus.FORBIDDEN);
    }

    public static LimsException conflict(String message) {
        return new LimsException(message, HttpStatus.CONFLICT);
    }
}
