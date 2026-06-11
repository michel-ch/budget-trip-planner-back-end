# AGENTS.md

Guidance for coding agents working in this repository. Read this first, then look
at the real directory tree before editing anything. This is a standard format read by
most coding agents; copy or symlink it to your agent's instructions file if it expects
a different name. This file is the complete, self-contained version.

## Project facts

These are discovered from `pom.xml`, `Dockerfile`, `mvnw` / `mvnw.cmd`, `.gitignore`,
and the ground-truth notes under `docs/_detect/`. Do not invent commands that don't
exist in the build.

- **Language / runtime:** Java 21 (`java.version`, `maven.compiler.source/target` in
  `pom.xml`).
- **Framework:** Spring Boot 3.5.6 (Spring Web, Spring Security, Spring Data JPA /
  Hibernate); PostgreSQL via the `org.postgresql:postgresql` driver. Auth uses
  `com.auth0:java-jwt` 4.4.0; Lombok for boilerplate. No `spring-boot-starter-parent`;
  the Spring Boot BOM (`spring-boot-dependencies:3.5.6`) is imported via
  `dependencyManagement`.
- **Package manager / build:** Maven via the wrapper (`mvnw` on Linux/macOS,
  `mvnw.cmd` on Windows). BUILD NOTE: this project targets Java 21; the system default
  JDK may be older, so a JDK 21 is required. Set `JAVA_HOME` to a JDK 21 install before
  invoking the wrapper.
- **Run locally:** `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`). The
  `dev` Maven profile is active by default and `server.port=8080`. A `prod` profile
  exists (`./mvnw -Pprod ...`). Docker: the provided `Dockerfile` builds and runs the
  app in the `dev` profile, exposing `8080` (app) and `5005` (JDWP debug); it does NOT
  start a database (PostgreSQL must be provided separately).
- **Lint:** none configured in the build. There is no Checkstyle, Spotless, PMD, or
  SpotBugs plugin in `pom.xml`. The community fallback for Java
  (`google-java-format` / Spotless + Checkstyle) is recommended but NOT wired up here;
  do not run or claim a lint command that the build does not define.
- **Test:** `./mvnw test` (Windows: `mvnw.cmd test`). Uses JUnit / Spring Boot Test
  (`spring-security-test` is the only test-scoped dependency declared). Note: a test
  suite is being added as part of this same effort; historically no `src/test`
  directory existed.
- **Type-check:** no separate step. Compilation is the check:
  `./mvnw -DskipTests compile` (Windows: `mvnw.cmd -DskipTests compile`).

## Navigation

- **Code is the source of truth.** The `docs/` folder holds intent, architecture, and
  detection notes; use it for orientation, but when docs and code disagree, trust the
  code and flag the mismatch. Never prefer documentation over the actual source.
- **Read the minimum.** Find the smallest set of files that completes the task. Don't
  read the whole repo for a one-file change, and don't read ten files to infer a
  convention you can confirm in one.
- **Reuse context.** Don't re-open a file you've already read this session unless you
  suspect it changed.
- **Target your searches.** Prefer direct reads when the location is known or
  documented; fall back to a wide `grep`/`find` only when you can't locate it otherwise.

Actual layout of this repository:

```
src/main/java/com/planner/app/
  Application.java          @SpringBootApplication entry point (logs profile/port/datasource)
  auth/
    api/                    AuthController (/api/auth: signin, signup) + auth DTOs
    jwt/                    JwtAuthenticationFilter, JwtUtil (stateless Bearer-token auth)
  config/                   SecurityConfig (filter chain, CORS, BCrypt, providers)
  controller/               REST controllers per domain: Voyage, Expense, Image,
                            Itinerary, Location, TravelGroup, User
  service/                  @Service business layer (one per domain) + AuthService
    api/                    CustomUserDetailsService (UserDetailsService impl)
  dao/                      Spring Data JPA repositories (JpaRepository) per domain
  entity/                   JPA entities: Voyage, Expense, Image, Itinerary, Location,
                            TravelGroup, User
  dto/                      request/response DTOs: VoyageDTO, UserDTO

src/main/resources/
  dev/                      dev application.properties (GIT-IGNORED credentials folder)
  prod/                     prod application.properties (GIT-IGNORED credentials folder)
  db/                       V1__Init_Setup.sql, V1__Add_Data.sql (hand-written schema/seed)

docs/                       architecture/intent notes and diagrams (supporting context)
  _detect/                  ground-truth detection notes (stack, architecture, services, gaps, ...)
  diagrams/                 Mermaid diagrams (.mmd)
  infra/                    infrastructure notes + infra-flux.drawio
  images/                   rendered diagram PNGs
```

Notes specific to this repo:
- `src/main/resources/dev/` and `src/main/resources/prod/` are **git-ignored
  credential folders** (`### Credentials ###` in `.gitignore`). Their
  `application.properties` (DB credentials, JWT secret) are intentionally untracked, so
  a fresh clone has no profile config. Never commit real values into them.
- `.mvn` is git-ignored, so the wrapper's `maven-wrapper.properties` (which pins the
  Maven distribution version) is not present in the tracked repo.
- The seven domains (`Voyage`, `Expense`, `Image`, `Itinerary`, `Location`,
  `TravelGroup`, `User`) each have a matching controller, service, repository, and
  entity. Only the `Voyage` slice and the auth flow are fully implemented; the other
  domain controllers/services are scaffolding (empty bodies) for now.

## Scan vs. open on demand

The paths below are noisy in bulk and rarely worth searching across. Keep them **out of
wide searches**, but open a specific file when the task genuinely needs it.

- **Build output:** `target/` (compiled classes, the repackaged jar, generated sources)
- **Build logs:** `baseline-build.log` and any `*.log` *(only when the task is about them)*
- **Rendered media:** `docs/images/*.png` *(open one if you need to confirm a diagram)*
- **Git-ignored credential folders:** `src/main/resources/{dev,prod}/` *(open the
  property file only when you must confirm a config key; never scan in bulk, never
  commit)*
- **Tooling / editor:** `.idea/`, `.vscode/`, `.settings`, `.mvn/`

## Architecture diagram (Mermaid -> draw.io)

When asked to document or map the architecture (or when a structural change makes an
existing diagram stale), generate a diagram of the system **as it actually is in the
code** -- read entry points (`Application.java`), controller `@RequestMapping` routes,
`SecurityConfig`, the entities, and the profile property files; don't diagram from
assumptions.

Diagrams already present in this repo (keep them in sync rather than starting over):

- **System view (primary):** `docs/architecture.mmd` (Mermaid) and
  `docs/architecture.drawio` (draw.io) -- the system-view architecture, rendered to
  `docs/images/architecture.png`. Supporting component/service maps live under
  `docs/diagrams/` (`backend-architecture.mmd`, `backend-services.mmd`,
  `request-lifecycle.mmd`, `auth-flow.mmd`).
- **Detailed data model:** `docs/diagrams/db-schema.mmd` (`erDiagram`) plus
  `docs/diagrams/db-architecture.mmd` and `docs/diagrams/entity-lifecycle.mmd`.
- **Infrastructure:** `docs/infra/infra-flux.drawio` (draw.io) and
  `docs/diagrams/infra-architecture.mmd` (Mermaid). Notes in `docs/infra/infra-flux.md`.

**What to include** (only the parts that exist in this project):

- **Program structure** -- the layered REST monolith: controllers (`/api/**`) ->
  `@Service` business layer -> Spring Data JPA repositories (`dao`) -> Hibernate ->
  PostgreSQL. Constructor injection throughout (Lombok `@RequiredArgsConstructor`).
- **Database** -- a single PostgreSQL datastore (`tripbudgetplanner`), the seven
  entities and their relationships. Use the `erDiagram` in `docs/diagrams/db-schema.mmd`
  for the detailed schema.
- **External boundaries** -- the API exposed under `/api`; PostgreSQL is the only
  external system (no S3/object storage, no message queue, no outbound HTTP, no mail).
- **Security features** -- stateless JWT: `JwtAuthenticationFilter` -> `JwtUtil` +
  `CustomUserDetailsService`, configured in `SecurityConfig` (CSRF off, CORS for
  `http://localhost:4200`, `STATELESS` sessions, `/api/auth/**` and `OPTIONS`
  permitAll, everything else authenticated). Mark the public / app / data trust zones
  explicitly with `subgraph`s.

**How to produce it:**

1. Write the diagram as Mermaid first -- `flowchart`/`graph` for the system view,
   `erDiagram` for the data model. Group related components with `subgraph`s; label
   edges with the protocol or action (HTTP, SQL, validates...).
2. Save Mermaid under `docs/diagrams/` (the existing convention), e.g. update the
   matching `*.mmd` file rather than adding a parallel one.
3. **For draw.io output**, follow the existing infra convention (`docs/infra/*.drawio`):
   write a valid draw.io XML file (`<mxfile>` / `<mxGraphModel>` with `mxCell` nodes and
   edges) that mirrors the Mermaid diagram -- one `mxCell` per component (vertex) and per
   relationship (edge), grouped/styled to match the subgraphs. As a fallback, draw.io can
   import Mermaid natively (Extras -> *Edit Diagram*).
4. Verify both: the Mermaid must parse, and the draw.io XML must be well-formed.
   Diagrams that don't open are worse than none.
5. Keep them in sync with reality: if your change alters the architecture, update the
   matching diagram(s) in the same change.

## Conventions

- **Stay in scope.** Keep changes to what the task requires. No drive-by refactors
  unless explicitly asked.
- **Respect module boundaries.** Keep the layering intact: controllers handle HTTP and
  DTO mapping, services hold business logic and `@Transactional` boundaries,
  repositories handle persistence. Don't reach across layers (e.g. a controller calling
  a repository directly) unless the task requires it.
- **Match local style.** Follow the patterns already used: constructor injection via
  Lombok `@RequiredArgsConstructor`, `@RestController` + `@RequestMapping("/api/...")`
  routes, `JpaRepository<Entity, Integer>` repositories, and the `auth` / `config` /
  `controller` / `service` / `dao` / `entity` / `dto` package split. Discover
  conventions from a neighboring domain slice (the `Voyage` slice is the most complete)
  before introducing a new one.

## Code quality

Write clean, idiomatic code -- the kind an experienced Java/Spring developer would write
and approve in review. Code that merely works is not done; it must also be readable,
consistent, and conventional.

### Clean code

- **Clarity over cleverness.** Optimize for the next reader. Obvious beats clever.
- **Name things well.** Intention-revealing names for variables, methods, and types; no
  cryptic abbreviations or one-letter names outside tight loops.
- **Small, focused units.** Each method does one thing. Prefer early returns / guard
  clauses over deep nesting.
- **Don't repeat yourself -- but don't over-abstract.** Extract a helper when
  duplication is real and stable, not on the first repetition.
- **No magic values.** Replace unexplained numbers and strings with named constants
  (or config keys where appropriate).
- **Handle errors explicitly.** Don't swallow exceptions or return null where the
  caller can't tell; fail clearly and let Spring's exception handling surface the right
  HTTP status.
- **Leave no dead weight.** No unused imports or variables, no stray debug prints/logs,
  no commented-out code, no `TODO` without context. (Note: `ImageController` has a
  known stale `ExpenseService` import documented in `docs/_detect/services.md`; if you
  touch that file, removing it is in scope.)
- **Comment the _why_, not the _what_.** The code shows what it does; comments explain
  intent, trade-offs, and non-obvious decisions -- and stay accurate after edits.

### Language standards

- **Follow Java/Spring idioms and the project's existing patterns.** Write code the way
  this codebase already does (constructor injection, layered packages, JPA repositories)
  rather than porting patterns from another stack.
- **The project's config is authoritative.** Honor `pom.xml` settings (Java 21,
  `parameters=true`, UTF-8 source encoding). Don't reformat unrelated code.
- **No formatter or linter is configured in this build.** Match the surrounding style by
  hand. If formatting tooling is ever adopted, `google-java-format` + Spotless and
  Checkstyle are the recommended fallback -- but do not assume or run them today.

| Language | Standard tooling (format / lint / types) | Style reference | In this repo |
|---|---|---|---|
| Java | `google-java-format` / Spotless · Checkstyle, PMD, SpotBugs · `javac` | Google Java Style | **Authoritative:** no formatter/linter configured; `google-java-format` + Checkstyle are the fallback. Compilation (`mvnw compile`) is the only enforced check. |

For SQL, infra, and markup files that also appear here:

| Area | Standard tooling | Style reference | In this repo |
|---|---|---|---|
| SQL | `sqlfluff` / sql-formatter | one dialect (PostgreSQL), stay consistent | `src/main/resources/db/V*.sql`; not wired to any migration tool (Hibernate `ddl-auto` governs schema). |
| Dockerfile | hadolint | best-practice conventions | single root `Dockerfile`; none configured. |
| YAML / JSON / properties | yamllint / project convention | schema / project convention | profile `application.properties` under git-ignored `dev/` and `prod/`. |
| Markdown | markdownlint | project convention | `docs/**` and this file; none configured. |

Whatever the project already uses wins over these tables. Today, that means: no
automated style enforcement -- match the existing code.

## Dependencies & security

- **Prefer what's already installed.** Solve problems with the JDK standard library or
  the project's existing dependencies (Spring Web, Spring Security, Spring Data JPA,
  Lombok, java-jwt) first. Add a new dependency only when it clearly beats writing the
  code, and say what you added and why. Never swap out a core dependency (Spring Boot,
  Hibernate, the JWT library) on your own initiative.
- **Use Maven to manage dependencies.** Edit `pom.xml` and let the Spring Boot BOM
  govern versions where possible (this project has no parent POM; versions resolve from
  `spring-boot-dependencies:3.5.6`). Pin a version explicitly only when the BOM doesn't
  cover it (as with `java-jwt`).
- **Never hardcode secrets.** No DB passwords, JWT secrets, tokens, or connection
  strings in code, tests, fixtures, or examples. The real values live only in the
  git-ignored `src/main/resources/{dev,prod}/application.properties`. The `dev` profile
  reads `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` /
  `SPRING_DATASOURCE_PASSWORD` from the environment with localhost fallbacks; prefer
  environment variables and placeholders (`YOUR_DB_PASSWORD`) in any docs or samples.
- **Don't commit the credential folders.** `src/main/resources/{dev,prod}/` are
  git-ignored on purpose; don't add them to git or reproduce their secret values
  anywhere.
- **Validate at the boundaries.** Treat HTTP input (request bodies, path/query params)
  as untrusted. Keep using JPA/parameterized queries (the repositories and JPQL
  `@Query` are parameterized -- don't build SQL by string concatenation). Don't disable
  security checks: leave the JWT filter, `SecurityConfig` rules, and
  `BCryptPasswordEncoder` in place; don't loosen CORS or auth rules to make something
  work.
- **Don't log sensitive data.** Keep passwords, JWT secrets/tokens, and personal data
  out of log statements and error messages. Note the `dev` profile already logs SQL and
  `com.planner.app` at `DEBUG` -- don't add credential values to that output.

## Testing

Add or update tests for every change that affects behavior. Choose the test types that
fit the code you touched -- don't apply all of them everywhere, and don't skip the ones
that matter.

A test suite is being introduced in this same effort. Use JUnit + Spring Boot Test
(`spring-security-test` is already available for security-aware tests); if you need the
broader Spring testing utilities, add `spring-boot-starter-test` (test scope) via
`pom.xml`. Run tests with `./mvnw test` (Windows: `mvnw.cmd test`).

### Core rules

- **Every bug fix gets a regression test.** Write a test that fails on the broken code
  and passes after your fix. Put it next to the related tests and reference the
  bug/issue in the test name or a short comment.
- **Reproduce before you fix.** For a reported bug, write the failing test first (red),
  then make it pass (green). Don't fix blind.
- **Every new feature or public method ships with tests** covering the happy path, the
  edge cases, and the failure/error paths.
- **Test behavior, not implementation.** Assert on observable outputs and contracts
  (HTTP status, response body, persisted state), not on private internals, so refactors
  don't break the tests.
- **Match the setup.** Mirror `src/main/java/com/planner/app/...` under
  `src/test/java/com/planner/app/...` (Maven's standard test source root). Use the
  framework, naming, and helpers the suite settles on; don't introduce a second test
  framework.
- **Keep tests deterministic.** No reliance on real network, wall-clock time, random
  seeds, run order, or shared mutable state. For DB-touching code, prefer an in-memory
  or test-scoped datasource over the developer's real PostgreSQL; control time and
  randomness. A test that passes or fails at random is a bug -- fix or remove it.

### Which tests, by what you touched

| If the code is... | Write... |
|---|---|
| Pure logic in a service, mapping helpers, `JwtUtil` token logic | **Unit tests** -- inputs -> outputs, edge cases, error cases |
| A fixed bug | **Regression test** -- fails before the fix, passes after |
| Service + repository + DB together | **Integration tests** -- a test datasource (in-memory or test-scoped), not full mocks |
| A REST endpoint under `/api/**` | **Contract / API tests** -- status codes, payload shape, error format (e.g. `MockMvc`) |
| Auth, the JWT filter, `SecurityConfig` rules, validation | **Security-focused tests** -- rejected/forbidden cases, expired/invalid tokens, boundary and malicious inputs (use `spring-security-test`) |
| Schema / seed SQL or entity mapping | **Migration & data-integrity tests** -- mapping round-trips, no data loss |

If a change spans several of these, cover each relevant layer rather than piling
everything into one oversized test.

### Don't

- Don't weaken, delete, or skip an existing test to make the suite pass. Fix the cause
  -- or, if behavior intentionally changed, update the test and say why.
- Don't write tests that can't fail (no real assertions, or `assertTrue(true)`).
- Don't chase a coverage number for its own sake. Cover the change and its edge cases
  well; meaningful coverage beats a high percentage.
- Don't leave disabled (`@Disabled`) or commented-out tests behind.
- Don't test Spring or the JDK itself -- test this project's own code.

## Before finishing

Run the project's real checks and fix any failures -- don't skip them or work around
them. Use the wrapper (`./mvnw` on Linux/macOS, `mvnw.cmd` on Windows), with a JDK 21
on `JAVA_HOME`:

```
# format  -> not configured in this project (no Spotless/google-java-format plugin); skip
# lint    -> not configured in this project (no Checkstyle/PMD/SpotBugs plugin); skip
./mvnw test                 # run the test suite (JUnit / Spring Boot Test)
./mvnw -DskipTests compile  # type-check via compilation (no separate type-check step)
```

A full build that also runs tests and repackages the jar:

```
./mvnw clean package
```

If a check genuinely doesn't exist in this project (format, lint), say so instead of
inventing one.

If you changed behavior, confirm the suite includes a test that covers it, and that the
test fails without your change and passes with it.
