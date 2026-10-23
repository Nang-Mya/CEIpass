package com.chiangraipass.config;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private MutableClock clock;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        filter = new RateLimitFilter(new ObjectMapper(), List.of(
                new RateLimitFilter.Rule("POST", "/api/v1/auth/login", 2, Duration.ofMinutes(10)),
                new RateLimitFilter.Rule("POST", "/api/v1/auth/register", 1, Duration.ofHours(1))), clock);
    }

    private record Result(MockHttpServletResponse response, boolean passedThrough) {
    }

    private Result call(String method, String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean passed = new AtomicBoolean();
        filter.doFilter(request, response, (req, res) -> passed.set(true));
        return new Result(response, passed.get());
    }

    @Test
    void requestsUnderTheLimitPassThrough() throws Exception {
        assertThat(call("POST", "/api/v1/auth/login", "1.1.1.1").passedThrough()).isTrue();
        assertThat(call("POST", "/api/v1/auth/login", "1.1.1.1").passedThrough()).isTrue();
    }

    @Test
    void requestOverTheLimitGets429WithRetryAfterAndJsonBody() throws Exception {
        call("POST", "/api/v1/auth/login", "1.1.1.1");
        call("POST", "/api/v1/auth/login", "1.1.1.1");

        Result blocked = call("POST", "/api/v1/auth/login", "1.1.1.1");

        assertThat(blocked.passedThrough()).isFalse();
        assertThat(blocked.response().getStatus()).isEqualTo(429);
        assertThat(blocked.response().getHeader("Retry-After")).isEqualTo("600");
        assertThat(blocked.response().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(blocked.response().getContentAsString()).contains("Too many requests");
    }

    @Test
    void eachClientIpHasItsOwnLimit() throws Exception {
        call("POST", "/api/v1/auth/login", "1.1.1.1");
        call("POST", "/api/v1/auth/login", "1.1.1.1");

        assertThat(call("POST", "/api/v1/auth/login", "2.2.2.2").passedThrough()).isTrue();
    }

    @Test
    void rulesAreIndependent() throws Exception {
        call("POST", "/api/v1/auth/register", "1.1.1.1");

        assertThat(call("POST", "/api/v1/auth/register", "1.1.1.1").passedThrough()).isFalse();
        assertThat(call("POST", "/api/v1/auth/login", "1.1.1.1").passedThrough()).isTrue();
    }

    @Test
    void otherMethodsAndPathsAreNotLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(call("GET", "/api/v1/auth/login", "1.1.1.1").passedThrough()).isTrue();
            assertThat(call("GET", "/api/v1/catalog/passes", "1.1.1.1").passedThrough()).isTrue();
        }
    }

    @Test
    void requestsAreAllowedAgainAfterTheWindow() throws Exception {
        call("POST", "/api/v1/auth/login", "1.1.1.1");
        call("POST", "/api/v1/auth/login", "1.1.1.1");
        assertThat(call("POST", "/api/v1/auth/login", "1.1.1.1").passedThrough()).isFalse();

        clock.advance(Duration.ofMinutes(10));

        assertThat(call("POST", "/api/v1/auth/login", "1.1.1.1").passedThrough()).isTrue();
    }
}