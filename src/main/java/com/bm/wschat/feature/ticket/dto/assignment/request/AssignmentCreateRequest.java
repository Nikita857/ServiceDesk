package com.bm.wschat.feature.ticket.dto.assignment.request;

import com.bm.wschat.feature.ticket.model.AssignmentMode;

// AssignmentCreateRequest.java — для создания назначения (POST)
public record AssignmentCreateRequest(
        Long ticketId,

        // Куда назначаем
        Long toLineId,     // можно назначить на линию
        Long toUserId,     // или сразу на человека (если DIRECT)

        // Откуда (опционально)
        Long fromLineId,
        Long fromUserId,

        String note,
        AssignmentMode mode // по умолчанию можно не передавать
) {}
