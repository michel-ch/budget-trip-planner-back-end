# TESTING.md

Focused testing guide for the Budget Trip Planner backend. This is the testing
split referenced by `AGENTS.md`; read that file for the full agent guidance and
`ARCHITECTURE.md` for the system layout.

## Stack and why

| Tool | Role | Why |
|---|---|---|
| **JUnit 5 (Jupiter)** | Test engine | The ecosystem standard for Spring Boot 3 / Java 21. It is the engine that `spring-boot-starter-test` pulls in by default, so it matches what the framework and the wider community expect. |
| **Mockito** | Mocking collaborators | Lets service-layer units run with their dependencies (repositories, encoder, JWT util, auth manager) faked, so tests are fast and need no database. |
| **AssertJ** | Fluent assertions | Readable, expressive assertions (`assertThat(...).isEqualTo(...)`). |
| **MockMvc** | Web-layer testing | Exercises controllers and the Spring Security filter chain in-process, without starting a real servlet container or opening a port. |
| **spring-security-test** | Security helpers | Provides `@WithMockUser` and request post-processors to drive authenticated and unauthenticated cases through the real `SecurityConfig`. |

All of the above (except `spring-security-test`, which was already present) come
from a single dependency added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

The version is governed by the Spring Boot BOM (`spring-boot-dependencies`), so
no explicit version is declared.

## How to run

Tests run on **JDK 21** through the Maven wrapper. From the project root:

```powershell
# Windows / PowerShell
$env:JAVA_HOME='C:\Users\mtx\.jdks\temurin-21.0.10'; .\mvnw.cmd test
```

```bash
# Unix-like shells
JAVA_HOME=/path/to/jdk-21 ./mvnw test
```

JDK 21 is required: the project targets `java.version=21` and will not compile on
an older JDK. The wrapper (`mvnw` / `mvnw.cmd`) pins the Maven version, so no
local Maven install is needed.

## Layout convention

Tests live under `src/test/java`, mirroring the package of the class under test
in `src/main/java`. A test for `src/main/java/com/planner/app/foo/Bar.java` goes
in `src/test/java/com/planner/app/foo/BarTest.java`. Test classes are named
`<ClassUnderTest>Test`.

Current suite:

| Test class | Type | Covers |
|---|---|---|
| `auth/jwt/JwtUtilTest` | Pure unit (no Spring, no DB) | Token generation; `getUsernameFromToken` round-trips the subject; issuer is `planner-app`; `validateToken` accepts a fresh token and rejects garbage, a tampered token, a token signed with a different secret, and a token with the wrong issuer. |
| `service/AuthServiceTest` | Mockito unit | `register` saves a `Location` then a `User` with a BCrypt-**encoded** (never plaintext) password and returns the expected DTO; duplicate username / email throw `RuntimeException`; `login` returns a token + user on success and throws `RuntimeException` on bad credentials. |
| `auth/api/AuthControllerTest` | `@WebMvcTest` web slice | `POST /api/auth/signin` and `POST /api/auth/signup` are reachable **without** authentication; signup returns 400 with a meaningful body when the service throws. |
| `controller/VoyageControllerTest` | `@WebMvcTest` web slice | `GET /api/voyages/{id}` is rejected (401/403) without authentication and returns 200 for an authenticated user (`@WithMockUser`, service mocked). |

## DB-free approach (important)

There is **no PostgreSQL available in CI**, so the suite must run without one.
Two patterns keep it database-free:

1. **Unit tests** (`JwtUtilTest`, `AuthServiceTest`) — no Spring context at all.
   Collaborators are constructed directly or mocked with Mockito.
2. **Web-layer slices** (`@WebMvcTest`) — boot only the web layer plus the real
   `SecurityConfig`. The controller's service is `@MockBean`-ed, and the
   security filter chain's collaborators (`JwtUtil`, `UserDetailsService`, which
   the custom `JwtAuthenticationFilter` needs) are also `@MockBean`-ed so the
   chain wires up without the full application context.

**Do not use a plain `@SpringBootTest` that boots the full context** here: it
would try to open the PostgreSQL datasource configured in
`application.properties` and fail with no database present.

### Wiring note for `@WebMvcTest` + security

`SecurityConfig` is a `@Component`-driven chain: it injects the custom
`JwtAuthenticationFilter` (itself a `@Component`), which depends on `JwtUtil` and
a `UserDetailsService`. None of those are part of a web slice by default, so each
web-slice test does:

```java
@WebMvcTest(SomeController.class)
@Import(SecurityConfig.class)
class SomeControllerTest {
    @MockBean SomeService someService;      // the controller's collaborator
    @MockBean JwtUtil jwtUtil;              // filter dependency
    @MockBean UserDetailsService userDetailsService; // filter dependency
    // ...
}
```

## Path for future integration tests

When end-to-end / repository-backed coverage is needed, add a **test datasource**
before introducing full-context tests — either an in-memory **H2** database (with
a `@SpringBootTest` + `@ActiveProfiles("test")` setup pointing at H2) or
**Testcontainers** spinning up a real PostgreSQL in Docker. Only then is a full
`@SpringBootTest` appropriate. Until that infrastructure exists, keep new tests in
the unit + web-slice style above so the suite stays green without a database.

## Core rules

- **Regression test per bug.** Every bug fix ships with a test that fails on the
  broken code and passes after the fix. Reproduce first (red), then fix (green).
- **Test behavior, not implementation.** Assert on observable outputs and
  contracts (returned DTOs, HTTP status, interactions), not private internals, so
  refactors don't break tests.
- **Keep tests deterministic.** No reliance on real network, wall-clock time,
  random seeds, run order, or shared mutable state. Use obvious dummy test
  values for secrets (e.g. a fake `jwt.secret`); never hardcode real ones.
- **Cover the change and its edge cases** — happy path, edge cases, and the
  failure/error paths — rather than chasing a coverage number.
- **No `@Disabled` / skipped / assertion-free tests.** Don't weaken or delete a
  test to make the suite pass; fix the cause.
