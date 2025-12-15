package com.bm.wschat.feature.wiki.dto.response;

import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;
import java.util.Set;

/**
 * Полный ответ со статьей
 */
public record WikiArticleResponse(
                Long id,
                String title,
                String slug,
                String content,
                String excerpt,
                Long categoryId,
                String categoryName,
                Set<String> tags,
                UserShortResponse createdBy,
                UserShortResponse updatedBy,
                Long viewCount,
                Long likeCount,
                Boolean likedByCurrentUser,
                Instant createdAt,
                Instant updatedAt) {
}
