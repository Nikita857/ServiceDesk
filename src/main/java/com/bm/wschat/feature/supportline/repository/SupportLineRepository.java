package com.bm.wschat.feature.supportline.repository;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportLineRepository extends JpaRepository<SupportLine, Long> {

    Optional<SupportLine> findByName(String name);

    List<SupportLine> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT sl FROM SupportLine sl LEFT JOIN FETCH sl.specialists WHERE sl.id = :id")
    Optional<SupportLine> findByIdWithSpecialists(@Param("id") Long id);

    @Query("SELECT sl FROM SupportLine sl WHERE :specialist MEMBER OF sl.specialists")
    List<SupportLine> findBySpecialist(@Param("specialist") User specialist);
}
