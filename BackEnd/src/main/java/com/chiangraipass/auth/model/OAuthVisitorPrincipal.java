package com.chiangraipass.auth.model;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

public class OAuthVisitorPrincipal implements OidcUser, VisitorIdentity {

    private final VisitorAccount account;
    private final OidcUser delegate;

    public OAuthVisitorPrincipal(VisitorAccount account, OidcUser delegate) {
        this.account = account;
        this.delegate = delegate;
    }

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
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return account.accountId().toString();
    }
}
