package com.bm.wschat.feature.supportline.mapper;

import com.bm.wschat.feature.supportline.dto.request.CreateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineResponse;
import com.bm.wschat.feature.supportline.model.SupportLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct маппер для линий поддержки.
 * Преобразование SpecialistResponse выполняется в SupportLineService
 * для добавления статусов активности.
 */
@Mapper(componentModel = "spring")
public interface SupportLineMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialists", ignore = true)
    @Mapping(target = "lastAssignedIndex", constant = "0")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    SupportLine toEntity(CreateSupportLineRequest request);

    @Mapping(target = "specialistCount", expression = "java(line.getSpecialists() != null ? line.getSpecialists().size() : 0)")
    @Mapping(target = "specialists", ignore = true) // Заполняется в сервисе с учётом статусов
    SupportLineResponse toResponse(SupportLine line);

    @Mapping(target = "specialistCount", ignore = true)
    SupportLineListResponse toListResponse(SupportLine line);

    List<SupportLineListResponse> toListResponses(List<SupportLine> lines);
}
