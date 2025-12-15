package com.bm.wschat.feature.wiki.dto.response;

import java.time.Instant;
import java.util.Set;

/**
 * Краткий ответ для списка (без content)
 */
public record WikiArticleListResponse(
                Long id,
                String title,
                String slug,
                String excerpt,
                String categoryName,
                Set<String> tags,
                String authorName,
                Long viewCount,
                Long likeCount,
                Boolean likedByCurrentUser,
                Instant updatedAt) {
}
