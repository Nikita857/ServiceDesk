package com.bm.wschat.feature.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content is required") @Size(max = 10000, message = "Message must not exceed 10000 characters") String content,

        Boolean internal) {
    public SendMessageRequest {
        if (internal == null) {
            internal = false;
        }
    }
}
