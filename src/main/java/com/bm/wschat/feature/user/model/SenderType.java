package com.bm.wschat.feature.user.model;

import org.springframework.security.core.GrantedAuthority;

public enum SenderType implements GrantedAuthority {
    USER,
    SYSADMIN,
    DEV1C,
    DEVELOPER,
    ADMIN;

    @Override
    public String getAuthority() { return name();}
}
