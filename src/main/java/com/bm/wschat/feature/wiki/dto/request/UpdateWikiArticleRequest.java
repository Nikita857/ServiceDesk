package com.bm.wschat.feature.wiki.dto.request;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateWikiArticleRequest(
        @Size(max = 250, message = "Title must not exceed 250 characters") String title,

        String content,

        @Size(max = 500, message = "Excerpt must not exceed 500 characters") String excerpt,

        Long categoryId,

        Set<String> tags) {
}
