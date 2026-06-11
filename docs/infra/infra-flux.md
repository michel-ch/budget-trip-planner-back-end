# Infrastructure Flux Diagram — Budget Trip Planner (back-end)

> Authoritative companion to `docs/infra/infra-flux.drawio`. Every flux (`F1…F6`) below is
> grounded in real configuration/code with `file:line` evidence. The `F#` IDs are identical
> between this file and the draw.io description panel.
>
> **Deployment context:** on-prem / Docker. No cloud provider, no Terraform, no managed
> services, no CI/CD pipeline file, and no external object storage were detected — so none are
> drawn. Generic shapes only.
>
> **Secrets policy:** only environment-variable *names* and property *keys* appear here — never
> their values.

---

## Perimeters

| Perimeter | Meaning | Components |
|---|---|---|
| **Client / External** | Anything outside the container that initiates traffic | Browser SPA (Angular dev server `http://localhost:4200`), Developer IDE/debugger |
| **Application container (Docker)** | The single `maven:3.9-eclipse-temurin-21-alpine` container running the Spring Boot app | Spring Boot 3.5.6 app (Java 21), Spring Security filter chain, `JwtAuthenticationFilter`, `JwtUtil`, HikariCP pool, container env vars |
| **Data tier (PostgreSQL)** | The PostgreSQL database the app persists to | PostgreSQL `tripbudgetplanner` @ 5432 |

---

## Flux table

| F# | Name | Category | Trigger | Source → … → Target | Protocol | Auth | Data | Frequency | Evidence (file:line) |
|----|------|----------|---------|---------------------|----------|------|------|-----------|----------------------|
| **F1** | HTTP REST runtime | runtime | Client issues an API call | Browser SPA → Spring Boot app `:8080` `/api/**` | HTTP REST/JSON | Bearer JWT (except `/api/auth/**`, OPTIONS) | Request/response JSON (voyages, expenses, users…) | Per user request | `server.port=8080` (dev/application.properties:16, prod/application.properties:18); `EXPOSE 8080` (Dockerfile:10); CORS origin `http://localhost:4200` (SecurityConfig.java:55) |
| **F2** | JWT auth (stateless) | auth | Request carries `Authorization: Bearer <JWT>` | Client → `JwtAuthenticationFilter` → `JwtUtil` (HMAC256 verify) → `SecurityContext` | HTTP header on F1 | HS256 JWT, issuer `planner-app` | Token subject = username; no server-side session | Every authenticated request | `authHeader.startsWith("Bearer ")` (JwtAuthenticationFilter.java:35-37); `validateToken` (JwtAuthenticationFilter.java:44); `SessionCreationPolicy.STATELESS` (SecurityConfig.java:45); `addFilterBefore(jwtAuthenticationFilter, …)` (SecurityConfig.java:47); `Algorithm.HMAC256` + issuer (JwtUtil.java:33,41-43); `java-jwt 4.4.0` (pom.xml:94-97) |
| **F3** | JDBC persistence | data | App reads/writes entities | Spring Boot app (HikariCP) → PostgreSQL `:5432/tripbudgetplanner` | JDBC (PostgreSQL driver) | DB username/password via env vars | Hibernate/JPA SQL over schema in `V1__Init_Setup.sql` | Per data operation | `spring.datasource.url=…jdbc:postgresql://…:5432/tripbudgetplanner` (dev/application.properties:4, prod:5); `driver-class-name=org.postgresql.Driver` (dev:7, prod:8); `postgresql` runtime dep (pom.xml:116-120); schema tables (V1__Init_Setup.sql:5-90) |
| **F4** | JDWP remote debug *(dev only)* | ops | Developer attaches debugger | Developer IDE → JVM debug agent `:5005` in container | JDWP over `dt_socket` (TCP) | None (open dev socket) | Debug control / breakpoints; no business data | On-demand, dev only | `EXPOSE … 5005` (Dockerfile:10); `-agentlib:jdwp=…address=*:5005` (Dockerfile:15); dev profile launch (Dockerfile:13-14) |
| **F5** | Secrets / config injection | secrets-config | Container start / Spring context load | Container env vars + app properties → Spring `@Value`/`datasource` | Process env + property resolution | n/a (delivery channel) | `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`; `jwt.secret`, `jwt.expiration` (keys only) | Once at startup | `${SPRING_DATASOURCE_URL:…}` / `_USERNAME` / `_PASSWORD` (dev/application.properties:4-6); `jwt.secret`, `jwt.expiration` (dev:31-32, prod:25-26); `@Value("${jwt.secret}")` / `${jwt.expiration:…}` (JwtUtil.java:16-20) |
| **F6** | Image build & run | cicd | `docker build` then container run | Dockerfile → `mvn dependency:go-offline` → `mvn spring-boot:run` (dev profile) | Maven / Docker build | n/a | Build artifacts + running app; **no external CI/CD pipeline file detected** | Per build | base image (Dockerfile:1); `mvn dependency:go-offline -B` (Dockerfile:6); `CMD ["mvn","spring-boot:run",…profiles=dev…]` (Dockerfile:13-16) |

---

## Per-flux descriptions

**F1 — HTTP REST runtime.** The browser SPA (the Angular dev server at `http://localhost:4200`, the only
allowed CORS origin) calls the Spring Boot application on port `8080` under `/api/**`. Traffic is plain
HTTP REST/JSON in this on-prem/dev setup (no TLS terminator is configured in the repo). All routes require
authentication except `/api/auth/**` (login/register) and CORS `OPTIONS` pre-flight, which are `permitAll`.

**F2 — JWT auth (stateless).** Every request carrying `Authorization: Bearer <JWT>` passes through
`JwtAuthenticationFilter`, registered before `UsernamePasswordAuthenticationFilter`. The filter extracts the
token, and `JwtUtil` verifies the HMAC256 signature and issuer (`planner-app`) using the `jwt.secret` key. On
success it populates the `SecurityContext`. The chain is `STATELESS` — no server-side session is stored. F2 is
not a separate network hop; it is the auth check riding on the F1 request edge.

**F3 — JDBC persistence.** The application persists through Spring Data JPA / Hibernate over the bundled
PostgreSQL JDBC driver to `jdbc:postgresql://<host>:5432/tripbudgetplanner`. Connections are pooled by
HikariCP (Spring Boot's default pool — no alternative pool is configured). The DB username and password are
supplied via the `SPRING_DATASOURCE_*` env vars (see F5). The schema is defined in `V1__Init_Setup.sql`
(Locations, Images, Users, Friends, Voyages, Travel_groups, Group_memberships, Expenses, Itinerary).

**F4 — JDWP remote debug (development only).** The Dockerfile exposes port `5005` and launches the JVM with
`-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`, opening a Java Debug Wire Protocol
socket so a developer's IDE can attach. This is a **development-only** flux (the image runs the `dev` profile
via `mvn spring-boot:run`); it must not be exposed in production.

**F5 — Secrets / config injection.** Configuration is delivered two ways: container environment variables
(`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) which the dev
properties resolve with `${VAR:default}` fallbacks, and application property keys (`jwt.secret`,
`jwt.expiration`) read by `JwtUtil` via `@Value`. This file references **names/keys only**; no secret values
are recorded here or in the diagram.

**F6 — Image build & run.** There is **no external CI/CD pipeline** in the repository (no `.github/`,
`.gitlab-ci.yml`, `Jenkinsfile`, or compose file). The build is the Dockerfile itself: it primes the Maven
cache with `mvn dependency:go-offline -B`, copies sources, then the container's `CMD` runs
`mvn spring-boot:run` with the `dev` profile and the JDWP agent. This is a develop-in-container workflow, not
a packaged production deployment.

---

## Acronym glossary

| Acronym | Expansion | One-line meaning |
|---|---|---|
| **REST** | Representational State Transfer | HTTP-style request/response API convention used on F1 |
| **JSON** | JavaScript Object Notation | Request/response body format on F1 |
| **JWT** | JSON Web Token | Signed bearer token proving the caller's identity (F2) |
| **HS256 / HMAC256** | HMAC with SHA-256 | Symmetric algorithm signing/verifying the JWT (F2) |
| **JDBC** | Java Database Connectivity | Java↔SQL driver protocol the app uses to reach PostgreSQL (F3) |
| **HikariCP** | Hikari Connection Pool | Spring Boot's default JDBC connection pool (F3) |
| **DDL** | Data Definition Language | The `CREATE TABLE …` schema in `V1__Init_Setup.sql` (F3) |
| **JPA** | Jakarta Persistence API | ORM abstraction (via Hibernate) over the JDBC layer (F3) |
| **JDWP** | Java Debug Wire Protocol | Remote-debug protocol exposed on port 5005 (F4) |
| **SPA** | Single Page Application | The browser client (Angular dev server) calling the API (F1) |
| **CORS** | Cross-Origin Resource Sharing | Browser policy; only `http://localhost:4200` is allowed |
| **CI/CD** | Continuous Integration / Delivery | Automated build/deploy — **not present** in this repo (F6) |

> **OIDC (OpenID Connect) is not used.** Authentication is self-issued HS256 JWTs via `java-jwt`, not an
> external identity provider — listed here only to make that absence explicit.

---

## Vestigial / not wired

- **Image object storage — not present.** The `Images` table stores only a `url TEXT` reference
  (`V1__Init_Setup.sql:11-16`); there is no S3 / MinIO / blob store and no file-upload code anywhere
  (no `MultipartFile`, no storage client). `ImageController` and `ImageService` are empty scaffolds with no
  endpoints/methods (`ImageController.java:9-15`, `ImageService.java:9-17`). No object-storage flux is drawn.
- **`prod` profile — defined but not the runtime path.** `prod/application.properties` exists, but the
  Dockerfile always launches the `dev` profile (`Dockerfile:14`). The prod profile hard-codes
  `localhost:5432` and is not wired to a deployment; treat it as scaffolded.
- **Spring Boot DevTools / LiveReload — dev convenience, not an infra flux.**
  `spring.devtools.*` (`dev/application.properties:25-29`) drives in-container hot reload; it is not a
  network flux between perimeters and is therefore not drawn as an `F#`.
- **`isTokenExpired` in `JwtUtil`** (`JwtUtil.java:69-76`) is unused by the filter path (the filter relies on
  `validateToken`); not a flux, noted for completeness.
