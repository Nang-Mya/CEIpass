package com.chiangraipass.auth.model;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record EmailVisitorPrincipal(VisitorAccount account) implements UserDetails, VisitorIdentity {

    @Override
    public UUID accountId() {
        return account.accountId();
    }

    @Override
    public String email() {
        return account.email();
    }

    @Override
    public String displayName() {
        return account.displayName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_VISITOR"));
    }

    @Override
    public String getPassword() {
        return account.passwordHash();
    }

    @Override
    public String getUsername() {
        return account.email();
    }
}
