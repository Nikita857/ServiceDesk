package com.bm.wschat.feature.auth.mapper;

import com.bm.wschat.feature.user.dto.response.UserAuthResponse;
import com.bm.wschat.feature.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(source = "fio", target = "FIO")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "roles", target = "roles")
    UserAuthResponse toAuthResponse(User user);
}
