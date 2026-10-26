package com.chiangraipass.config;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.auth.model.VisitorAccount;
import com.chiangraipass.auth.service.VisitorAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class SessionSecurityTokenFilterTest {

    private final VisitorAccountService accounts = mock(VisitorAccountService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<VisitorAccountService> provider = mock(ObjectProvider.class);

    private final SessionSecurityTokenFilter filter = new SessionSecurityTokenFilter(provider);
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        given(provider.getIfAvailable()).willReturn(accounts);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void signIn(String securityToken) {
        VisitorAccount account =
                new VisitorAccount(id, "a@example.com", "A", "hash", true, null, securityToken);
        EmailVisitorPrincipal principal = new EmailVisitorPrincipal(account);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private boolean run(String path, MockHttpSession session) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setSession(session);
        AtomicBoolean passedThrough = new AtomicBoolean();
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> passedThrough.set(true));
        return passedThrough.get();
    }

    @Test
    void keepsTheSessionWhileTheTokenMatches() throws Exception {
        signIn("t1");
        given(accounts.findSecurityToken(id)).willReturn(Optional.of("t1"));
        MockHttpSession session = new MockHttpSession();

        assertThat(run("/api/v1/auth/me", session)).isTrue();

        assertThat(session.isInvalid()).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void endsTheSessionAfterTheTokenChanges() throws Exception {
        signIn("t1");
        given(accounts.findSecurityToken(id)).willReturn(Optional.of("t2"));
        MockHttpSession session = new MockHttpSession();

        assertThat(run("/api/v1/auth/me", session)).isTrue();

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void endsTheSessionWhenTheAccountIsNoLongerActive() throws Exception {
        signIn("t1");
        given(accounts.findSecurityToken(id)).willReturn(Optional.empty());
        MockHttpSession session = new MockHttpSession();

        run("/api/v1/me/qr", session);

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void ignoresRequestsOutsideTheApi() throws Exception {
        signIn("t1");
        MockHttpSession session = new MockHttpSession();

        assertThat(run("/FrontEnd/js/app.js", session)).isTrue();

        verifyNoInteractions(accounts);
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void ignoresSignInsWithoutATokenAndAnonymousRequests() throws Exception {
        signIn(null);
        run("/api/v1/auth/me", new MockHttpSession());
        SecurityContextHolder.clearContext();
        run("/api/v1/auth/me", new MockHttpSession());

        verifyNoInteractions(accounts);
    }
}