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

## Sending password-reset emails on Cloud Run

The application already sends a six-digit password-reset code to the account's email address. It does not send registration confirmations or messages to a team inbox. By default, `MAIL_MODE` is `log`, so messages are written to application logs instead of being delivered. To send real email:

1. Choose an SMTP or transactional-email provider and verify the sender address/domain with that provider. Use its SMTP host, username, and password; do not use your normal mailbox password. For Gmail, use an App Password and note Gmail's sending limits.
2. In Google Cloud Secret Manager, create a secret containing the SMTP password (for example, `SMTP_PASSWORD`).
3. Ensure the Cloud Run service's runtime service account has the **Secret Manager Secret Accessor** role for that secret.
4. In the Cloud Run service configuration, set these environment variables:

   | Variable | Example / value |
   | --- | --- |
   | `MAIL_MODE` | `smtp` |
   | `MAIL_FROM` | Your provider-verified sender address |
   | `SPRING_MAIL_HOST` | SMTP host from your provider |
   | `SPRING_MAIL_PORT` | `587` |
   | `SPRING_MAIL_USERNAME` | SMTP username from your provider |
   | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | `true` |
   | `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | `true` |
   | `SPRING_MAIL_PASSWORD` | Reference the `SMTP_PASSWORD` Secret Manager secret; do not enter the password as a plain environment variable |

   These are standard Spring Boot mail settings, so no source-code or dependency change is needed. For example, with the `gcloud` CLI, update a service after creating the secret and granting access:

   ```powershell
   gcloud run services update SERVICE_NAME --region REGION `
     --update-env-vars "MAIL_MODE=smtp,MAIL_FROM=verified@example.com,SPRING_MAIL_HOST=smtp.provider.example,SPRING_MAIL_PORT=587,SPRING_MAIL_USERNAME=smtp-user,SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true,SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true" `
     --update-secrets "SPRING_MAIL_PASSWORD=SMTP_PASSWORD:latest"
   ```

5. Send a password-reset request for an existing email/password account, then check that inbox (including spam) and the Cloud Run logs. If delivery fails, logs contain `Could not send a password reset code`; check the SMTP host/port, secret access, provider credentials, and sender verification. Cloud Run blocks outbound SMTP on port 25; use the provider's authenticated TLS submission port, typically 587.

## Implemented API

- `GET /api/v1/auth/config` reports whether registration is enabled and which providers are configured.
- `GET /api/v1/auth/csrf` issues a CSRF token.
- `POST /api/v1/auth/register` creates an account after validating the fields and notice acknowledgement.
- `POST /api/v1/auth/login` accepts form-encoded email/password and establishes a session.
- `GET /api/v1/auth/me` returns the current account, or `401` without an authenticated session.
- `POST /api/v1/auth/logout` invalidates the current session.
- `POST /api/v1/auth/oauth/prepare` validates provider setup, records the notice acknowledgement for a new OAuth account, and returns its authorization URL.
- OAuth callbacks use Spring Security's state validation and OIDC token/signature validation; only verified provider emails are accepted.


## Before production

This is a functional authentication foundation, not a production identity program. Email verification, password reset/recovery, account lockout or distributed rate limiting, monitoring/abuse response, and account linking are not implemented. Add and test these controls before enabling public registration or connecting payment/pass issuance. Configure HTTPS and `SESSION_COOKIE_SECURE=true`; ensure the reverse proxy forwards only trusted headers. Review privacy and retention obligations with the responsible privacy/legal team.
