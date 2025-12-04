package com.bm.wschat.shared.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Contract;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {
    @NotNull
    @Contract("_ -> new")
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }

    @Contract("_, _ -> new")
    public static <T> @NotNull ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now() );
    }

    @Contract("_ -> new")
    public static <T> @NotNull ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }

    @Contract("_ -> new")
    public static <T> @NotNull ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }

    @Contract("_, _ -> new")
    public static <T> @NotNull ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, Instant.now());
    }
}
