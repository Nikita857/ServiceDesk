package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepositry extends JpaRepository<Ticket, String> {
}
