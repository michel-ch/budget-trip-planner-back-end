# Backend Architecture — Detection Notes

Internal grounding notes for the backend architecture overview diagram. Every statement is traced to a source file. No secret values are reproduced here.

## Application

- Single Spring Boot monolith. Entry point `com.planner.app.Application` (`Application.java`) annotated `@SpringBootApplication`; `main` calls `SpringApplication.run(...)` and logs active profile, server port, and database URL on startup.
- API is served under the `/api` base path (startup log in `Application.java`; controller `@RequestMapping` values such as `/api/voyages` and `/api/auth`).

## Package structure (`com.planner.app`)

Layered, package-by-type layout confirmed by the source tree:

- `auth` — authentication module.
  - `auth.api.AuthController` (`@RestController`, `@RequestMapping("/api/auth")`): `POST /api/auth/signin`, `POST /api/auth/signup`.
  - `auth.api.dto` — `LoginDTO`, `LoginResponseDTO`, `RegisterRequest`.
  - `auth.jwt.JwtAuthenticationFilter`, `auth.jwt.JwtUtil`.
- `config` — `SecurityConfig` (Spring Security wiring).
- `controller` — REST controllers (`@RestController`): `Voyage`, `Expense`, `Image`, `Itinerary`, `Location`, `TravelGroup`, `User`.
- `service` — business layer (`@Service`): one service per domain plus `AuthService` and `service.api.CustomUserDetailsService`.
- `dao` — Spring Data JPA repositories (`@Repository`, extend `JpaRepository`): one per domain.
- `entity` — JPA entities: `Voyage`, `Expense`, `Image`, `Itinerary`, `Location`, `TravelGroup`, `User`.
- `dto` — request/response DTOs: `VoyageDTO`, `UserDTO`.

### Seven domains

`Voyage`, `Expense`, `Image`, `Itinerary`, `Location`, `TravelGroup`, `User` — each has a matching controller, service, repository, and entity (confirmed from the file listing of `controller/`, `service/`, `dao/`, and `entity/`).

## Layered request flow

Grounded in the representative `Voyage` vertical slice:

1. `VoyageController` (`@RestController`, `@RequestMapping("/api/voyages")`) receives the HTTP request; `createVoyage` accepts a `VoyageDTO` (`@RequestBody`) and maps its fields onto a `Voyage` entity before delegating.
2. `VoyageService` (`@Service`, `@Transactional`, constructor-injected `VoyageRepository`) holds the business logic (`getVoyageById`, `createVoyage`).
3. `VoyageRepository` (`@Repository extends JpaRepository<Voyage, Integer>`) performs persistence; includes a JPQL `@Query`.
4. Hibernate/JPA maps entities to PostgreSQL.

DTO mapping exists where DTOs are present (`VoyageDTO`, `UserDTO`): controllers translate between request bodies and entities; entities are returned to the client (e.g. `VoyageController` returns `ResponseEntity<Voyage>`).

Dependency injection is constructor-based throughout via Lombok `@RequiredArgsConstructor` (seen in `SecurityConfig`, `VoyageController`, `VoyageService`, `JwtAuthenticationFilter`, `AuthController`).

## Security model (stateless JWT)

From `config/SecurityConfig.java` and `auth/jwt/JwtAuthenticationFilter.java`:

- `@Configuration @EnableWebSecurity`. `SecurityFilterChain` bean:
  - CSRF disabled; CORS enabled (allowed origin `http://localhost:4200`, credentials allowed, exposes `Authorization` header).
  - Authorization rules: `OPTIONS /**` permitAll (preflight); `/api/auth/**` permitAll (login/register); `anyRequest().authenticated()`.
  - Session policy `STATELESS` — no server-side HTTP session.
  - Custom `DaoAuthenticationProvider` using `UserDetailsService` + `BCryptPasswordEncoder`.
  - `JwtAuthenticationFilter` registered `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)` — it is the entry gate.
- `JwtAuthenticationFilter extends OncePerRequestFilter`: reads the `Authorization: Bearer <token>` header, extracts the username via `JwtUtil.getUsernameFromToken`, loads the user via `UserDetailsService`, validates with `JwtUtil.validateToken`, and on success populates `SecurityContextHolder`.
- JWT issuance is on the auth path: `AuthController` -> `AuthService` (`login` / `register`), with `JwtUtil` producing the token. `/api/auth/**` is unauthenticated so tokens can be obtained.
- JWT config keys present in both profiles: `jwt.secret`, `jwt.expiration=86400000` (~24h). Secret value intentionally omitted.

## Profiles (dev / prod split)

Two profile-specific property files under `src/main/resources`:

- `dev/application.properties`: app name `budget_trip_planner`; datasource URL/username/password sourced from env vars with localhost defaults; `spring.jpa.hibernate.ddl-auto=update`; `show-sql=true`; verbose logging (`com.planner.app=DEBUG`, Hibernate SQL `DEBUG`); Spring DevTools hot reload enabled (`restart`, `livereload`).
- `prod/application.properties`: app name `budget`; `spring.profiles.active=prod`; static datasource config; `spring.jpa.hibernate.ddl-auto=validate`; `show-sql=false`; reduced logging (`root=WARN`, `com.planner.app=INFO`); no DevTools.
- Common to both: `server.port=8080`; PostgreSQL driver `org.postgresql.Driver`; `hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect`.

## Datastore

- PostgreSQL, database `tripbudgetplanner` at `localhost:5432` (default in dev, explicit in prod). Accessed via JDBC through Spring Data JPA / Hibernate using `PostgreSQLDialect`.

## Ports

- `8080` — application HTTP (both profiles, `server.port=8080`).
- `5005` — JDWP remote debug, exposed by the Dockerfile (`EXPOSE 8080 5005`, JVM arg `-agentlib:jdwp=...address=*:5005`).
- `5432` — PostgreSQL (datasource URL).
- `4200` — allowed CORS origin (front-end), not a backend listen port.

## Build / run model

From `Dockerfile`:

- Base image `maven:3.9-eclipse-temurin-21-alpine` (Maven build, JDK 21).
- Layered build: copy `pom.xml`, run `mvn dependency:go-offline -B`, then copy `src`.
- `EXPOSE 8080 5005`.
- Runtime command: `mvn spring-boot:run` with `-Dspring-boot.run.profiles=dev`, JDWP JVM args on `*:5005`, and `spring-boot.run.fork=false` — runs the `dev` profile with DevTools hot reload and a remote-debug socket.
- Maven is the build tool (`pom.xml` present); Spring Boot is the runtime; Docker provides the container packaging.
