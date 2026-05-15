package com.zoopick.server.entity;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@NullMarked
public enum Role {
    STUDENT,
    ADMIN(STUDENT);

    public static final String PREFIX = "ROLE_";

    private final Collection<GrantedAuthority> authorities;

    Role(Role... childRoles) {
        authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority(getRoleName()));
        for (Role childRole : childRoles)
            authorities.add(new SimpleGrantedAuthority(childRole.getRoleName()));
    }

    public String getRoleName() {
        return PREFIX + this.name();
    }

    public Collection<GrantedAuthority> getGrantedAuthority() {
        return Set.copyOf(authorities);
    }
}
