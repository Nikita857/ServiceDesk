package com.bm.wschat.feature.ticket.repository;

import com.bm.wschat.feature.ticket.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

}
