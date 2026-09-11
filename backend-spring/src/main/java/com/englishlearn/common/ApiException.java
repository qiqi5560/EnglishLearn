package com.englishlearn.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：携带 HTTP 状态码（与响应 body.code 一致）。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public ApiException(int code, String message) {
        this(HttpStatus.valueOf(code), message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return status.value();
    }
}