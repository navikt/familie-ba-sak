# AGENTS.md — familie-ba-sak

Backend for case management of child benefit (barnetrygd). Spring Boot 4 + Kotlin 2, Maven, PostgreSQL, Flyway, Kafka.  
Runs on Nais (GCP). Owned by team-baks (teamfamilie namespace).

## Build & Test Commands

Always include `-Dkotlin.compiler.daemon=false` in every `mvn` command.  
The Kotlin compiler daemon fails to start in this environment due to RMI registry permission restrictions (`Operation not permitted`), so the compiler must run in-process instead.

```bash
# Build (skip tests)
mvn package -DskipTests -Dkotlin.compiler.daemon=false

# Run all tests (needs Docker for Testcontainers PostgreSQL)
mvn test -Dkotlin.compiler.daemon=false

# Unit tests only (no DB, no Spring context)
mvn test -Penhetstest -Dkotlin.compiler.daemon=false

# Integration tests only (starts Testcontainers PostgreSQL + Spring context)
mvn test -Pintegrasjonstest -Dkotlin.compiler.daemon=false

# End-to-end tests (full flow with WireMock, two variants)
mvn test -Pverdikjedetest-feature-toggle-paa -Dkotlin.compiler.daemon=false
mvn test -Pverdikjedetest-feature-toggle-av -Dkotlin.compiler.daemon=false

# Format check / fix (ktlint via Maven Antrun)
mvn antrun:run@ktlint-format -Dkotlin.compiler.daemon=false

# Run single test class
mvn test -Dtest=MyTestClass -Dkotlin.compiler.daemon=false

# Run single test method
mvn test -Dtest=MyTestClass#myMethod -Dkotlin.compiler.daemon=false
```

CI (pull requests) runs ktlint, unit tests, integration tests, and end-to-end tests (both toggle variants) in parallel.  
Sonar runs after unit and integration tests complete. All jobs must pass.  
Push to main (except changes in paths ignored by the workflow, e.g. `**.md`) builds with tests skipped and deploys dev-gcp → prod-gcp.

## Project Structure

```text
src/
  main/kotlin/no/nav/familie/ba/sak/
    kjerne/                    # Domain logic (behandling, vedtak, beregning, eøs, vilkårsvurdering, ...)
    config/                    # Spring configuration
    ekstern/                   # External-facing APIs (pensjon, etc.)
    integrasjoner/             # Clients for other services (PDL, økonomi, ...)
    sikkerhet/                 # Auth/security
    statistikk/                # Statistics/reporting
    task/                      # Async task definitions (prosessering framework)
    internal/                  # Internal endpoints
    common/                    # Shared utilities
  main/resources/
    db/migration/              # Flyway migrations (sequential V<N>__description.sql)
    db/init/                   # Initial schema (V1, V2 — base tables)
    avro/                      # Avro IDL for Kafka schemas
    application.yaml           # Main config (port 8089)
  test/
    enhetstester/kotlin/       # Unit tests (no Spring context, no DB)
    integrasjonstester/kotlin/ # Integration tests (@Tag("integration"), Testcontainers)
    testdata/kotlin/           # Shared data generators (datagenerator/*.kt)
    resources/cucumber/        # Cucumber .feature files (BDD scenarios)
```

## Code Style

### Language

Write everything in Norwegian — code (variable names, class names, methods, comments), documentation, commit messages, PR descriptions, and error messages.  
Exceptions are technical keywords, framework APIs, and standardized terms that don't translate naturally (e.g. `fun`, `class`, `repository`, `service`).

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.  
Do not rename variables, restructure working code, or refactor beyond the task at hand.  
Keep diffs small and focused so they are easy to review.

### Testing Conventions

- **Unit tests** go in `src/test/enhetstester/`. No `@Tag` needed (selected by excluding `integration` and `verdikjedetest` tags).
- **Integration tests** go in `src/test/integrasjonstester/`. Must extend `AbstractSpringIntegrationTest` (which adds `@Tag("integration")` and activates mock profiles).
- **End-to-end tests** extend `AbstractVerdikjedetest` with `@Tag("verdikjedetest")`. Use WireMock on port 1337.
- **Test data generators** live in `src/test/testdata/kotlin/.../datagenerator/`. Use these instead of creating ad-hoc test objects.
- **Cucumber tests** are in `src/test/resources/cucumber/` with step defs in enhetstester. Run as unit tests (no DB needed). Controlled by `RunCucumberTest.kt`.
- All three test source roots (`enhetstester`, `integrasjonstester`, `testdata`) are registered via `build-helper-maven-plugin`.
- Test execution order is randomized (`runOrder=random`).
- Integration tests require Docker (Testcontainers). They will fail without a running Docker daemon.
- Structure test bodies with `// Arrange`, `// Act`, and `// Assert` comments. Use `// Act & Assert` when combined, e.g. with `assertThrows { ... }`.

### Key Dependencies & Frameworks

- **JDK 25** (required; set in `pom.xml` and CI workflows)
- **Spring Boot 4** with Jetty (Tomcat excluded), Spring Data JPA, Spring Kafka
- **Kotlin 2** with `spring` and `jpa` compiler plugins (allopen/noarg)
- **Auth**: `spring-security` for Azure AD / TokenX token validation
- **Async tasks**: Nav's `prosessering` framework for background jobs
- **Feature toggles**: Unleash. Toggle locally with `-D<flag>=true/false` as VM option.
- **Coverage**: Kover (not JaCoCo) — reports go to `target/coverage/` or `target/site/kover/`
- **JSON**: Jackson 3
- **Mocking**: MockK (not Mockito)
- **Sentry** for error tracking
- **Cucumber 7.x** for BDD tests

## Git Workflow

- Merge to `main` → auto-deploy: build → dev-gcp → prod-gcp.
- Emergency deploy: manual workflow `manual-deploy-prod` (build and deploy) or `manual-deploy-with-image` (deploy existing image).
- Nais manifests: `.nais/app-dev.yaml`, `.nais/app-prod.yaml`.

## Auth Model

- **Inbound**: Azure AD (from frontend `familie-ba-sak-frontend`, mottak, klage, pensjon, bidrag). TokenX from `familie-ba-minside-frontend` (citizen self-service).
- **Outbound**: Azure AD on-behalf-of / client_credentials to integrasjoner, brev, klage, oppdrag, PDL, etc.
- Role groups configured in Nais manifest (veileder, saksbehandler, beslutter, forvaltning, strengt fortrolig, fortrolig).
- Namespace: `teamfamilie`. Azure AD + TokenX enabled. Kafka pool: `nav-dev`/`nav-prod`.

## Boundaries

### ✅ Always

- Run tests and ktlint after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input
- Use `secureLogger` when logging national identity numbers (fødselsnummer) or other PII — never the standard logger
- Keep this file in sync with the codebase when making changes that affect the information described here

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema
- Creating new Flyway migrations (check `src/main/resources/db/migration/` for the next available version number)

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries
- Edit generated Avro files in `target/generated-sources/`
- Commit or push changes unless the user explicitly asks you to

## Keeping This File Current

Update this file in the same commit whenever a change affects the information described here:

- **New top-level packages** under `src/main/kotlin/.../sak/` or new test source roots: update "Project Structure".
- **CI workflow changes**: update "Build & Test Commands" if profiles, job order, or JDK version change.
- **Auth/access policy changes**: update "Auth Model" if inbound/outbound rules or auth mechanisms change.
- **New test conventions**: update "Testing Conventions" if new base classes, tags, or test source roots are introduced.
- **New code generation**: update "Key Dependencies & Frameworks" if new generators or plugin phases are added.

Note: Exact dependency versions and Flyway migration numbers are intentionally omitted — they change frequently (e.g. via Dependabot) and are easy for an agent to look up directly from `pom.xml` or the migration directory.

If unsure whether a change warrants an update, ask: "Would a future agent get this wrong without the update?" If yes, update the file.
