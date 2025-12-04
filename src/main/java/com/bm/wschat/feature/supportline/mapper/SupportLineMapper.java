package com.bm.wschat.feature.supportline.mapper;

import com.bm.wschat.feature.supportline.dto.request.CreateSupportLineRequest;
import com.bm.wschat.feature.supportline.dto.response.SpecialistResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineResponse;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface SupportLineMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialists", ignore = true)
    @Mapping(target = "lastAssignedIndex", constant = "0")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    @Mapping(target = "version", ignore = true)
    SupportLine toEntity(CreateSupportLineRequest request);

    @Mapping(target = "specialistCount", expression = "java(line.getSpecialists() != null ? line.getSpecialists().size() : 0)")
    SupportLineResponse toResponse(SupportLine line);

    @Mapping(target = "specialistCount", expression = "java(line.getSpecialists() != null ? line.getSpecialists().size() : 0)")
    SupportLineListResponse toListResponse(SupportLine line);

    List<SupportLineListResponse> toListResponses(List<SupportLine> lines);

    SpecialistResponse toSpecialistResponse(User user);

    List<SpecialistResponse> toSpecialistResponses(Set<User> users);
}
