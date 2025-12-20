package com.bm.wschat.feature.user.repository;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityStatusRepository extends JpaRepository<UserActivityStatusEntity, Long> {

    /**
     * Найти статус по ID пользователя
     */
    Optional<UserActivityStatusEntity> findByUserId(Long userId);

    /**
     * Проверить существование записи статуса
     */
    boolean existsByUserId(Long userId);

    /**
     * Найти статус по сущности пользователя
     */
    Optional<UserActivityStatusEntity> findByUser(User user);
}
