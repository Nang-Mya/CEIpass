package com.chiangraipass.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limits how often one client IP may call sensitive authentication endpoints. */
public class RateLimitFilter extends OncePerRequestFilter {

    public record Rule(String method, String path, int maxRequests, Duration window) {
    }

    private record ActiveRule(Rule rule, SlidingWindowRateLimiter limiter) {
    }

    private final ObjectMapper mapper;
    private final List<ActiveRule> rules;

    public RateLimitFilter(ObjectMapper mapper, List<Rule> rules, Clock clock) {
        this.mapper = mapper;
        this.rules = rules.stream()
                .map(rule -> new ActiveRule(
                        rule, new SlidingWindowRateLimiter(rule.maxRequests(), rule.window(), clock)))
                .toList();
    }

    public static List<Rule> defaultRules() {
        return List.of(
                new Rule("POST", "/api/v1/auth/login", 20, Duration.ofMinutes(10)),
                new Rule("POST", "/api/v1/auth/register", 5, Duration.ofHours(1)),
                new Rule("POST", "/api/v1/auth/oauth/prepare", 20, Duration.ofMinutes(10)),
                new Rule("POST", "/api/v1/auth/password/forgot", 5, Duration.ofHours(1)),
                new Rule("POST", "/api/v1/auth/password/verify-otp", 10, Duration.ofMinutes(10)),
                new Rule("POST", "/api/v1/auth/password/reset", 20, Duration.ofMinutes(10)),
                new Rule("POST", "/api/v1/me/privacy-preferences", 20, Duration.ofMinutes(10))
        );
    }

    @Override
    protected void doFilterInternal(
            @org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain chain)
            throws ServletException, IOException {
        for (ActiveRule active : rules) {
            if (matches(active.rule(), request)) {
                SlidingWindowRateLimiter.Decision decision =
                        active.limiter().tryAcquire(request.getRemoteAddr());
                if (!decision.allowed()) {
                    reject(response, decision.retryAfterSeconds());
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean matches(Rule rule, HttpServletRequest request) {
        // getServletPath() excludes path parameters such as ";jsessionid=...".
        return rule.method().equalsIgnoreCase(request.getMethod())
                && rule.path().equals(request.getServletPath());
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("Cache-Control", "no-store");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
                Map.of("message", "Too many requests. Try again later."));
    }
}