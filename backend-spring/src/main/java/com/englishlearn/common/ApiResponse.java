package com.englishlearn.common;

/**
 * 统一响应信封：{ code, message, data }，code=0 表示成功。
 * 与原 FastAPI 后端保持一致。
 */
public record ApiResponse(int code, String message, Object data) {

    public static ApiResponse ok() {
        return new ApiResponse(0, "ok", null);
    }

    public static ApiResponse ok(Object data) {
        return new ApiResponse(0, "ok", data);
    }

    public static ApiResponse ok(Object data, String message) {
        return new ApiResponse(0, message, data);
    }

    public static ApiResponse fail(int code, String message) {
        return new ApiResponse(code, message, null);
    }
}