# Gaps: What could not be detected / is not present

Each item states the gap and the artifact that would be needed to document it.

## No frontend in this repository

- **Gap:** This is a backend-only repo (`com.planner.app`, Spring Boot REST API at
  `http://localhost:8080/api`). No frontend project (no `package.json`, no SPA/web client source)
  exists here.
- **Needed to document:** A separate frontend repository or directory, or its build config, to
  describe the client side.

## No CI/CD pipeline configuration

- **Gap:** No CI/CD config found — no `.github/workflows`, `.gitlab-ci.yml`, `Jenkinsfile`, or
  `.circleci/`. Build/test/deploy is manual (Maven Wrapper / Docker).
- **Needed to document:** A pipeline definition file describing build, test, and deployment stages.

## No automated test classes

- **Gap:** There is no `src/test` directory at all — only `src/main`. The only test-related
  artifact is the `spring-security-test` dependency (`test` scope) in `pom.xml`; no test classes
  exist to run. `spring-boot-starter-test` is not even declared.
- **Needed to document:** Actual test sources under `src/test/java` (and likely
  `spring-boot-starter-test`) to describe a test suite or coverage.

## No `docker-compose` / external PostgreSQL provisioning

- **Gap:** No `docker-compose.yml`/`.yaml`. The `Dockerfile` runs only the application
  (`mvn spring-boot:run`, dev profile) and does not start a database. The app expects PostgreSQL
  at `jdbc:postgresql://localhost:5432/tripbudgetplanner` (or via `SPRING_DATASOURCE_*` env vars).
- **Needed to document:** A `docker-compose.yml` (or equivalent infra config) defining a PostgreSQL
  service, so the DB provisioning and connection wiring can be described instead of assumed
  external.

## No migration tool wiring

- **Gap:** A hand-written `src/main/resources/db/V1__Init_Setup.sql` exists with Flyway-style
  naming, but **no Flyway or Liquibase dependency** is in `pom.xml` and nothing executes the
  script. Schema is governed by Hibernate `ddl-auto` (`update` in dev, `validate` in prod).
- **Needed to document:** A migration tool dependency + config (e.g. Flyway) wiring the `db/V*.sql`
  scripts into startup, to document a real, versioned migration process.

## Maven distribution version not pinned in tracked files

- **Gap:** The wrapper scripts `mvnw`/`mvnw.cmd` exist (wrapper version 3.3.4), but the pinned
  Maven distribution version lives in `.mvn/wrapper/maven-wrapper.properties`, which is gitignored
  (`.mvn` is excluded) and not present in the repo.
- **Needed to document:** The `maven-wrapper.properties` `distributionUrl` to state the exact
  Maven version the wrapper downloads.

## Credential/profile property files are untracked

- **Gap:** `src/main/resources/{dev,prod}/application.properties` are excluded by `.gitignore`
  (`### Credentials ###`). A fresh clone has no profile config, so DB credentials, JWT secret, and
  per-profile settings are not reproducible from the repo alone.
- **Needed to document:** A committed template (e.g. `application.properties.example`) or a
  documented list of required keys/env vars to recreate the profile config.
