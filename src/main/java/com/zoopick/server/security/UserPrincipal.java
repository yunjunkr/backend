package com.zoopick.server.security;

import com.zoopick.server.entity.Role;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.AuthenticatedPrincipal;

@NullMarked
public record UserPrincipal(Long id, String email, String nickname, Role role) implements AuthenticatedPrincipal {
    @Override
    public String getName() {
        return email;
    }
}
