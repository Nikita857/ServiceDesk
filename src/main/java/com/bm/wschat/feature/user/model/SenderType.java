package com.bm.wschat.feature.user.model;

import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public enum SenderType implements GrantedAuthority {
    USER,
    SYSADMIN,
    DEV1C,
    DEVELOPER,
    ADMIN;

    @Override
    public String getAuthority() {
        return name();
    }

    public static SenderType findMainRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Set ролей не может быть пустым");
        }

        for (SenderType type : SenderType.values()) {
            if (roles.contains(type.name())) {
                return type;
            }
        }

        // Если ни одна известная роль не найдена
        throw new IllegalArgumentException("No valid SenderType found in roles: " + roles);
    }

    public static SenderType toSenderType(String senderType) {
        if (senderType == null) {
            throw new IllegalArgumentException("senderType cannot be null");
        }

        return switch (senderType) {
            case "USER" -> SenderType.USER;
            case "SYSADMIN" -> SenderType.SYSADMIN;
            case "DEV1C" -> SenderType.DEV1C;
            case "DEVELOPER" -> SenderType.DEVELOPER;
            case "ADMIN" -> SenderType.ADMIN;
            default -> throw new IllegalArgumentException("Unknown senderType: " + senderType);
        };
    }
}