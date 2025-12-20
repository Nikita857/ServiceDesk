package com.bm.wschat.feature.user.repository;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityStatusRepository extends JpaRepository<UserActivityStatusEntity, Long> {
    Optional<UserActivityStatus> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    Optional<UserActivityStatusEntity> findByUser(User user);
}
