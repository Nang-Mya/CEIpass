# Chiang Rai Pass

Chiang Rai Pass is a mobile-first web application for visitors to explore Chiang Rai attractions and pass options. It combines an installable web interface with a Spring Boot API for visitor accounts, pass information, QR codes, and usage history. The project is a working academic prototype: the catalog and account features are available locally, while payment processing and venue-side pass redemption are not integrated with live providers.

## GROUP MEMBERS

6831503079  Suphanee Muelae
6831503085  Nang Thanthar Aye Mya
6831503090  Kunyakorn Klin-eam
6831503093  Thitaree Panyasao
6831503106  Sirinda Pumsiri

## Member responsibilities

- **6831503079 — Suphanee Muelae:** Attraction/catalog data, home search and restaurant/hotel browsing, and pass-history and usage-summary features.
- **6831503085 — Nang Thanthar Aye Mya:** Pass purchase/checkout interface, sign-in flow integration, and unique QR pass behavior.
- **6831503090 — Kunyakorn Klin-eam:** Visitor-interface layout and styling, attraction-card presentation, and PWA/offline app-shell behavior.
- **6831503093 — Thitaree Panyasao:** Visitor-interface and attraction/pass catalog content updates.
- **6831503106 — Sirinda Pumsiri:** Account and profile features, email verification and password reset, privacy controls, and authentication/security improvements.

These responsibility areas summarize contributions visible in the repository history. Please confirm them with all members before submission.

## Features and project requirements

### Implemented in this prototype

- Mobile-first visitor interface with home, pass browsing, QR, history, profile, restaurant, and hotel sections.
- Attraction and pass catalog data, with attraction search and local images.
- Visitor registration and sign-in, profile management, email verification, and password reset flows.
- Privacy notice and visitor privacy preferences.
- Pass QR, pass summary, and usage-history API features.
- PWA manifest and app-shell service worker for limited offline use.
- H2 in-memory database for local development.

### Not yet production-ready

- Checkout is a prototype; there is no live payment-provider integration or payment webhook processing.
- Venue-side QR scanning/redemption is not implemented.
- H2 data is temporary and is lost when the application stops; a persistent production database and migration plan are still needed.
- OAuth is disabled unless provider credentials and callback configuration are supplied.
- Email delivery defaults to application logging. Production SMTP requires local environment configuration.
- Offline mode does not guarantee that a payment or pass redemption has succeeded.

## Requirements

- Java 21 and Maven 3.9 or later.
- Network access on the first build so Maven can download dependencies.
- No API keys, provider credentials, or demo password are required for the local default run.

## Run locally

Run all commands from the repository root.

1. Confirm the installed Java and Maven versions:

   ```sh
   java -version
   mvn -version
   ```

2. Start the Spring Boot application:

   ```sh
   mvn spring-boot:run
   ```

3. Open the application at [http://localhost:8080](http://localhost:8080). The API health endpoint is [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health).

4. Stop the application with `Ctrl+C`. The default database is in-memory H2; application data is reset when the process stops.

To run the backend test suite:

```sh
mvn test
```

### Optional local demo account

No demo login is enabled by default. To seed the local demo account and sample pass, set your own password in the environment when starting the application. Do not commit the value:

```sh
DEMO_ACCOUNT_PASSWORD='choose-a-local-password' mvn spring-boot:run
```

The demo account email is `demo@ceipass.com`. The chosen value is only an example placeholder; use a private local value. Do not use this demo account or the in-memory database for production.

### Optional OAuth or email setup

Google/Apple OAuth requires provider credentials and callback configuration. SMTP delivery requires mail settings; without them, the application uses its configured log mode. Set credentials in environment variables rather than source files. See `BackEnd/src/main/resources/application.yml` for supported environment variable names.

## Project layout

- `pom.xml` — root Maven entry point; `mvn spring-boot:run` and `mvn test` work from this directory.
- `BackEnd/src/main/java` — Spring Boot application and API source.
- `BackEnd/src/main/resources/static` — frontend, styles, images, manifest, and service worker served by Spring Boot.
- `BackEnd/pom.xml` — backend-local Maven project configuration, retained for existing workflows.
