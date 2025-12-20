package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.feature.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Сервис правил переадресации тикетов между линиями поддержки.
 * Схема переадресации:
 * 
 * USER → SYSADMIN, 1CSUPPORT (выбор при создании тикета)
 * SYSADMIN → 1CSUPPORT
 * 1CSUPPORT → SYSADMIN, DEV1C
 * DEV1C → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
 * DEVELOPER → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
 * ADMIN → любая линия
 */
@Service
@RequiredArgsConstructor
public class ForwardingRulesService {

    private final SupportLineRepository supportLineRepository;

    /**
     * Маппинг роли на разрешённые целевые роли для переадресации.
     * Ключ — роль текущего специалиста, значение — список ролей, куда можно
     * переадресовать.
     */
    private static final Map<SenderType, Set<SenderType>> FORWARDING_RULES = Map.of(
            SenderType.USER, Set.of(SenderType.SYSADMIN, SenderType.ONE_C_SUPPORT),
            SenderType.SYSADMIN, Set.of(SenderType.ONE_C_SUPPORT),
            SenderType.ONE_C_SUPPORT, Set.of(SenderType.SYSADMIN, SenderType.DEV1C),
            SenderType.DEV1C,
            Set.of(SenderType.SYSADMIN, SenderType.ONE_C_SUPPORT, SenderType.DEV1C, SenderType.DEVELOPER),
            SenderType.DEVELOPER,
            Set.of(SenderType.SYSADMIN, SenderType.ONE_C_SUPPORT, SenderType.DEV1C, SenderType.DEVELOPER)
    // ADMIN не указан — имеет полный доступ
    );

    /**
     * Маппинг названия линии поддержки на соответствующую роль.
     */
    private static final Map<String, SenderType> LINE_TO_ROLE = Map.of(
            "Первая линия (SYSADMIN)", SenderType.SYSADMIN,
            "Поддержка 1С (1CSUPPORT)", SenderType.ONE_C_SUPPORT,
            "Линия 1С (DEV1C)", SenderType.DEV1C,
            "Линия разработчиков (DEVELOPER)", SenderType.DEVELOPER);

    /**
     * Проверяет, может ли пользователь переадресовать тикет на указанную линию.
     *
     * @param user   пользователь, выполняющий переадресацию
     * @param toLine целевая линия поддержки
     * @return true если переадресация разрешена
     */
    public boolean canForwardTo(User user, SupportLine toLine) {
        // Админ может всё
        if (user.isAdmin()) {
            return true;
        }

        SenderType userRole = getUserMainRole(user);
        SenderType targetRole = getLineRole(toLine);

        if (targetRole == null) {
            // Неизвестная линия — запрещаем
            return false;
        }

        Set<SenderType> allowedTargets = FORWARDING_RULES.get(userRole);
        if (allowedTargets == null) {
            // Роль не в правилах — запрещаем
            return false;
        }

        return allowedTargets.contains(targetRole);
    }

    /**
     * Возвращает список линий, на которые пользователь может переадресовать тикет.
     *
     * @param user пользователь
     * @return список доступных линий для переадресации
     */
    public List<SupportLine> getAvailableForwardingLines(User user) {
        // Админ может на любую линию
        if (user.isAdmin()) {
            return supportLineRepository.findAllByOrderByDisplayOrderAsc();
        }

        SenderType userRole = getUserMainRole(user);
        Set<SenderType> allowedTargets = FORWARDING_RULES.get(userRole);

        if (allowedTargets == null || allowedTargets.isEmpty()) {
            return List.of();
        }

        // Фильтруем линии по разрешённым ролям
        List<SupportLine> allLines = supportLineRepository.findAllByOrderByDisplayOrderAsc();
        List<SupportLine> result = new ArrayList<>();

        for (SupportLine line : allLines) {
            SenderType lineRole = getLineRole(line);
            if (lineRole != null && allowedTargets.contains(lineRole)) {
                result.add(line);
            }
        }

        return result;
    }

    /**
     * Валидирует переадресацию и выбрасывает исключение если запрещена.
     *
     * @param user     пользователь
     * @param fromLine исходная линия
     * @param toLine   целевая линия
     * @throws IllegalArgumentException если переадресация запрещена
     */
    public void validateForwarding(User user, SupportLine fromLine, SupportLine toLine) {
        if (user.isAdmin()) {
            return; // Админ может всё
        }

        if (!canForwardTo(user, toLine)) {
            SenderType userRole = getUserMainRole(user);
            SenderType targetRole = getLineRole(toLine);
            Set<SenderType> allowed = FORWARDING_RULES.getOrDefault(userRole, Set.of());

            throw new IllegalArgumentException(
                    String.format("Роль %s не может переадресовать тикет на линию '%s' (%s). " +
                            "Разрешённые направления: %s",
                            userRole, toLine.getName(), targetRole,
                            allowed.isEmpty() ? "нет" : allowed));
        }
    }

    /**
     * Получить главную роль пользователя.
     */
    private SenderType getUserMainRole(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return SenderType.USER;
        }
        try {
            return SenderType.findMainRole(user.getRoles());
        } catch (IllegalArgumentException e) {
            return SenderType.USER;
        }
    }

    /**
     * Получить роль, ассоциированную с линией поддержки.
     */
    private SenderType getLineRole(SupportLine line) {
        if (line == null || line.getName() == null) {
            return null;
        }
        return LINE_TO_ROLE.get(line.getName());
    }
}
