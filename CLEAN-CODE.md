# CLEAN-CODE.md

Focused clean-code guidance for this repository. This is the clean-code split referenced
by `AGENTS.md`; read that file for the full architecture/testing context. The rules here
are tailored to what this codebase actually is and how it is actually written, so future
changes match the local style instead of importing a foreign one.

## Language and style reference

- **Language / runtime:** Java 21 (`<java.version>21</java.version>` in `pom.xml`).
- **Framework:** Spring Boot (REST), Spring Data JPA, Spring Security, Lombok.
- **Style reference:** Google Java Style Guide. Use it as the default for anything not
  already settled by surrounding code.

### No formatter or linter is configured

`pom.xml` currently wires only the Spring Boot plugin and `maven-compiler-plugin`. There is
**no** `spotless`, `google-java-format`, `checkstyle`, `pmd`, or `spotbugs` plugin, and there
is no `.editorconfig` or `checkstyle.xml` in the repo. Nothing auto-formats or lints the code.

**Recommended fallback (not yet wired):**

- **Format:** `google-java-format` (standalone or via the **Spotless** Maven plugin).
- **Lint / static analysis:** **Checkstyle** (Google ruleset) as the baseline; optionally PMD
  or SpotBugs later.

These are recommendations only. Do **not** add them as part of an unrelated change, and do
**not** run a formatter across the whole tree in a feature PR — that produces noisy diffs and
violates "surgical changes." If/when a formatter is adopted, format the whole repo in a single
dedicated commit so the baseline is clear. Until then, match the style of the file you are in.

## Clean-code rules

These mirror the `AGENTS.md` "Code quality" section, condensed:

- **Clarity over cleverness.** Optimize for the next reader; obvious beats clever.
- **Name things well.** Intention-revealing names for variables, methods, and types. Avoid
  cryptic abbreviations and one-letter names outside tight loops.
- **Small, focused units.** Each method does one thing. Prefer early returns / guard clauses
  over deep nesting (the service layer already throws early on validation failures).
- **DRY, but not over-abstracted.** Extract a helper only when duplication is real and stable
  — not on the first repetition. Do not add interfaces, generics, or config for single-use code.
- **No magic values.** Replace unexplained numbers and strings with named constants. Local
  precedent: `JwtUtil.ISSUER = "planner-app"` and the `${jwt.expiration:86400000}` default.
- **Handle errors explicitly.** Never swallow an exception or return an empty error body. Fail
  clearly using the project's error style (see "Error-return style" below).
- **Leave no dead weight.** No unused imports or variables, no stray debug prints, no
  commented-out code, no context-free `TODO`. Remove only dead weight that is unambiguously
  yours to remove; flag anything risky instead of deleting it.
- **Comment the _why_, not the _what_.** Code shows what it does; comments explain intent and
  non-obvious decisions, and must stay accurate after edits.

## Conventions observed in this codebase

Follow these so new code matches what is already here:

- **Package-by-layer.** Code is organized by technical layer under `com.planner.app`:
  - `controller/` (plus `auth/api/` for the auth controller) — REST controllers (`@RestController`,
    `@RequestMapping("/api/...")`).
  - `service/` (plus `service/api/`) — `@Service` business logic.
  - `dao/` — Spring Data `JpaRepository` interfaces.
  - `entity/` — JPA `@Entity` classes.
  - `dto/` and `auth/api/dto/` — request/response DTOs.
  - `auth/jwt/`, `config/` — security wiring (JWT filter/util, `SecurityConfig`).
- **Constructor injection via Lombok `@RequiredArgsConstructor`.** Controllers and services
  declare `private final` collaborators and let Lombok generate the constructor. Do not use
  field `@Autowired`. (One service, `LocationService`, uses an explicit constructor; either is
  acceptable, but prefer `@RequiredArgsConstructor` for new code to match the majority.)
- **DTOs at the API boundary.** Controllers accept and return DTOs (`LoginDTO`,
  `RegisterRequest`, `UserDTO`, `LoginResponseDTO`, `VoyageDTO`), not entities directly, except
  where a controller currently returns an entity (`VoyageController` returns `Voyage`). Services
  convert entities to DTOs in private `convertToDTO(...)` helpers (see `AuthService`). DTOs use
  Lombok (`@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`).
- **Validation in the service layer.** Services throw `RuntimeException` with a meaningful
  message on a business-rule failure (e.g. `AuthService.register` throws
  `"Username already exists"` / `"Email already exists"`). Controllers catch `RuntimeException`
  and translate it to an HTTP status.
- **Error-return style.** Controllers wrap the call in `try/catch (RuntimeException e)` and
  return a `ResponseEntity` with an appropriate status:
  - When a meaningful message is available, put it in the body — either in a typed DTO field
    (`loginUser` returns `LoginResponseDTO.builder().message(e.getMessage()).build()` with `401`)
    or as the exception message itself (`registerUser` returns `e.getMessage()` with `400`).
  - Some handlers still return an empty body (`ResponseEntity.status(...).build()`, e.g.
    `VoyageController`). Prefer surfacing `e.getMessage()` for new error paths so clients can
    distinguish causes; mirror the existing handlers rather than inventing a new error shape
    (no global `@ControllerAdvice` / error-envelope type exists yet).
- **Logging.** Where logging exists it uses Lombok `@Slf4j` with parameterized messages
  (`log.error("...: {}", value)`), as in `AuthService`. Never log secrets, tokens, raw
  passwords, or full credentials.
- **Routes.** All REST endpoints live under `/api/**`; `/api/auth/**` is public, everything
  else requires a Bearer JWT (see `SecurityConfig`).

## Before finishing (this repo)

Build on JDK 21 with the Maven wrapper from the project root:

```
$env:JAVA_HOME='C:\Users\mtx\.jdks\temurin-21.0.10'; .\mvnw.cmd -q -DskipTests compile
```

- **Format / lint:** none configured — see "No formatter or linter is configured" above. State
  that they do not exist rather than inventing a command.
- **Type-check:** the Java compiler (`mvnw compile`) is the type check; it must end in
  `BUILD SUCCESS`.
- **Test:** `.\mvnw.cmd test` (no test sources exist yet; add them with new behavior per
  `AGENTS.md` / `TESTING.md`).
