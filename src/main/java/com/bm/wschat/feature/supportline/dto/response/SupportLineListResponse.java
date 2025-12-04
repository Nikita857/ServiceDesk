package com.bm.wschat.feature.supportline.dto.response;

public record SupportLineListResponse(
        Long id,
        String name,
        String description,
        Integer slaMinutes,
        Integer specialistCount,
        Integer displayOrder) {
}
