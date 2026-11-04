# Visitor App Feature

## Scope and implementation status

This feature adds the five requested mobile-first screens and navigable bottom bar using the existing plain HTML/CSS/JavaScript frontend.

| Screen | Available in the UI now | Requires a backend before production |
|---|---|---|
| Home | Quick links to passes and QR; clearly marked sample partner-attraction cards. | Published partner catalog records, current pass eligibility, approved imagery, and accurate accessibility/location details. |
| Buy | Clearly marked sample passes, add/remove quantity, locally persisted demo cart, subtotal, and a safe checkout explanation. | Published pass records, server-owned cart/order, inventory/eligibility, payment-provider checkout, and order status. |
| My QR | Stable per-pass QR display; account-scoped offline caching; pass validity states. | Authentication and pass eligibility are implemented. Venue scanner validation and per-benefit usage recording remain unimplemented. |
| History | Loading, empty, and unavailable states; renders a validated history response. | Authenticated, paginated, server-owned usage records and retention/privacy rules. |
| Profile | Email/password account creation and session sign-in; optional Google/Apple OAuth; current account display and sign-out; published-notice acknowledgement before registration. | Provider credentials/callback setup, email verification and recovery, rate limiting, privacy rights, and authoritative consent-management APIs. |

The backend provides JPA-backed public catalog endpoints and server-side session authentication. By default it uses an in-memory H2 database with Hibernate schema generation; catalog data starts empty and unpublished, and no sample pass or partner is seeded or presented as real inventory. New account registration is disabled unless the operator enables it and configures a reviewed privacy notice/version. Google and Apple sign-in require provider credentials. Payment, redemption, and order flows are not implemented. The frontend still renders clearly labeled sample catalog listings and does not yet consume the catalog endpoints. Do not treat the browser's cart or cached QR data as authoritative.

## Navigation and usability

- The bottom navigation exposes Home, Buy, My QR, History, and Profile and identifies the active page.
- Home has quick access to buying and the pass QR, followed by sample partner attractions.
- The visitor UI uses responsive layouts, large touch controls, visible focus states, reduced-motion preferences, and safe-area spacing for mobile devices.
- Loading, empty, offline, and unconfigured API states should remain explicit as live integrations are added.
- The QR screen never reports a redemption as confirmed from local state.

## Offline model and duplicate-scan risk

The service worker caches only the visitor app shell and static assets. It does not cache API responses. Increment the app-shell cache version (`CACHE_NAME` in `BackEnd/src/main/resources/static/sw.js`) when deploying changed cached assets so returning installations refresh the shell. A QR credential returned by the authenticated QR endpoint is kept separately in IndexedDB, keyed by an opaque account identifier; the visitor can remove that cached record on this device.

The visitor app can display a saved QR while offline, but the partner scanner must be online to validate the pass and record a use. Displaying a cached QR does not mean a redemption was accepted. This repository does not yet implement the scanner or redemption API.

The web platform cannot reliably block screenshots. A screenshot may be copied to another device or shown later. Mitigations belong in credential design and scanner validation:

- Use a cryptographically unpredictable, stable, per-pass credential. Never encode personal information or payment secrets in the QR.
- Display the QR payload issued by the backend; do not rotate it on refresh or impose a rolling QR expiry. Pass status and benefit quotas still govern acceptance.
- Validate pass eligibility and the requested benefit's remaining quota at the scanner.
- Record each permitted use atomically and idempotently; scanning does not consume or invalidate the pass's QR credential.
- Require connectivity for scanner validation until an offline redemption design is explicitly approved and implemented.

## Frontend API contract expected

Catalog endpoints below are implemented. Other endpoints remain proposed contracts. Keep the UI's sample data behind a repository/client module when transitioning to live API responses.

### Public catalog

- `GET /api/v1/catalog/passes` returns published pass IDs, display names, descriptions, decimal `price` (`BigDecimal` in Java, serialized as a JSON number), currency, validity description, and availability. Unpublished passes are not returned; a published but temporarily unavailable pass is returned with `available: false`.
- `GET /api/v1/catalog/attractions` returns published partner ID, public name, description, location, category, approved image URL, accessibility information, and current pass eligibility.
- The API response is authoritative for price and availability; never accept client-supplied totals as payment amounts.
- Hibernate creates the catalog tables from the JPA entities in the default local H2 configuration. There is currently no catalog-management API or seed data, so records must be populated through a separately authorized process before these endpoints return listings. A production database and schema migration strategy have not yet been selected.

### Orders and payment

- `POST /api/v1/me/orders` accepts pass IDs and quantities plus an idempotency key; the server recalculates prices and returns an order ID, line items, currency, and server-calculated total.
- `POST /api/v1/me/orders/{orderId}/payment` starts payment through the configured provider and returns a provider-safe continuation detail and pending status.
- `GET /api/v1/me/orders/{orderId}` returns the backend-owned order/payment state.
- Payment success is confirmed by provider-verified server processing, not by a front-end redirect.

### QR issuance and history

- `GET /api/v1/me/qr` requires authentication and returns `accountId` (opaque, non-PII cache key), `userPassId`, `passName`, `qrImageDataUrl`, and the stable opaque `qrPayload`. It does not return a rolling `expiresAt`; never let the browser construct QR credentials.
- The frontend accepts cached QR data only for its previously active account, uses it for offline display, and sends no cached credential to another account. The scanner remains responsible for validating current pass status and benefit quota.
- `GET /api/v1/me/usage-history` requires authentication and returns paginated entries containing `attractionName`, `passName`, and `usedAt` (ISO-8601), plus stable entry IDs for future pagination.
- Actual scanner redemption uses a separate authenticated venue API, for example `POST /api/v1/venue/redemptions`, with the credential and an idempotency key. Venue authentication/authorization must be distinct from visitor authentication. Online processing must be atomic. Offline synchronization must preserve the original event and detect conflicts.

All authenticated endpoints should return a consistent `401` when the visitor session is absent/expired; the frontend should clear account-scoped cached QR data on a connected `401`, account change, or sign-out, and must not show a stale cached QR to a signed-out account. Apply appropriate `Cache-Control: no-store` headers to identity, payment, and QR API responses.

## Profile, consent, and PDPA implementation boundary

- Email/password authentication uses BCrypt hashes, Spring Security sessions, CSRF protection, and an HTTP-only session cookie. Never store a password or session token in the demo cart or QR cache. Email verification, rate limiting, password recovery, and account locking are not yet implemented; do not enable public production registration until these controls are added.
- Google and Apple sign-in require provider app configuration and verified OIDC responses. OAuth state and ID-token validation are handled by Spring Security; provider accounts are not silently linked to existing email accounts. Configure callback URLs and secrets as described in `docs/authentication.md`.
- Publish a clear privacy notice before enabling registration. It should identify the data controller/contact, purposes, data categories, legal basis per purpose, recipients/processors, retention, data-subject rights/request channel, and cross-border transfers where relevant. Obtain qualified legal/privacy review; this template does not claim PDPA compliance.
- Distinguish notice acknowledgement from consent. Do not make optional marketing consent a condition of purchasing or using a pass. Record consent only where consent is the selected legal basis, with purpose, notice/version, choice, and timestamp. Support withdrawal and retain appropriate evidence.
- `POST /api/v1/me/privacy-preferences` should accept purpose-scoped choices and notice version; the backend must validate and persist an auditable record. Do not infer consent from a prechecked checkbox, silence, or the browser's `localStorage`.
- Provide a way to request account/data access or correction and account deletion/retention review through a properly authenticated backend process.

The operator must publish and review the privacy notice and configure its URL/version before enabling account registration. A notice acknowledgement is stored separately from optional marketing consent; marketing consent is not collected by this implementation. This does not claim PDPA compliance. Privacy rights and consent withdrawal need their own backend workflows.

## Acceptance checks

- Navigate through all five destinations using the bottom bar and Home quick links; confirm active navigation and keyboard focus are clear.
- Add, remove, and reload pass quantities; ensure the cart persists only as sample data on that browser and enforces its quantity cap.
- Verify checkout states clearly that no order/payment was created until an API exists.
- Verify the QR screen's pending, service-unavailable, offline-without-cache, cached, expired-pass, and clear-cache states. Repeated issuance and reload must preserve each pass's QR payload; different passes must have distinct payloads.
- Verify the service worker serves the app shell offline and does not cache `/api/` or cross-origin responses.
- Verify History handles loading, empty, malformed, and API failure responses without fabricating records.
- Verify email registration and login establish real server-side sessions, login/logout require CSRF tokens, and account configuration keeps registration disabled without an explicitly enabled, published privacy notice.
- Verify Google and Apple login only appear when configured, OAuth callbacks validate state and verified identity claims, and an existing email account is never auto-linked.
- At venues, test simultaneous online redemptions and the offline reconciliation conflict policy separately. Screenshot blocking is not an acceptance criterion because browsers cannot guarantee it.
