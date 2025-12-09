package com.bm.wschat.feature.auth.mapper;

import com.bm.wschat.feature.user.dto.response.UserAuthResponse;
import com.bm.wschat.feature.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "fio", target = "fio")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "telegramId", target = "telegramId")
    @Mapping(source ="specialist", target = "specialist")
    @Mapping(source = "roles", target = "roles")
    @Mapping(source = "active", target = "active")
    UserAuthResponse toAuthResponse(User user);
}
