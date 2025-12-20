package com.bm.wschat.feature.supportline.dto.response;

import com.bm.wschat.feature.user.model.UserActivityStatus;

/**
 * DTO для специалиста линии поддержки.
 *
 * @param id                     ID пользователя
 * @param username               логин
 * @param fio                    ФИО
 * @param active                 активен ли аккаунт
 * @param activityStatus         текущий статус активности (AVAILABLE, BUSY,
 *                               UNAVAILABLE и т.д.)
 * @param availableForAssignment доступен ли для назначения тикетов
 */
public record SpecialistResponse(
                Long id,
                String username,
                String fio,
                boolean active,
                UserActivityStatus activityStatus,
                boolean availableForAssignment) {
}
