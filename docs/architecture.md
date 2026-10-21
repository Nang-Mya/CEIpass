# Chiang Rai Pass Architecture Blueprint

## Purpose and status

This document translates the product requirements into an initial architecture. The repository uses plain HTML/CSS/JavaScript for the static visitor UI and Java 21 with Spring Boot 3.5 for the REST API. The backend currently defaults to in-memory H2 with Hibernate schema generation; Flyway is disabled, and no persistent production database or migration strategy has been configured. Public catalog read endpoints and server-side email/password session authentication are implemented; the catalog starts empty, and Google/Apple OAuth require provider credentials. Payment, history, QR issuance/redemption, and live frontend catalog integration remain future work.

## Product requirements and measurable outcomes

| Requirement | Initial acceptance criterion |
|---|---|
| Fast QR display | From a deliberate user action to a visibly rendered QR, p95 under 1 second on agreed representative mobile devices and network profiles. Record the measurement method and test profiles before release. |
| Useful offline experience | The app shell and explicitly approved read-only visitor data can be loaded offline, with freshness shown where relevant. |
| Safe payment state | A payment is marked successful only after server-side verification from a trusted provider response or verified webhook. |
| Single-use redemption | At most one concurrent redemption request for a QR credential can succeed. |
| Auditable partner settlement | Every transaction and revenue allocation is traceable to its source, with documented currency precision and rounding rules. |

The QR performance target is a UI performance objective. It does not permit bypassing server-side payment or redemption authorization.

## Logical architecture

```text
Visitor's mobile browser / installed PWA
  ├── App shell and visitor UI
  ├── Service worker: versioned static-asset caching
  └── IndexedDB: approved, non-authoritative offline data
              │ HTTPS API
              ▼
        Application backend
          ├── Authentication and authorization
          ├── Public pass / attraction catalog
          ├── Visitor pass / QR issuance and redemption
          ├── Checkout and payment-provider adapter
          ├── Verified webhook intake and event processing
          └── Transaction and partner-allocation services
              │
              ├── Relational persistence and audit/ledger records
              └── Payment provider (for example, PromptPay)
```

This is a logical separation, not a requirement to deploy each box as a separate service. Start with the simplest deployment that supports clear module boundaries, reliable transactions, and the expected operational needs.

## Front-end and PWA

### Suggested responsibilities

- Keep the initial route and QR display path small. Defer nonessential screens and assets until after the QR is visible.
- Render a QR only from a credential issued for the authenticated visitor and relevant pass or transaction.
- Provide explicit loading, ready, expired, offline, payment-pending, and error states. Distinguish “QR displayed” from “payment confirmed” and “redemption accepted.”
- Cache the app shell and versioned static assets in the service worker. Use IndexedDB only for approved data that has a documented purpose, freshness policy, and invalidation strategy.
- Exclude payment secrets, authentication tokens where inappropriate, and sensitive personal data from generic cache strategies. Define cache behavior for logout, account switching, expiry, and service-worker upgrades.
- Measure real-device performance on the critical path. If network issuance is needed to obtain a QR, optimize that request and its rendering; do not silently reuse expired credentials to meet the visual target.

### Offline redemption constraint

The current product direction allows venues to accept offline QR codes and reconcile later, with temporary duplicate-redemption risk explicitly accepted. Offline access cannot guarantee global one-time redemption across disconnected venues or scanners: two isolated scanners cannot know that the other has already accepted the same code. Preserve offline acceptance records and surface duplicate conflicts for review during synchronization; do not silently overwrite or claim a global one-time guarantee. The scanner trust model, credential lifetime, and synchronization rules still need implementation and validation.

## Back-end and API boundaries

Keep these responsibilities explicit, whether they are separate modules or services:

- **Public catalog:** expose only published partner and pass information. The implemented catalog response represents prices as decimal `BigDecimal` values and returns availability explicitly. Catalog reads are public; catalog editing and publication require a separately authorized management process.
- **Pass and QR:** issue credentials, check ownership and validity, and perform single-use redemption.
- **Checkout and payment:** create provider payment requests and track provider-backed payment state.
- **Webhook intake:** verify and durably record provider events, then hand them to idempotent processing.
- **Transactions and partner allocation:** record financial events and auditable allocation entries.
- **Authorization and audit:** enforce access on protected actions and record security-relevant outcomes without logging secrets.

Define versioned API contracts for QR issuance/redemption, checkout/payment status, and provider callbacks. Validate input at the boundary and return consistent errors. Browser redirects, client-submitted payment status, and cached client state are not authoritative evidence of payment.

Use bounded timeouts for provider calls. Apply retries only where they are safe; use provider-supported idempotency keys for payment creation and other retryable writes.

## Payment webhook handling

1. Read the exact raw request bytes required by the provider's signature scheme; verify the signature using the provider's documented algorithm and a secret held outside source control.
2. Apply provider-supported timestamp or replay checks. Support secret rotation and avoid logging signature material or sensitive payload fields.
3. Persist the verified event durably before acknowledging receipt. Store the provider event identifier under a unique constraint.
4. Process the event idempotently. Duplicate delivery must not create duplicate payment transitions, redemptions, transaction entries, or partner allocations.
5. Check the event's order reference, amount, currency, and allowed state transition against server-side records.
6. Record processing status and failures so that transient errors can be retried or reviewed without losing the original event.

The exact acknowledgement timing, retry policy, and response codes should follow the selected provider's webhook contract.

## Database and consistency safeguards

- Use a transactional relational store or another persistence system whose conditional-write and uniqueness guarantees are understood and tested.
- Model payment and QR lifecycles explicitly. Document legal transitions and enforce them on the server.
- Redeem a QR through one atomic conditional write: change it from eligible/unredeemed to redeemed only when its identifier matches, it is unexpired, and its current state permits redemption. Treat one affected row as success; zero affected rows means it was expired, invalid, or already used.
- Back the redemption condition with a database uniqueness or equivalent invariant where appropriate. Do not use “read, then later write” as the only concurrency safeguard.
- In the same transaction, persist the successful redemption and its audit record. Define safe behavior for a client retry after a timeout when the first request may already have committed.
- Enforce uniqueness for provider event IDs and relevant idempotency keys.
- The catalog API and JPA entities currently represent prices as decimal `BigDecimal` values. Define the production currency/precision and rounding policy consistently across catalog, payment-provider amounts, refunds, and partner settlement; never use binary floating-point for money.
- Preserve an immutable or append-only audit trail for financial events and partner allocations. Corrections should be represented as explicit adjustment entries rather than silently rewriting history.

## Security and reliability checklist

- Authenticate and authorize QR issuance, payment status, and redemption actions.
- Treat QR contents as opaque, unpredictable, short-lived credentials; avoid putting personal or payment data in the code.
- Rate-limit sensitive operations and make repeated requests safe through idempotency.
- Keep provider credentials and signing secrets in a managed secret store or protected runtime configuration.
- Use TLS for browser, backend, and provider communication.
- Use structured logs and correlation identifiers across checkout, webhook, and redemption flows; redact credentials and sensitive personal/financial data.
- Monitor webhook delivery/processing failures, payment states stuck in pending, redemption conflicts, and settlement discrepancies.
- Back up transaction data and define recovery and reconciliation procedures before launch.

## Current repository layout

The Java application is organized by feature under `BackEnd/src/main/java/com/chiangraipass`, with shared application configuration in `config`. See [Backend package structure](../structure.md) for the complete package tree. The test sources follow the same feature layout.

```text
FrontEnd/
  index.html
  manifest.webmanifest
  sw.js                # versioned app shell cache; does not cache API responses
  css/
  js/                  # configuration and five-screen visitor UI
  icons/
BackEnd/
  pom.xml
  src/main/java/com/chiangraipass/
    auth/               # session authentication, accounts, OIDC
    catalog/            # public passes and attractions
    pass/               # pass and benefit persistence models
    payment/            # payment persistence model
    health/             # health endpoint
    config/             # shared security, OAuth, CORS configuration
  src/main/resources/
    application.yml     # in-memory H2 defaults; Flyway disabled
  src/test/java/
docs/
  architecture.md
  development-workflow.md
  visitor-app-feature.md
```

There are currently no checked-in SQL migrations or database bootstrap scripts. Hibernate creates the schema in the local H2 database. Select a persistent production database, migration tool, backup/recovery process, and deployment configuration before treating accounts or catalog data as durable. Keep modules deployable together until operational requirements justify splitting them. Avoid duplicating payment or redemption state across independent services without a clear ownership and consistency model.

The static visitor UI currently includes Home, Buy, My QR, History, and Profile screens. Pass/attraction cards and the cart are clearly labeled local preview data. The app shell is cached by the service worker; API responses are not cached by it. The QR and history screens may call their planned API routes, but issuance, redemption, and history APIs are not implemented.

## Decisions to make before business-feature implementation

Record the decisions and rationale in pull requests or a lightweight architecture decision record:

- Supported browser/device targets and how the static front end will be served in each environment.
- Hosting, deployment environments, API domain, and observability approach.
- Production database hosting, migration strategy, and the transaction/isolation behavior relied on for conditional redemption.
- Payment provider(s), signature verification contract, idempotency support, refund flow, and sandbox process.
- Trusted offline scanner authorization, credential lifetime, and reconciliation design for the accepted temporary duplicate-redemption risk.
- Pass/QR validity period, renewal behavior, visitor identity model, and privacy/retention requirements.
- Partner split formula, rounding, refunds/chargebacks, settlement schedule, and reconciliation owner.
- Definition of the under-one-second QR metric, percentile, device profiles, and network test conditions.

## Required validation before launch

- Concurrently submit multiple redemptions for the same QR and prove exactly one succeeds.
- Replay the same valid webhook and prove that payment and ledger effects occur once.
- Test invalid signatures, unknown event IDs, wrong amounts/currencies, delayed events, and provider retries.
- Test QR expiry boundaries and safe client retries after timeouts.
- Test PWA startup and approved offline screens after fresh install, reload, cache update, logout, and account change.
- Measure QR display performance on agreed mobile devices and network profiles.
- Verify partner allocations, rounding, refunds, and reconciliation against known test cases.
