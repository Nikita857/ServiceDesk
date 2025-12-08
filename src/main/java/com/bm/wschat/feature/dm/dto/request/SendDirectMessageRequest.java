package com.bm.wschat.feature.dm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendDirectMessageRequest(
        @NotNull(message = "Recipient ID is required") Long recipientId,

        @NotBlank(message = "Message content is required") @Size(max = 10000, message = "Message must not exceed 10000 characters") String content) {
}
