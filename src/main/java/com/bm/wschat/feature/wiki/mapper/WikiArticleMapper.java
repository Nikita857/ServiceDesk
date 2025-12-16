package com.bm.wschat.feature.wiki.mapper;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleListResponse;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleResponse;
import com.bm.wschat.feature.wiki.model.WikiArticle;
import com.bm.wschat.shared.dto.UserShortResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WikiArticleMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "tags", source = "tagSet")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "likedByCurrentUser", ignore = true)
    @Mapping(target = "viewCount", source = "viewsTotal")
    WikiArticleResponse toResponse(WikiArticle article);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "tags", source = "tagSet")
    @Mapping(target = "authorName", expression = "java(article.getCreatedBy().getFio() != null ? article.getCreatedBy().getFio() : article.getCreatedBy().getUsername())")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "likedByCurrentUser", ignore = true)
    @Mapping(target = "viewCount", source = "viewsTotal")
    WikiArticleListResponse toListResponse(WikiArticle article);

    UserShortResponse toUserShortResponse(User user);
}
