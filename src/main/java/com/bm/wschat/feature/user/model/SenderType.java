package com.bm.wschat.feature.user.model;

import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

/**
 * Типы пользователей/ролей в системе.
 * Определяет уровень доступа и тип специалиста.
 * <p>
 * Схема переадресации тикетов:
 * 
 * <pre>
 * USER       → SYSADMIN, 1CSUPPORT
 * SYSADMIN   → 1CSUPPORT
 * 1CSUPPORT  → SYSADMIN, DEV1C
 * DEV1C      → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
 * DEVELOPER  → SYSADMIN, 1CSUPPORT, DEV1C, DEVELOPER
 * ADMIN      → любая линия
 * </pre>
 */
public enum SenderType implements GrantedAuthority {
    /** Обычный пользователь (создаёт тикеты) */
    USER,
    /** Системный администратор (1-я линия) */
    SYSADMIN,
    /** Специалист 1С - поддержка (4-я линия, выбирается пользователем) */
    // Важно: начинается с цифры, поэтому нужен специальный обработчик
    @SuppressWarnings("java:S115") // Имя с цифры - допустимо для бизнес-требования
    ONE_C_SUPPORT,
    /** Специалист 1С - консультант (2-я линия) */
    DEV1C,
    /** Разработчик (3-я линия) */
    DEVELOPER,
    /** Администратор системы */
    ADMIN;

    /** Алиас для совместимости с именем "1CSUPPORT" в БД и API */
    public static final String ONE_C_SUPPORT_ALIAS = "1CSUPPORT";

    @Override
    public String getAuthority() {
        // Для 1CSUPPORT возвращаем алиас для совместимости с Spring Security
        if (this == ONE_C_SUPPORT) {
            return ONE_C_SUPPORT_ALIAS;
        }
        return name();
    }

    /**
     * Найти главную роль из набора ролей.
     * Приоритет по порядку объявления в enum.
     */
    public static SenderType findMainRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Set ролей не может быть пустым");
        }

        for (SenderType type : SenderType.values()) {
            if (roles.contains(type.getAuthority())) {
                return type;
            }
        }

        throw new IllegalArgumentException("No valid SenderType found in roles: " + roles);
    }

    /**
     * Преобразовать строку в SenderType.
     */
    public static SenderType toSenderType(String senderType) {
        if (senderType == null) {
            throw new IllegalArgumentException("senderType cannot be null");
        }

        return switch (senderType) {
            case "USER" -> SenderType.USER;
            case "SYSADMIN" -> SenderType.SYSADMIN;
            case "1CSUPPORT", "ONE_C_SUPPORT" -> SenderType.ONE_C_SUPPORT;
            case "DEV1C" -> SenderType.DEV1C;
            case "DEVELOPER" -> SenderType.DEVELOPER;
            case "ADMIN" -> SenderType.ADMIN;
            default -> throw new IllegalArgumentException("Unknown senderType: " + senderType);
        };
    }

    /**
     * Проверяет, является ли роль специалистом (может обрабатывать тикеты).
     */
    public boolean isSpecialistRole() {
        return this == SYSADMIN || this == ONE_C_SUPPORT || this == DEV1C || this == DEVELOPER || this == ADMIN;
    }
}