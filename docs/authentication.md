# Authentication setup

The API supports email/password accounts and optional Google and Apple OpenID Connect sign-in. Authentication uses a server-side Spring Security session; the browser receives an HTTP-only `JSESSIONID` cookie. Passwords are stored as BCrypt hashes and unsafe requests require a CSRF token.

## Local database

The backend currently uses an in-memory H2 database by default. Spring Data JPA repositories persist accounts for the lifetime of the running process, and Hibernate generates/updates the schema (`spring.jpa.hibernate.ddl-auto: update`). Flyway is disabled and there are no checked-in backend schema migrations. Local accounts are lost when the API stops. Choose and configure a persistent production database and migration strategy before using this authentication service for production accounts.

## Registration and privacy notice

New-account registration is disabled by default. Enable it only after publishing and reviewing the real privacy notice:

- `ACCOUNT_REGISTRATION_ENABLED=true`
- `PRIVACY_NOTICE_VERSION=<published notice version>`
- `PRIVACY_NOTICE_URL=<HTTPS URL of the published notice>`

The API reports registration as available only when the explicit enable flag is true and both notice settings are valid. The registration form requires acknowledgement of that notice; it does not collect marketing consent. Existing accounts may still sign in while registration is disabled.

## Password rules

New passwords must have at least 8 characters (at most 72), at least 1 number, at least 1 special character, and only English letters, numbers and symbols. Spaces are allowed but do not count as special characters. The rules are enforced by `PasswordPolicy` on the server and mirrored in the browser for faster feedback; the server response lists every unmet rule.

- **Sign-in messages:** the first five wrong passwords (and any unknown email) return the same `Invalid email or password.` message. The fifth wrong password locks the account; from the next attempt the response (`423`) says it is locked for 10 minutes, even if the password is then correct. This reveals that the email has an account, but only after five failed attempts on it.
## Google

Create an OAuth 2.0 web client and configure:

- `OAUTH_ENABLED=true`
- `GOOGLE_CLIENT_ID=<web client ID>`
- `GOOGLE_CLIENT_SECRET=<web client secret>`
- Add `<API base URL>/login/oauth2/code/google` as an authorized redirect URI.

For local development, the callback is `http://localhost:8080/login/oauth2/code/google`. Production must use HTTPS and the exact public API origin.

## Apple

Create an Apple Services ID, associate it with the Sign in with Apple capability and domain, and configure:

- `OAUTH_ENABLED=true`
- `APPLE_CLIENT_ID=<Services ID>`
- `APPLE_CLIENT_SECRET=<short-lived client-secret JWT>`
- Add `<API base URL>/login/oauth2/code/apple` as the return URL in the Apple developer configuration.

Apple's client secret is a signed JWT with an expiry, not a permanent password. Generate and rotate it according to Apple's current requirements. Apple sign-in requires a public HTTPS callback/domain; ordinary HTTP localhost is not suitable.

At least one provider must have both its client ID and secret configured when `OAUTH_ENABLED=true`. The app can enable either provider or both. Provider subject IDs are stored separately from passwords. Accounts are not automatically linked by matching email; account linking needs a separately authenticated flow.

## Browser/API configuration

- Set `FRONTEND_URL` to the frontend origin, without a path, for OAuth completion redirects.
- Set `CORS_ALLOWED_ORIGINS` to the exact comma-separated frontend origins.
- Set `SESSION_COOKIE_SECURE=true` in HTTPS environments. Keep it false only for local HTTP development.
- Store all provider secrets in the deployment secret manager/environment, never in source files or frontend JavaScript.
- The frontend calls `GET /api/v1/auth/csrf` before registration, login, logout, and OAuth preparation. Keep the frontend and API on the same site (for example, `localhost` on different ports for local development) so the `SameSite=Lax` session cookie is sent.

## Implemented API

- `GET /api/v1/auth/config` reports whether registration is enabled and which providers are configured.
- `GET /api/v1/auth/csrf` issues a CSRF token.
- `POST /api/v1/auth/register` creates an account after validating the fields and notice acknowledgement.
- `POST /api/v1/auth/login` accepts form-encoded email/password and establishes a session.
- `GET /api/v1/auth/me` returns the current account, or `401` without an authenticated session.
- `POST /api/v1/auth/logout` invalidates the current session.
- `POST /api/v1/auth/oauth/prepare` validates provider setup, records the notice acknowledgement for a new OAuth account, and returns its authorization URL.
- OAuth callbacks use Spring Security's state validation and OIDC token/signature validation; only verified provider emails are accepted.

## Email verification

New email/password accounts must verify their email before they can sign in. Registration emails a single-use link (`/#verify?token=...`, valid 24 hours; only a hash of the token is stored). The page posts the token to `POST /api/v1/auth/verify-email`. A visitor who signs in with the correct password but an unverified email gets `403` with `code: EMAIL_NOT_VERIFIED` and a Resend button; a wrong password still returns the generic `401`, so the response never reveals verification status to someone who does not know the password. `POST /api/v1/auth/resend-verification` always answers the same way and sends at most one link per minute per account. Accounts created through Google/Apple are verified automatically.

Mail settings: `MAIL_MODE=log` (default) prints emails to the application log for local development; it exposes links, so production must set `MAIL_MODE=smtp` together with `MAIL_FROM` and `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`. Links point to `FRONTEND_URL`. Accounts that existed before this change must be backfilled with an `email_verified_at` value (or asked to verify) when the production database migration is written.

## Before production

This is a functional authentication foundation, not a production identity program. Email verification, password reset/recovery, account lockout or distributed rate limiting, monitoring/abuse response, and account linking are not implemented. Add and test these controls before enabling public registration or connecting payment/pass issuance. Configure HTTPS and `SESSION_COOKIE_SECURE=true`; ensure the reverse proxy forwards only trusted headers. Review privacy and retention obligations with the responsible privacy/legal team.
