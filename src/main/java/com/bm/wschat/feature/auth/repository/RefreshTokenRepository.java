package com.bm.wschat.feature.auth.repository;

import com.bm.wschat.feature.auth.model.RefreshToken;
import com.bm.wschat.feature.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByUser(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByToken(String token);
}
