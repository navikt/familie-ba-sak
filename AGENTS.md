# AGENTS.md

Backend for saksbehandling av barnetrygd. Spring Boot 4 + Kotlin 2, Maven, PostgreSQL, Flyway, Kafka. Runs on Nais (GCP). Owned by team-baks (teamfamilie namespace).

## Build & test

```bash
# Build (skip tests)
./mvnw package -DskipTests

# Run all tests (needs Docker for Testcontainers PostgreSQL)
./mvnw test

# Unit tests only (no DB, no Spring context)
./mvnw test -Penhetstest

# Integration tests only (starts Testcontainers PostgreSQL + Spring context)
./mvnw test -Pintegrasjonstest

# Verdikjedetester (full flow with WireMock, two variants)
./mvnw test -Pverdikjedetest-feature-toggle-paa
./mvnw test -Pverdikjedetest-feature-toggle-av

# Format check / fix (ktlint via Maven Antrun)
./mvnw antrun:run@ktlint-format

# Run single test class
./mvnw test -Dtest=MyTestClass

# Run single test method
./mvnw test -Dtest=MyTestClass#myMethod
```

CI (pull requests) runs ktlint, enhetstest, integrasjonstest, and verdikjedetester (both toggle variants) in parallel. Sonar runs after enhetstest and integrasjonstest complete. All jobs must pass. Push to main skips tests and deploys directly.

## Project layout

```
src/
  main/kotlin/no/nav/familie/ba/sak/
    kjerne/             # Domain logic (behandling, vedtak, beregning, eøs, vilkårsvurdering, ...)
    config/             # Spring configuration
    ekstern/            # External-facing APIs (pensjon, etc.)
    integrasjoner/      # Clients for other services (PDL, økonomi, ...)
    sikkerhet/          # Auth/security
    statistikk/         # Statistics/reporting
    task/               # Async task definitions (prosessering framework)
    internal/           # Internal endpoints
    common/             # Shared utilities
  main/resources/
    db/migration/       # Flyway migrations (sequential V<N>__description.sql)
    db/init/            # Initial schema (V1, V2 — base tables)
    avro/               # Avro IDL for Kafka schemas
    application.yaml    # Main config (port 8089)
  test/
    enhetstester/       # Unit tests (no Spring context, no DB)
    integrasjonstester/ # Integration tests (@Tag("integration"), Testcontainers)
    testdata/           # Shared data generators (datagenerator/*.kt)
    resources/cucumber/ # Cucumber .feature files (BDD scenarios)
```

## Test conventions

- **Unit tests** go in `src/test/enhetstester/`. No `@Tag` needed (they're selected by excluding `integration` and `verdikjedetest` tags).
- **Integration tests** go in `src/test/integrasjonstester/`. Must extend `AbstractSpringIntegrationTest` (which adds `@Tag("integration")` and activates mock profiles).
- **Verdikjedetester** extend `AbstractVerdikjedetest` with `@Tag("verdikjedetest")`. Use WireMock on port 1337.
- **Test data generators** live in `src/test/testdata/kotlin/.../datagenerator/`. Use these instead of creating ad-hoc test objects.
- **Cucumber tests** are in `src/test/resources/cucumber/` with step defs in enhetstester. Run as unit tests (no DB needed). Controlled by `RunCucumberTest.kt`.
- All three test source roots (`enhetstester`, `integrasjonstester`, `testdata`) are registered via `build-helper-maven-plugin`.
- Test execution order is randomized (`runOrder=random`).

## Database (Flyway)

- Migrations in `src/main/resources/db/migration/` use sequential numbering: `V<N>__description.sql`.
- New migrations must use the next available number. Check the directory for the current highest version before creating a new one.
- Flyway also scans `classpath:db/init` for base schema.
- Production DB: PostgreSQL on GCP (`familie-ba-sak` instance).
- Local dev with Testcontainers: run with `--dbcontainer` VM option, or use Docker manually (see README).

## Code generation

- **Avro**: `avro-maven-plugin` generates Java classes from `src/main/resources/avro/*.avdl` into `target/generated-sources/` during `generate-sources` phase. Do not edit generated files.

## Key dependencies & frameworks

- **Spring Boot 4** with Jetty (Tomcat excluded), Spring Data JPA, Spring Kafka
- **Kotlin 2** with `spring` and `jpa` compiler plugins (allopen/noarg)
- **Auth**: `spring-security` for Azure AD / TokenX token validation.
- **Async tasks**: Nav's `prosessering` framework for background jobs
- **Feature toggles**: Unleash. Toggle locally with `-D<flag>=true/false` as VM option.
- **Coverage**: Kover (not JaCoCo) — reports go to `target/coverage/` or `target/site/kover/`
- **JSON**: Jackson 3
- **Mocking**: MockK (not Mockito)
- **Sentry** for error tracking
- **Cucumber 7.x** for BDD tests

## Formatting

- **ktlint** enforced in CI.
- Run `./mvnw antrun:run@ktlint-format` before committing to auto-fix.

## Deployment

- Merge to `main` → auto-deploy: build → dev-gcp → prod-gcp.
- Emergency deploy: manual workflow `manual-deploy-prod` (build and deploy) or `manual-deploy-with-image` (deploy existing image).
- Nais manifests: `.nais/app-dev.yaml`, `.nais/app-prod.yaml`.
- Namespace: `teamfamilie`. Azure AD + TokenX enabled. Kafka pool: `nav-dev`/`nav-prod`.

## Auth model

- **Inbound**: Azure AD (from frontend `familie-ba-sak-frontend`, mottak, klage, pensjon, bidrag). TokenX from `familie-ba-minside-frontend` (citizen self-service).
- **Outbound**: Azure AD on-behalf-of / client_credentials to integrasjoner, brev, klage, oppdrag, PDL, etc.
- Role groups configured in Nais manifest (veileder, saksbehandler, beslutter, forvaltning, strengt fortrolig, fortrolig).

## Common pitfalls

- Integration tests require Docker (Testcontainers). They will fail without a running Docker daemon.
- JDK 25 required (set in pom.xml and CI workflows).
- Use `secureLogger` when logging fødselsnummer or other PII — never the standard logger.
- Don't commit changes unless the user explicitly asks you to.

## Keeping this file current

This file must stay in sync with the codebase. When you make a change that affects information described here, update AGENTS.md in the same commit. Specifically:

- **New top-level packages** under `src/main/kotlin/.../sak/` or new test source roots: update "Project layout".
- **CI workflow changes**: update "Build & test" commands or the CI pipeline description if profiles, job order, or JDK version change.
- **Auth/access policy changes**: update "Auth model" if inbound/outbound rules or auth mechanisms change.
- **New test conventions**: update "Test conventions" if new base classes, tags, or test source roots are introduced.
- **New code generation**: update "Code generation" if new generators or plugin phases are added.

Note: Exact dependency versions and Flyway migration numbers are intentionally omitted — they change frequently (e.g. via Dependabot) and are easy for an agent to look up directly from `pom.xml` or the migration directory.

If unsure whether a change warrants an update, ask: "Would a future agent get this wrong without the update?" If yes, update the file.
