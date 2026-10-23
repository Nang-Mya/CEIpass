# Chiang Rai Pass

Chiang Rai Pass is a planned mobile-first web application for tourists. Its key goals are to display dynamic QR codes quickly, remain useful when connectivity is unstable, process payments reliably, and prevent QR redemption fraud.

> **Project status:** The repository contains a mobile-first visitor UI and a Spring Boot backend foundation. Email/password authentication and public catalog endpoints are implemented. OAuth requires provider credentials. The frontend still uses sample catalog/cart data; payment, order history, and venue redemption are not implemented. The backend defaults to an in-memory H2 database, so it is for local development/testing—not persistent production data. The Cloud Run pipeline currently serves only a static frontend preview.

## Product and architecture goals

- Display the visitor's current QR code in under one second for the agreed measurement conditions.
- Provide a useful offline experience without implying that an unverified payment or redemption succeeded.
- Integrate payment providers such as PromptPay through trusted server-side flows.
- Process payment webhooks securely, durably, and idempotently.
- Prevent multiple successful redemptions of the same QR code, including simultaneous scans.
- Maintain auditable transactions and partner revenue splits.

## Documentation

- [Architecture blueprint](docs/architecture.md): current stack and package layout, logical components, security and consistency requirements, and decisions still to make.
- [Backend package structure](structure.md): feature-oriented Java packages and matching test layout.
- [Development workflow](docs/development-workflow.md): branch naming, pull request expectations, and a phased implementation sequence.
- [Visitor app feature](docs/visitor-app-feature.md): five-screen navigation, current preview behavior, offline QR limits, and the backend contracts needed for live data.
- [Authentication setup](docs/authentication.md): email/password sessions, Google/Apple OAuth configuration, privacy-notice gating, and production prerequisites.

## Starter stack

- **Front end:** plain HTML, CSS, and JavaScript in `FrontEnd/`; served as static files during development, with a PWA app-shell cache.
- **Back end:** Java 21 and Spring Boot 3.5 in `BackEnd/`.
- **Database:** Spring Data JPA with in-memory H2 by default. Hibernate creates/updates the local schema; Flyway is currently disabled. This is not a persistent production database configuration.

## Run locally

Prerequisites: Java 21 and Maven 3.9+.

1. From the repository root, start the backend:

   ```powershell
   mvn -f BackEnd/pom.xml spring-boot:run
   ```

   The default H2 database is created in memory and reset when the application stops. Hibernate manages the schema; no database setup or SQL migration step is required. Check the API at `http://localhost:8080/api/v1/health`.

2. Serve `FrontEnd/` at `http://localhost:5500` with a static-file development server, such as the VS Code Live Server extension. The visitor UI calls the API at `http://localhost:8080`; configure `FrontEnd/js/config.js` if your local API address differs. The five screens and sample catalog/cart work in the browser. Public catalog and authentication endpoints are implemented, but the catalog UI still uses sample data. New registrations remain disabled until a reviewed privacy notice is published and explicitly configured; provider sign-in additionally requires provider credentials. See [Authentication setup](docs/authentication.md). Payment, history, and venue redemption are not implemented.

3. Run backend tests with `mvn -f BackEnd/pom.xml test`.

## Google Cloud continuous deployment

The root [cloudbuild.yaml](cloudbuild.yaml) runs backend verification, builds the static frontend preview, pushes it to Artifact Registry, and deploys it as a public Cloud Run service. The deployed image is the frontend-only Nginx container; it does not start the Spring Boot API. Requests under `/api/` return an explicit `503`. The backend is not deployed by this pipeline.

Before connecting a Cloud Build trigger to `main`:

1. Enable the Cloud Build, Cloud Run, and Artifact Registry APIs in the Google Cloud project. Create the Artifact Registry Docker repository named `cei-pass` in `us-central1`, or change `_AR_REPOSITORY` in `cloudbuild.yaml` to match the repository you created.
2. Grant the Cloud Build service account permission to push images to that repository and deploy Cloud Run services.
3. Create a Cloud Build repository trigger for pushes to `main` and set `cloudbuild.yaml` as its build configuration. The trigger builds the frontend preview and deploys service `cei-pass`; the build must pass the backend test suite first.

The Cloud Run URL serves the frontend preview only. Login, catalog, and other API-dependent features remain unavailable there until a persistent production database, backend deployment, and frontend-to-backend routing are implemented and configured. The current backend uses in-memory H2 and must not be deployed as a production data store. Select a production database and migration strategy before enabling persistent production accounts or passes. Registration remains disabled by default; set up and review privacy-notice configuration before enabling it. Google/Apple OAuth also needs provider credentials and the deployed callback URL before it can be enabled.
