package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.ticket.model.Assignment;
import com.bm.wschat.feature.ticket.model.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // История назначений тикета
    List<Assignment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    Page<Assignment> findByTicketIdOrderByCreatedAtDesc(Long ticketId, Pageable pageable);

    // Назначения пользователю по статусу
    List<Assignment> findByToUserIdAndStatusOrderByCreatedAtDesc(Long userId, AssignmentStatus status);

    Page<Assignment> findByToUserIdAndStatusOrderByCreatedAtDesc(Long userId, AssignmentStatus status,
            Pageable pageable);

    // Назначения на линию по статусу
    List<Assignment> findByToLineIdAndStatusOrderByCreatedAtDesc(Long lineId, AssignmentStatus status);

    // Текущее (последнее принятое) назначение тикета
    @Query("SELECT a FROM Assignment a WHERE a.ticket.id = :ticketId AND a.status = 'ACCEPTED' " +
            "ORDER BY a.acceptedAt DESC LIMIT 1")
    Optional<Assignment> findCurrentByTicketId(@Param("ticketId") Long ticketId);

    // Последнее назначение тикета (любого статуса)
    @Query("SELECT a FROM Assignment a WHERE a.ticket.id = :ticketId ORDER BY a.createdAt DESC LIMIT 1")
    Optional<Assignment> findLatestByTicketId(@Param("ticketId") Long ticketId);

    // Количество ожидающих назначений у пользователя
    Long countByToUserIdAndStatus(Long userId, AssignmentStatus status);

    // Количество активных (принятых) назначений у пользователя
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.toUser.id = :userId AND a.status = 'ACCEPTED'")
    Long countActiveByUserId(@Param("userId") Long userId);

    // Проверка есть ли ожидающее назначение на тикет
    boolean existsByTicketIdAndStatus(Long ticketId, AssignmentStatus status);

    // Назначение с деталями
    @Query("SELECT a FROM Assignment a " +
            "LEFT JOIN FETCH a.ticket " +
            "LEFT JOIN FETCH a.fromLine " +
            "LEFT JOIN FETCH a.fromUser " +
            "LEFT JOIN FETCH a.toLine " +
            "LEFT JOIN FETCH a.toUser " +
            "WHERE a.id = :id")
    Optional<Assignment> findByIdWithDetails(@Param("id") Long id);
}
