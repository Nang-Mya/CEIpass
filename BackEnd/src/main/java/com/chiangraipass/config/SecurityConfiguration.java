package com.chiangraipass.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.chiangraipass.auth.model.VisitorIdentity;
import com.chiangraipass.auth.service.VisitorOAuth2UserService;
import com.chiangraipass.auth.service.LoginAttemptService;
import com.chiangraipass.auth.service.VisitorUserDetailsService;
import com.chiangraipass.auth.model.EmailVisitorPrincipal;
import com.chiangraipass.config.AuthProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.authentication.LockedException;


@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    DaoAuthenticationProvider visitorAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPostAuthenticationChecks(user -> {
            if (!user.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException("User credentials have expired.");
            }
            if (user instanceof EmailVisitorPrincipal visitor && !visitor.account().emailVerified()) {
                throw new DisabledException("Email address is not verified.");
            }
        });
        return provider;
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            CsrfTokenRepository csrfTokenRepository,
            ObjectMapper objectMapper,
            AuthProperties authProperties,
            VisitorOAuth2UserService oauthUserService,
            ObjectProvider<ClientRegistrationRepository> registrations) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/catalog/**",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/config",
                                "/api/v1/auth/register",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/oauth/prepare",
                                "/api/v1/auth/login",
                                "/oauth2/**",
                                "/login/oauth2/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/me/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                apiUnauthorized(objectMapper),
                                new AntPathRequestMatcher("/api/**")))
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(jsonLoginSuccess(objectMapper))
                        .failureHandler(jsonLoginFailure(objectMapper)))
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
        http.addFilterBefore(
                new RateLimitFilter(objectMapper, RateLimitFilter.defaultRules(), Clock.systemUTC()),
                CsrfFilter.class);

        if (registrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oauthUserService))
                    .successHandler(oauthSuccess(authProperties))
                    .failureHandler(oauthFailure(authProperties)));
            http.addFilterBefore(new OAuthPrivacyNoticeFilter(), OAuth2LoginAuthenticationFilter.class);
        }
        return http.build();
        }

    private AuthenticationEntryPoint apiUnauthorized(ObjectMapper mapper) {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            mapper.writeValue(response.getOutputStream(), Map.of("message", "Sign in is required."));
        };
    }

    private AuthenticationSuccessHandler jsonLoginSuccess(ObjectMapper mapper) {
        return (request, response, authentication) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            mapper.writeValue(response.getOutputStream(), accountResponse(authentication));
        };
    }

    private AuthenticationFailureHandler jsonLoginFailure(ObjectMapper mapper) {
        return (request, response, exception) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            int status;
            if (exception instanceof LockedException) {
                // The account locks when the 5th wrong password is recorded; from the next attempt
                // Spring Security rejects it with LockedException before checking the password.
                status = HttpStatus.LOCKED.value();
                body.put("message", "Too many failed attempts. The account is locked for "
                        + LoginAttemptService.LOCK_DURATION.toMinutes() + " minutes. Try again later.");
            } else if (exception instanceof DisabledException) {
                status = HttpStatus.FORBIDDEN.value();
                body.put("code", "EMAIL_NOT_VERIFIED");
                body.put("message", "Please verify your email address before signing in. "
                        + "Open the link we emailed you, or request a new one.");
            } else {
                status = HttpServletResponse.SC_UNAUTHORIZED;
                body.put("message", "Invalid email or password.");
            }
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Cache-Control", "no-store");
            mapper.writeValue(response.getOutputStream(), body);
        };
    }

    private AuthenticationSuccessHandler oauthSuccess(AuthProperties properties) {
        return (request, response, authentication) -> response.sendRedirect(
                frontendUrl(properties) + "/#profile?auth=success");
    }

    private AuthenticationFailureHandler oauthFailure(AuthProperties properties) {
        return (request, response, exception) -> response.sendRedirect(
                frontendUrl(properties) + "/#profile?auth=failed");
    }

    private String frontendUrl(AuthProperties properties) {
        return properties.auth().frontendUrl().replaceAll("/+$", "");
    }

    private Map<String, Object> accountResponse(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof VisitorIdentity visitor)) {
            throw new IllegalStateException("Authenticated principal is not a visitor account.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountId", visitor.accountId());
        result.put("email", visitor.email());
        result.put("displayName", visitor.displayName());
        return result;
    }
}
