package com.bm.wschat.feature.wiki.service;

import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.feature.wiki.dto.request.CreateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.request.UpdateWikiArticleRequest;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleListResponse;
import com.bm.wschat.feature.wiki.dto.response.WikiArticleResponse;
import com.bm.wschat.feature.wiki.mapper.WikiArticleMapper;
import com.bm.wschat.feature.wiki.model.ArticleLike;
import com.bm.wschat.feature.wiki.model.WikiArticle;
import com.bm.wschat.feature.wiki.model.WikiArticleView;
import com.bm.wschat.feature.wiki.model.WikiArticleViewId;
import com.bm.wschat.feature.wiki.repository.ArticleLikeRepository;
import com.bm.wschat.feature.wiki.repository.WikiArticleRepository;
import com.bm.wschat.feature.wiki.repository.WikiArticleViewRepository;
import com.bm.wschat.shared.model.Category;
import com.bm.wschat.shared.repository.CategoryRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiArticleService {

    private final WikiArticleRepository wikiArticleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final WikiArticleMapper wikiArticleMapper;
    private final WikiArticleViewRepository wikiArticleViewRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Создать статью
     */
    @Transactional
    public WikiArticleResponse createArticle(CreateWikiArticleRequest request, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователья не найден: " + userId));

        // Проверка уникальности title
        if (wikiArticleRepository.existsByTitle(request.title())) {
            throw new IllegalArgumentException("Статья с этим заголовком уже есть");
        }

        String slug = generateSlug(request.title());

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + request.categoryId()));
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
                .createdAt(Instant.now())
                .build();

        WikiArticle saved = wikiArticleRepository.save(article);
        log.info("Статья вики создана: id={}, slug={}", saved.getId(), slug);

        //Делаем автора автоматически просмотревшим статью
        incrementViews(author, article);

        return wikiArticleMapper.toResponse(saved);
    }

    /**
     * Получить по slug
     */
    @Transactional
    @Cacheable(cacheNames = "wiki-article", key = "#slug")
    public WikiArticleResponse getBySlug(String slug, Long userId) {
        WikiArticle article = wikiArticleRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + slug));

        //Получаем теги отдельным запросом для избежания
        // WARN 11448 HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
        Set<String> tags = wikiArticleRepository.findTagsByArticleId(article.getId());

        User user = userRepository.findById(userId).orElseThrow(
                () -> new EntityNotFoundException("Пользователь не найден: " + userId)
        );

        //Увеличиваем счетчик просмотров
        incrementViews(user, article);

        // Получаем количество лайков из отдельной таблицы
        long likeCount = articleLikeRepository.countByArticleId(article.getId());

        // Проверяем, лайкнул ли текущий пользователь
        boolean likedByCurrentUser = articleLikeRepository.existsByArticleIdAndUserId(article.getId(), userId);

        return buildArticleResponse(article, tags, likeCount, likedByCurrentUser);
    }

    /**
     * Получить по ID
     */
    public WikiArticleResponse getById(Long id, Long userId) {
        WikiArticle article = wikiArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Статья не найдена " + id));

        //Получаем теги отдельным запросом для избежания
        // WARN 11448 HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
        Set<String> tags = wikiArticleRepository.findTagsByArticleId(article.getId());

        long likeCount = articleLikeRepository.countByArticleId(article.getId());
        boolean likedByCurrentUser = userId != null &&
                articleLikeRepository.existsByArticleIdAndUserId(article.getId(), userId);

        return buildArticleResponse(article, tags, likeCount, likedByCurrentUser);
    }

    /**
     * Список всех статей
     */
    public Page<WikiArticleListResponse> getAllArticles(Pageable pageable, Long userId) {
        Page<WikiArticle> articles = wikiArticleRepository.findAllByOrderByUpdatedAtDesc(pageable);

        Map<Long, Set<String>> articleTags = articleTagFetcher(articles);
        return articles.map(
                article -> buildListResponse(
                        article,
                        articleTags.getOrDefault(article.getId(), Set.of()),
                        userId
                )
        );
    }

    /**
     * Популярные статьи
     */
    public Page<WikiArticleListResponse> getPopularArticles(Pageable pageable, Long userId) {
        Page<WikiArticle> articles = wikiArticleRepository.findAllByOrderByViewsTotalDesc(pageable);

        Map<Long, Set<String>> articleTags = articleTagFetcher(articles);
        return articles.map(
                article -> buildListResponse(
                        article,
                        articleTags.getOrDefault(article.getId(), Set.of()),
                        userId
                )
        );
    }

    /**
     * Поиск
     */
    public Page<WikiArticleListResponse> search(String query, Pageable pageable, Long userId) {
        if (query == null || query.trim().isEmpty()) {
            return getAllArticles(pageable, userId);
        }
        Page<WikiArticle> articles = wikiArticleRepository.search(query.trim(), pageable);
        Map<Long, Set<String>> articleTags = articleTagFetcher(articles);
        return articles.map(
                article -> buildListResponse(
                        article,
                        articleTags.getOrDefault(article.getId(), Set.of()),
                        userId
                )
        );
    }

    /**
     * По категории
     */
    public Page<WikiArticleListResponse> getByCategory(Long categoryId, Pageable pageable, Long userId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Категория не найдена: " + categoryId);
        }
        Page<WikiArticle> articles = wikiArticleRepository.findByCategoryIdOrderByUpdatedAtDesc(categoryId, pageable);
        Map<Long, Set<String>> articleTags = articleTagFetcher(articles);
        return articles.map(
                article -> buildListResponse(
                        article,
                        articleTags.getOrDefault(article.getId(), Set.of()),
                        userId
                )
        );
    }

    /**
     * Обновить статью
     */
    @CacheEvict(cacheNames = "wiki-article", key = "#request.title()")
    @Transactional
    public WikiArticleResponse updateArticle(Long id, UpdateWikiArticleRequest request, Long userId) {
        WikiArticle article = wikiArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + id));

        User editor = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Проверка прав
        if (!canModify(article, userId)) {
            throw new AccessDeniedException("У вас нет прав для редактирования этой статьи");
        }

        if (request.title() != null && !request.title().equals(article.getTitle())) {
            if (wikiArticleRepository.existsByTitle(request.title())) {
                throw new IllegalArgumentException("Статья с этим заголовком уже существует");
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
                    .orElseThrow(() -> new EntityNotFoundException("Категория не найдена: " + request.categoryId()));
            article.setCategory(category);
        }
        if (request.tags() != null) {
            article.setTagSet(new HashSet<>(request.tags()));
        }

        article.setUpdatedBy(editor);
        WikiArticle updated = wikiArticleRepository.save(article);

        log.info("Вики статья обновлена: id={}", id);

        return wikiArticleMapper.toResponse(updated);
    }

    /**
     * Удалить статью
     */
    @Transactional
    @CacheEvict(cacheNames = "wiki-article", key = "#id")
    public void deleteArticle(Long id, Long userId) {
        WikiArticle article = wikiArticleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + id));

        if (!canModify(article, userId)) {
            throw new AccessDeniedException("У вас нет прав удалять эту статью");
        }

        wikiArticleRepository.delete(article); // Soft delete
        log.info("Вики статья удалена: id={}", id);
    }

    /**
     * Лайкнуть статью
     */
    @Transactional
    @CacheEvict(cacheNames = "wiki-article", key = "#id")
    public void likeArticle(Long id, User user) {
        WikiArticle article = wikiArticleRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Статья не найдена: " + id));

        if (articleLikeRepository.existsByArticleAndUser(article, user)) {
            throw new EntityExistsException("Вы уже лайкнули эту статью");
        }

        articleLikeRepository.save(
                ArticleLike
                        .builder()
                        .user(user)
                        .article(article)
                        .build());
    }

    /**
     * Убрать лайк со статьи
     */
    @Transactional
    @CacheEvict(cacheNames = "wiki-article", key = "#id")
    public void unlikeArticle(Long id, User user) {
        WikiArticle article = wikiArticleRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Статья не найдена: " + id));

        if (!articleLikeRepository.existsByArticleAndUser(article, user)) {
            throw new EntityNotFoundException("Вы не лайкали эту статью");
        }

        articleLikeRepository.deleteByArticleAndUser(article, user);
    }

    // === Private helpers ===

    private boolean canModify(WikiArticle article, Long userId) {
        // Автор или специалист/админ
        if (article.getCreatedBy().getId().equals(userId)) {
            if (article.getCreatedBy().getRoles().contains(SenderType.ADMIN.name())) {
                return true;
            }
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

    /**
     * Построить полный ответ со статьей
     */
    private WikiArticleResponse buildArticleResponse(WikiArticle article, Set<String> tagSet, long likeCount, boolean likedByCurrentUser) {
        return new WikiArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getContent(),
                article.getExcerpt(),
                article.getCategory() != null ? article.getCategory().getId() : null,
                article.getCategory() != null ? article.getCategory().getName() : null,
                tagSet,
                article.getCreatedBy() != null ? wikiArticleMapper.toUserShortResponse(article.getCreatedBy()) : null,
                article.getUpdatedBy() != null ? wikiArticleMapper.toUserShortResponse(article.getUpdatedBy()) : null,
                article.getViewsTotal(),
                likeCount,
                likedByCurrentUser,
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    /**
     * Построить краткий ответ для списка
     */
    private WikiArticleListResponse buildListResponse(WikiArticle article, Set<String> tags, Long userId) {
        long likeCount = articleLikeRepository.countByArticleId(article.getId());
        boolean likedByCurrentUser = userId != null &&
                articleLikeRepository.existsByArticleIdAndUserId(article.getId(), userId);

        return new WikiArticleListResponse(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getExcerpt(),
                article.getCategory() != null ? article.getCategory().getName() : null,
                tags,
                article.getCreatedBy() != null
                        ? (article.getCreatedBy().getFio() != null ? article.getCreatedBy().getFio()
                        : article.getCreatedBy().getUsername())
                        : null,
                article.getViewsTotal(),
                likeCount,
                likedByCurrentUser,
                article.getUpdatedAt());
    }

    private Map<Long, Set<String>> articleTagFetcher(Page<WikiArticle> articles) {

        if (articles.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = articles.stream()
                .map(WikiArticle::getId)
                .toList();
        return
                wikiArticleRepository.findTagsByArticleIds(ids).stream()
                        .collect(Collectors.groupingBy(
                                row -> (Long) row[0],
                                Collectors.mapping(
                                        row -> (String) row[1],
                                        Collectors.toSet()
                                )
                        ));
    }

    private void incrementViews(User user, WikiArticle article) {
        // Инкремент просмотров только если пользователь ещё не смотрел статью
        if (!wikiArticleViewRepository.existsByArticleAndUser(article, user)) {
            WikiArticleView view = WikiArticleView.builder()
                    .id(new WikiArticleViewId(article.getId(), user.getId()))
                    .article(article)
                    .user(user)
                    .build();
            wikiArticleViewRepository.save(view);

            // Инкремент агрегированного поля
            wikiArticleViewRepository.incrementViewsTotal(article.getId());
        }
    }

}
