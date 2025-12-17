package com.bm.wschat.feature.user.repository;

import com.bm.wschat.feature.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") String role);

    /**
     * Поиск пользователей по ФИО или username (без учёта регистра)
     */
    @Query("SELECT u FROM User u WHERE u.id <> :excludeUserId AND " +
            "(LOWER(u.fio) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchByFioOrUsername(
            @Param("query") String query,
            @Param("excludeUserId") Long excludeUserId,
            Pageable pageable);
}
