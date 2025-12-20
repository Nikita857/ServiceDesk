package com.bm.wschat.feature.user.dto.request;

import com.bm.wschat.feature.user.model.UserActivityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull UserActivityStatus status) { }
