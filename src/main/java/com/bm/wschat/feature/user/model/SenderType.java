package com.bm.wschat.feature.user.model;

import org.springframework.security.core.GrantedAuthority;

public enum SenderType implements GrantedAuthority {
    USER,
    SPECIALIST,
    DEVELOPER,
    ADMIN,
    SYSTEM;

    @Override
    public String getAuthority() { return name();}
}
