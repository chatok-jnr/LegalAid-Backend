package com.legalaid.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T       data;
    private final String  message;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, String message) {
        this.success   = success;
        this.data      = data;
        this.message   = message;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<T>(true, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<T>(true, data, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<T>(false, null, message);
    }
}