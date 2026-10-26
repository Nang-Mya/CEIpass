package com.chiangraipass.config;

import java.io.IOException;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.auth.service.VisitorAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Ends a sign-in session once the account's security token has changed (for example after a
 * password reset) or the account is no longer active. The token is stored in the session
 * principal at sign-in and compared with the database on each API request.
 */
public class SessionSecurityTokenFilter extends OncePerRequestFilter {

    private final ObjectProvider<VisitorAccountService> accounts;

    public SessionSecurityTokenFilter(ObjectProvider<VisitorAccountService> accounts) {
        this.accounts = accounts;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof VisitorIdentity visitor
                && visitor.securityToken() != null
                && !visitor.securityToken().isEmpty()) {
            VisitorAccountService service = accounts.getIfAvailable();
            if (service != null) {
                boolean stillValid = service.findSecurityToken(visitor.accountId())
                        .map(visitor.securityToken()::equals)
                        .orElse(false);
                if (!stillValid) {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    SecurityContextHolder.clearContext();
                }
            }
        }
        chain.doFilter(request, response);
    }
}