package com.bm.wschat.feature.wiki.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.feature.wiki.dto.request.CreateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.request.UpdateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleListResponse;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleResponse;
import com.bm.wschat.feature.wiki.mapper.WikiArticleMapper;
import com.bm.wschat.feature.wiki.model.WikiArticle;
import com.bm.wschat.feature.wiki.repository.WikiArticleRepository;
import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiArticleService {

    private final WikiArticleRepository wikiArticleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final WikiArticleMapper wikiArticleMapper;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Создать статью
     */
    @Transactional
    public WikiArticleResponse createArticle(CreateWikiArticleRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Проверка уникальности title
        if (wikiArticleRepository.existsByTitle(request.title())) {
            throw new IllegalArgumentException("Article with this title already exists");
        }

        String slug = generateSlug(request.title());

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.categoryId()));
        }

        WikiArticle article = WikiArticle.builder()
                .title(request.title())
                .slug(slug)
                .content(request.content())
                .excerpt(request.excerpt())
                .category(category)
                .tagSet(request.tags() != null ? new HashSet<>(request.tags()) : new HashSet<>())
                .createdBy(author)
                .updatedBy(author)
                .build();

        WikiArticle saved = wikiArticleRepository.save(article);
        log.info("Wiki article created: id={}, slug={}", saved.getId(), slug);

        return wikiArticleMapper.toResponse(saved);
    }

    /**
     * Получить по slug
     */
    @Transactional
    public WikiArticleResponse getBySlug(String slug) {
        WikiArticle article = wikiArticleRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + slug));

        // Инкремент просмотров
        wikiArticleRepository.incrementViewCount(article.getId());

        return wikiArticleMapper.toResponse(article);
    }

    /**
     * Получить по ID
     */
    public WikiArticleResponse getById(Long id) {
        WikiArticle article = wikiArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + id));
        return wikiArticleMapper.toResponse(article);
    }

    /**
     * Список всех статей
     */
    public Page<WikiArticleListResponse> getAllArticles(Pageable pageable) {
        Page<WikiArticle> articles = wikiArticleRepository.findAllByOrderByUpdatedAtDesc(pageable);
        return articles.map(wikiArticleMapper::toListResponse);
    }

    /**
     * Популярные статьи
     */
    public Page<WikiArticleListResponse> getPopularArticles(Pageable pageable) {
        Page<WikiArticle> articles = wikiArticleRepository.findAllByOrderByViewCountDesc(pageable);
        return articles.map(wikiArticleMapper::toListResponse);
    }

    /**
     * Поиск
     */
    public Page<WikiArticleListResponse> search(String query, Pageable pageable) {
        if (query == null || query.trim().isEmpty()) {
            return getAllArticles(pageable);
        }
        Page<WikiArticle> articles = wikiArticleRepository.search(query.trim(), pageable);
        return articles.map(wikiArticleMapper::toListResponse);
    }

    /**
     * По категории
     */
    public Page<WikiArticleListResponse> getByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Category not found: " + categoryId);
        }
        Page<WikiArticle> articles = wikiArticleRepository.findByCategoryIdOrderByUpdatedAtDesc(categoryId, pageable);
        return articles.map(wikiArticleMapper::toListResponse);
    }

    /**
     * Обновить статью
     */
    @Transactional
    public WikiArticleResponse updateArticle(Long id, UpdateWikiArticleRequest request, Long userId) {
        WikiArticle article = wikiArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + id));

        User editor = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        // Проверка прав
        if (!canModify(article, userId)) {
            throw new AccessDeniedException("You don't have permission to edit this article");
        }

        if (request.title() != null && !request.title().equals(article.getTitle())) {
            if (wikiArticleRepository.existsByTitle(request.title())) {
                throw new IllegalArgumentException("Article with this title already exists");
            }
            article.setTitle(request.title());
            article.setSlug(generateSlug(request.title()));
        }
        if (request.content() != null) {
            article.setContent(request.content());
        }
        if (request.excerpt() != null) {
            article.setExcerpt(request.excerpt());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.categoryId()));
            article.setCategory(category);
        }
        if (request.tags() != null) {
            article.setTagSet(new HashSet<>(request.tags()));
        }

        article.setUpdatedBy(editor);
        WikiArticle updated = wikiArticleRepository.save(article);

        log.info("Wiki article updated: id={}", id);

        return wikiArticleMapper.toResponse(updated);
    }

    /**
     * Удалить статью
     */
    @Transactional
    public void deleteArticle(Long id, Long userId) {
        WikiArticle article = wikiArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Article not found: " + id));

        if (!canModify(article, userId)) {
            throw new AccessDeniedException("You don't have permission to delete this article");
        }

        wikiArticleRepository.delete(article); // Soft delete
        log.info("Wiki article deleted: id={}", id);
    }

    /**
     * Лайкнуть статью
     */
    @Transactional
    public void likeArticle(Long id) {
        if (!wikiArticleRepository.existsById(id)) {
            throw new EntityNotFoundException("Article not found: " + id);
        }
        wikiArticleRepository.incrementLikeCount(id);
    }

    // === Private helpers ===

    private boolean canModify(WikiArticle article, Long userId) {
        // Автор или специалист/админ
        if (article.getCreatedBy().getId().equals(userId)) {
            return true;
        }
        User user = userRepository.findById(userId).orElse(null);
        return user != null && (user.isAdmin() || user.isSpecialist());
    }

    private String generateSlug(String title) {
        String slug = Normalizer.normalize(title, Normalizer.Form.NFD);
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = NONLATIN.matcher(slug).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");

        // Уникальность
        String baseSlug = slug;
        int counter = 1;
        while (wikiArticleRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        return slug;
    }
}
