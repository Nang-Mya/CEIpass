# Development Workflow

## Branch model

Use a lightweight GitHub Flow:

- `main` is the protected, reviewed, releasable branch.
- Create a short-lived branch from the latest `main` for each focused change.
- Open a pull request (PR) back to `main`; merge only after required checks and review pass.
- Tag a verified release from `main` (for example, `v1.0.0`). Add a `release/<version>` branch only when a separate stabilization period is needed.

Do not commit directly to `main`. Avoid a long-lived `develop` branch unless the team later identifies a concrete integration need.

## Branch naming

Use lowercase kebab-case names with a type prefix:

| Work | Pattern | Examples |
|---|---|---|
| Feature | `feature/<short-description>` | `feature/qr-generator`, `feature/payment-integration`, `feature/pwa-offline-cache`, `feature/webhook-inbox` |
| Bug fix | `bugfix/<short-description>` | `bugfix/qr-expiry-validation`, `bugfix/duplicate-webhook` |
| Urgent production fix | `hotfix/<short-description>` | `hotfix/payment-status-reconciliation` |
| Release stabilization (optional) | `release/<version>` | `release/1.0.0` |

Keep branches focused. Split unrelated work into separate PRs. Include an issue number when the team has an issue-tracking convention, for example `feature/123-qr-generator`.

## Pull request process

1. Update the local `main` branch and create a branch using the naming convention above.
2. Make a focused change with tests and documentation for changed behavior, API contracts, or database migrations.
3. Push the branch and open a PR to `main`. Describe the purpose, implementation, test evidence, migration/configuration impacts, and any security or rollout considerations.
4. Request review from an owner of the affected area. Resolve review feedback in the same branch.
5. Require appropriate automated checks before merge:
   - formatter/linter and static checks;
   - front-end and back-end tests relevant to the change;
   - database migration validation;
   - security/dependency checks;
   - build and, when available, integration tests.
6. Merge using the repository's agreed merge method. Delete the branch after merge.
7. For releases, verify the candidate on the target environment, document the release, and tag the merged commit.

Protect `main` with required PR reviews and status checks once CI is configured. Never place production credentials or payment-provider secrets in source code, PR descriptions, or committed environment files.

## Suggested development phases

Treat the items below as independently reviewable feature branches. Adjust ordering when dependencies or team ownership require it.

| Phase | Example branch | Scope and exit evidence |
|---|---|---|
| 1. Project foundation | `feature/project-foundation` | Scaffolded: plain HTML/CSS/JavaScript UI, Java 21/Spring Boot API, in-memory H2 development database, local setup instructions, health endpoint, and backend CI. A persistent production database and migration strategy remain to be selected. |
| 2. API and data contracts | `feature/payment-and-qr-contracts` | Define API shapes, payment/QR states, schema migrations, currency precision, partner split and rounding rules. Review contracts before provider or UI flows depend on them. |
| 3. Transaction-safe redemption | `feature/qr-redemption` | Implement QR issuance/expiry and atomic one-time redemption. A concurrency test proves no more than one competing request succeeds. |
| 4. Payment provider | `feature/payment-integration` | Add server-side checkout/provider adapter, payment state transitions, and provider sandbox tests. Client state alone cannot mark a payment paid. |
| 5. Webhook inbox and processing | `feature/webhook-inbox` | Verify signatures, durably store events, deduplicate, process idempotently, and test invalid/replayed/duplicate events and retries. |
| 6. Visitor QR experience | `feature/qr-generator` | Implement the mobile-first QR display and loading/expiry/payment/offline/error states. Validate accessibility and the agreed display performance metric. |
| 7. PWA and offline data | `feature/pwa-offline-cache` | Add manifest, app shell caching, approved offline data storage, cache lifecycle, and offline messaging. Verify offline operation cannot falsely confirm a payment or globally authorize a redemption. |
| 8. Transaction history and partner allocation | `feature/partner-revenue-splitting` | Add auditable transaction/allocation records, refund/adjustment behavior, and test cases for rounding and reconciliation. |
| 9. Release readiness | `release/1.0.0` if needed | Complete end-to-end, security, concurrency, backup/recovery, performance, and operational checks; fix release-blocking defects on focused `bugfix/*` or `hotfix/*` branches. |

## Branch dependency guidance

- Agree on API contracts and state transitions before parallel front-end/back-end implementation.
- When a production database and migration tool are selected, land compatible migrations and backend behavior before relying on them in the UI.
- Keep webhook processing independent from browser redirect handling; the backend must remain authoritative.
- Coordinate changes to shared contracts and any future migrations in PR descriptions. Avoid parallel incompatible migrations against the same schema.
- The offline cache branch depends on a documented offline policy; it must not invent offline redemption guarantees.
