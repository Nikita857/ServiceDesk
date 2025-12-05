package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.mapper.assignment.AssignmentMapper;
import com.bm.wschat.feature.ticket.model.Assignment;
import com.bm.wschat.feature.ticket.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentMapper assignmentMapper;

    public AssignmentResponse assignTicket(AssignmentCreateRequest request) {
        Assignment saved = assignmentRepository.save(assignmentMapper.toEntity(request));
        return assignmentMapper.toResponse(saved);
    }
}
