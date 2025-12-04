package com.bm.wschat.feature.supportline.dto.response;

public record SpecialistResponse(
        Long id,
        String username,
        String fio,
        boolean active) {
}
