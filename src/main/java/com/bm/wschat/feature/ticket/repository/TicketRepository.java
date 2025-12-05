package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByCreatedById(Long userId, Pageable pageable);

    Page<Ticket> findByAssignedToId(Long specialistId, Pageable pageable);

    Page<Ticket> findBySupportLineId(Long lineId, Pageable pageable);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.createdBy " +
            "LEFT JOIN FETCH t.assignedTo " +
            "LEFT JOIN FETCH t.supportLine " +
            "LEFT JOIN FETCH t.categoryUser " +
            "LEFT JOIN FETCH t.categorySupport " +
            "WHERE t.id = :id")
    Optional<Ticket> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countByStatus();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedTo.id = :specialistId AND t.status NOT IN ('CLOSED', 'RESOLVED')")
    Long countActiveByAssignedTo(@Param("specialistId") Long specialistId);
}
