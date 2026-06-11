# Stack Detection

Source of truth: `pom.xml`, `Dockerfile`, `src/main/resources/{dev,prod}/application.properties`,
`src/main/java/com/planner/app/Application.java`, `LICENSE`. Only dependencies actually present
in `pom.xml` are listed. Versions not pinned in `pom.xml` are governed by the Spring Boot BOM
(`spring-boot-dependencies:3.5.6`, imported via `<dependencyManagement>`).

## Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (`java.version`, `maven.compiler.source/target`) |
| Framework | Spring Boot | 3.5.6 (`spring-boot.version` property) |
| Build | Maven + Maven Wrapper (`mvnw` / `mvnw.cmd`) | Wrapper scripts version 3.3.4; Maven distribution version comes from `.mvn/wrapper/maven-wrapper.properties` (gitignored, not in repo) |
| Build plugin | spring-boot-maven-plugin | 3.5.6 (`${spring-boot.version}`) |
| Build plugin | maven-compiler-plugin | 3.11.0 (pinned, `parameters=true`, source/target 21) |
| Web | spring-boot-starter-web | BOM-governed (3.5.6) |
| Database | PostgreSQL (driver `org.postgresql:postgresql`, `runtime` scope) | BOM-governed |
| ORM | Spring Data JPA / Hibernate (spring-boot-starter-data-jpa) | BOM-governed; dialect `org.hibernate.dialect.PostgreSQLDialect` |
| Auth | Spring Security (spring-boot-starter-security) | BOM-governed |
| Auth (JWT) | com.auth0 java-jwt | 4.4.0 (pinned) |
| Boilerplate | Lombok (`org.projectlombok:lombok`, optional; excluded from repackage) | BOM-governed |
| Dev loop | spring-boot-devtools (`runtime`, optional) | BOM-governed |
| Config metadata | spring-boot-configuration-processor (optional) | BOM-governed |
| Test (dep only) | spring-security-test (`test` scope) | BOM-governed |
| Container | Docker (`maven:3.9-eclipse-temurin-21-alpine` base image) | — |
| License | MIT | © 2025 Michel |

Notes:
- There is **no** `spring-boot-starter-parent`. The Spring Boot BOM is imported via
  `dependencyManagement` (`pom.xml` comment: `NO PARENT`), so unpinned dependency versions
  resolve from `spring-boot-dependencies:3.5.6`.
- `spring-boot-starter-test` is **not** declared; the only test-scoped dependency is
  `spring-security-test`.

## Build & run

Maven Wrapper (no local Maven install required):

```bash
# Linux / macOS
./mvnw clean package          # build + repackage executable jar (runs tests if any)
./mvnw test                   # run tests (no test sources currently exist)
./mvnw spring-boot:run        # run with default (dev) profile

# Windows
mvnw.cmd clean package
mvnw.cmd test
mvnw.cmd spring-boot:run
```

### Profiles

- Two Maven profiles in `pom.xml`: `dev` (`activeByDefault=true`) and `prod`. Each sets
  `spring.profiles.active` and filters the matching `src/main/resources/{dev,prod}` folder.
- Select prod explicitly: `./mvnw -Pprod clean package` (or `mvnw.cmd -Pprod ...`).
- The Spring profile can also be set at runtime, e.g.
  `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`.
- `Application.java` prints the active profile, server port, and datasource URL on startup.

### Server port

- `server.port=8080` in both `dev` and `prod` property files.
- `Application.java` advertises the API base path as `http://localhost:8080/api`.

### Config differences (dev vs prod)

| Key | dev | prod |
|-----|-----|------|
| `spring.jpa.hibernate.ddl-auto` | `update` | `validate` |
| `spring.jpa.show-sql` | `true` | `false` |
| `spring.devtools.*` | enabled (restart, livereload, poll/quiet tuning) | not configured |
| `logging.level.root` | `INFO` | `WARN` |
| `logging.level.com.planner.app` | `DEBUG` | `INFO` |
| `spring.jpa.properties.hibernate.dialect` | `PostgreSQLDialect` | `PostgreSQLDialect` (+ `spring.jpa.database-platform`) |

Both files also define `jwt.secret` and `jwt.expiration` (`86400000` = 24h). The secret value
is referenced by key name only and must not be committed in real deployments.

### Environment variables (read by dev config)

The `dev` datasource keys read environment variables with literal fallbacks:

- `SPRING_DATASOURCE_URL` (fallback `jdbc:postgresql://localhost:5432/tripbudgetplanner`)
- `SPRING_DATASOURCE_USERNAME` (fallback `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (fallback present; not reproduced here)

The `prod` config hardcodes these values (no env-var indirection).

### Docker path

`Dockerfile`:

- Base image `maven:3.9-eclipse-temurin-21-alpine`, `WORKDIR /app`.
- Copies `pom.xml`, runs `mvn dependency:go-offline -B` to cache deps, then copies `src`.
- `EXPOSE 8080 5005` (app port + JDWP debug port).
- `CMD` runs `mvn spring-boot:run` with `-Dspring-boot.run.profiles=dev`, JDWP attached
  (`-agentlib:jdwp=...,suspend=n,address=*:5005`), and `-Dspring-boot.run.fork=false`.

```bash
docker build -t budget-trip-planner .
docker run -p 8080:8080 -p 5005:5005 \
  -e SPRING_DATASOURCE_URL=... -e SPRING_DATASOURCE_USERNAME=... -e SPRING_DATASOURCE_PASSWORD=... \
  budget-trip-planner
```

This is a **dev-mode** container (runs `spring-boot:run`, not a packaged jar; DevTools + JDWP
enabled). It does not provision PostgreSQL itself — see `gaps.md`.

### Profile-based resource filtering / untracked credentials

- `pom.xml` filters `src/main/resources` and, per active profile, the matching
  `src/main/resources/dev` or `src/main/resources/prod` folder (Maven resource filtering enabled).
- `.gitignore` excludes `**/src/main/**/dev/` and `**/src/main/**/prod/` under a `### Credentials ###`
  heading. These folders hold the per-environment `application.properties` (DB credentials, JWT
  secret), so those property files are **intentionally untracked** in git.
- `.mvn` is also gitignored, so `.mvn/wrapper/maven-wrapper.properties` (which pins the Maven
  distribution version) is not present in the tracked repo.
