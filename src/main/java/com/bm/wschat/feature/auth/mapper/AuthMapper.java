package com.bm.wschat.feature.auth.mapper;

import com.bm.wschat.feature.user.dto.response.UserAuthResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.storage.MinioStorageService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class AuthMapper {

    @Autowired
    protected MinioStorageService minioStorageService;

    @Mapping(source = "id", target = "id")
    @Mapping(source = "fio", target = "fio")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "user", target = "avatarUrl", qualifiedByName = "toAvatarUrl")
    @Mapping(source = "telegramId", target = "telegramId")
    @Mapping(source = "specialist", target = "specialist")
    @Mapping(source = "roles", target = "roles")
    @Mapping(source = "active", target = "active")
    public abstract UserAuthResponse toAuthResponse(User user);

    @Named("toAvatarUrl")
    protected String toAvatarUrl(User user) {
        if (user == null || user.getAvatarUrl() == null) {
            return null;
        }
        return minioStorageService.generateDownloadUrl(
                user.getAvatarUrl(),
                minioStorageService.getBucket(MinioStorageService.BucketType.CHAT),
                user.getAvatarUrl());
    }
}
