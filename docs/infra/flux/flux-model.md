# Flux Model (shared master) - Budget Trip Planner back-end

> **This is the shared master for the multi-view flux diagram set** (`docs/infra/flux/`).
> It is the L0 / index owner: the component-id registry, the perimeter palette, the master
> `F1..F6` flux table, the view index, and the shared glossary that every view in this set
> reuses. The diagrams (`context.drawio`, `runtime.drawio`, `ops.drawio`) are **filtered
> views** of this model; their `F#` IDs and component node IDs are identical to those defined
> here. The authoritative, evidence-grounded narrative lives in `docs/infra/infra-flux.md` -
> this file condenses it but keeps every `F#` text consistent with it.
>
> **Deployment context:** on-prem / Docker. No cloud provider, no managed services, no
> external CI/CD pipeline file. Generic draw.io shapes only.
> **Secrets policy:** only environment-variable *names* and property *keys* appear here -
> never their values.

---

## (a) Component-id registry

These node IDs are **stable and global** - the same `mxCell` id is used in every diagram of the set.

| id | Name | Perimeter |
|----|------|-----------|
| `client` | Browser SPA (Angular dev, `http://localhost:4200`) | Client / External |
| `dev` | Developer IDE / debugger | Client / External |
| `app` | Spring Boot app (Java 21, Spring Boot 3.5.6, `:8080`) | Application container (Docker) |
| `sec` | Spring Security filter chain (`JwtAuthenticationFilter` + `JwtUtil`) | Application container (Docker) - sub-zone inside `app` |
| `cfg` | Config / Secrets source (container env vars + property keys) | Application container (Docker) - Secrets / Config sub-zone |
| `build` | Docker image build / Maven (`mvn spring-boot:run`) | Application container (Docker) - Build sub-zone |
| `db` | PostgreSQL `tripbudgetplanner` `:5432` | Data tier |

> The Context (L0) view collapses `sec` into the single `app` system node (`sec` is a sub-zone
> drawn explicitly only in the Runtime view). `cfg` and `build` are shown in Context inside an
> Application-colored "Build & Config" sub-zone adjacent to `app`.

---

## (b) Perimeter palette

| Perimeter | Node fill / stroke | Mega-box bg / stroke |
|-----------|--------------------|----------------------|
| Client / External | `#DAE8FC` / `#6C8EBF` | `#EAF2FD` / `#6C8EBF` |
| Application container (Docker) | `#FFE6CC` / `#D79B00` | `#FFF7E6` / `#D79B00` |
| Data tier | `#D5E8D4` / `#82B366` | (`#E9F5E8` / `#82B366`) |
| Sub-zone (function grouping) | `fillColor=none`, stroke `#bbbbbb`, dashed | - |

Edges: **solid** = runtime / primary (F1, F2, F3); **dotted** = secondary (F4 ops, F5
secrets-config, F6 cicd). All arrows: `edgeStyle=orthogonalEdgeStyle;rounded=1;endArrow=block;
strokeColor=#000000;strokeWidth=1.5;labelBackgroundColor=#ffffff;` with short `F# + 2-3 word`
labels and distinct exit/entry fractions.

---

## (c) Master flux table (F1-F6)

Condensed from `docs/infra/infra-flux.md`; `F#` text kept consistent. `file:line` evidence is
authoritative there.

| F# | Name | Category | Style | Source -> Target | Protocol | Auth | Notes |
|----|------|----------|-------|------------------|----------|------|-------|
| **F1** | HTTP REST runtime | runtime | solid | `client` -> `app` `:8080` `/api/**` | HTTP REST/JSON | Bearer JWT (except `/api/auth/**`, OPTIONS) | Plain HTTP in this on-prem/dev setup (no TLS in repo); CORS origin `http://localhost:4200`. |
| **F2** | JWT auth (stateless) | auth | solid | `client` -> `app` (`sec`: `JwtAuthenticationFilter` -> `JwtUtil`) | HTTP header on F1 | HS256 JWT, issuer `planner-app` | `SessionCreationPolicy.STATELESS`; not a separate hop - rides the F1 edge. |
| **F3** | JDBC persistence | data | solid | `app` (HikariCP) -> `db` `:5432/tripbudgetplanner` | JDBC (PostgreSQL driver) | DB user/pass via `SPRING_DATASOURCE_*` env vars | JPA/Hibernate over schema in `V1__Init_Setup.sql`. |
| **F4** | JDWP remote debug *(dev only)* | ops | dotted | `dev` -> `app` JVM debug agent `:5005` | JDWP over `dt_socket` (TCP) | None (open dev socket) | `-agentlib:jdwp=...address=*:5005`; must not be exposed in production. |
| **F5** | Secrets / config injection | secrets-config | dotted | `cfg` (env vars + property keys) -> `app` | Process env + property resolution | n/a (delivery channel) | `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD` + `jwt.secret`/`jwt.expiration` (names/keys only). |
| **F6** | Image build & run | cicd | dotted | `build` (Dockerfile/Maven) -> `app` | Maven / Docker build | n/a | `mvn dependency:go-offline` then `mvn spring-boot:run` (dev profile); **no external CI/CD pipeline** detected. |

**One-liners (kept identical to the draw.io description panels):**

- **F1** HTTP REST runtime [solid] - Browser SPA calls Spring Boot app on `:8080` under `/api/**`; plain HTTP REST/JSON (no TLS in repo). All routes require auth except `/api/auth/**` and CORS OPTIONS.
- **F2** JWT auth, stateless [solid] - Each request's `Authorization: Bearer <JWT>` passes `JwtAuthenticationFilter`; `JwtUtil` verifies HMAC256 + issuer `planner-app`. `SessionCreationPolicy.STATELESS`; rides on the F1 edge.
- **F3** JDBC persistence [solid] - App (HikariCP pool) reads/writes PostgreSQL at `:5432/tripbudgetplanner` via the PostgreSQL JDBC driver; DB creds from `SPRING_DATASOURCE_*` (see F5).
- **F4** JDWP remote debug, DEV ONLY [dotted] - Dockerfile exposes `:5005` and runs the JVM with `-agentlib:jdwp` (`dt_socket`); a developer IDE attaches. Must not be exposed in production.
- **F5** Secrets / config injection [dotted] - Container env vars (`SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`) and property keys (`jwt.secret`, `jwt.expiration`) resolve into the Spring context at startup. Names/keys only - never values.
- **F6** Image build & run [dotted] - Dockerfile primes the Maven cache (`mvn dependency:go-offline`) then `CMD` runs `mvn spring-boot:run` (dev profile). No external CI/CD pipeline detected.

---

## (d) View index (the map to the set)

| View | File | Carries | Audience / purpose |
|------|------|---------|--------------------|
| **Context (L0)** | `context.drawio` (source: `context.mmd`) | **F1-F6** top-level | System as one box + external actors; exec/context index map. |
| **Runtime** | `runtime.drawio` (source: `runtime.mmd`) | **F1, F2, F3** | The user request path: client -> app (`sec` sub-zone) -> db. |
| **Ops** | `ops.drawio` (source: `ops.mmd`) | **F4, F5, F6** | Debug, secrets/config injection, build & run lifecycle. |

Every `F#` in the master table appears in at least one view; no view renumbers an `F#` or a
component id. Context is the entry point and links the other two.

---

## (e) Shared glossary

| Acronym | Expansion | One-line meaning |
|---------|-----------|------------------|
| **REST** | Representational State Transfer | HTTP-style request/response API convention used on F1 |
| **JSON** | JavaScript Object Notation | Request/response body format on F1 |
| **JWT** | JSON Web Token | Signed bearer token proving the caller's identity (F2) |
| **JDBC** | Java Database Connectivity | Java-to-SQL driver protocol the app uses to reach PostgreSQL (F3) |
| **JDWP** | Java Debug Wire Protocol | Remote-debug protocol exposed on port 5005 (F4) |
| **SPA** | Single Page Application | The browser client (Angular dev server) calling the API (F1) |
| **CORS** | Cross-Origin Resource Sharing | Browser policy; only `http://localhost:4200` is allowed |
| **CI/CD** | Continuous Integration / Delivery | Automated build/deploy - **not present** in this repo (F6) |
