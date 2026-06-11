# Backend Service / Component Detection Notes

Project: Budget Trip Planner (Spring Boot backend)
Base package: `com.planner.app`
Entry point: `Application` (`@SpringBootApplication`)

## Architecture style

Classic **layered REST monolith**. A single Spring Boot application exposes
REST controllers under `/api/**`, delegates to a `@Service` business layer, which
persists through Spring Data JPA repositories (the `dao` package) to a single
**PostgreSQL** database. Wiring is done almost entirely via constructor injection
(Lombok `@RequiredArgsConstructor`, or an explicit constructor in `LocationService`).

Cross-cutting security is a stateless JWT chain
(`JwtAuthenticationFilter` -> `JwtUtil` + `CustomUserDetailsService`) configured in
`SecurityConfig`. Sessions are `STATELESS`; `/api/auth/**` and CORS preflight
(`OPTIONS`) are `permitAll`, everything else requires authentication.

## Controller -> Service -> Repository map

| Controller (route) | Service(s) wired | Repository(ies) reached | Responsibility (one line) |
|---|---|---|---|
| `AuthController` (`/api/auth`) | `AuthService` | `UserRepository`, `LocationRepository` | Sign in / sign up; issues JWT and creates user + location on registration. |
| `UserController` (`/api/users`) | `UserService` | `UserRepository` | User domain endpoints (controller body currently empty). |
| `VoyageController` (`/api/voyages`) | `VoyageService` | `VoyageRepository` | Get a voyage by id and create a voyage (only fully-implemented domain controller). |
| `ExpenseController` (`/api/expenses`) | `ExpenseService` | `ExpenseRepository` | Expense domain endpoints (controller body currently empty). |
| `ImageController` (`/api/images`) | `ImageService` | `ImageRepository` | Image-reference domain endpoints (controller body currently empty). |
| `ItineraryController` (`/api/itineraries`) | `ItineraryService` | `ItineraryRepository` | Itinerary domain endpoints (controller body currently empty). |
| `LocationController` (`/api/locations`) | `LocationService` | `LocationRepository` | Location domain endpoints (controller body currently empty). |
| `TravelGroupController` (`/api/travelgroups`) | `TravelGroupService` | `TravelGroupRepository` | Travel-group domain endpoints (controller body currently empty). |

### Security-chain components (not REST controllers)

| Component | Collaborators | Repository | Responsibility |
|---|---|---|---|
| `JwtAuthenticationFilter` (`OncePerRequestFilter`) | `JwtUtil`, `UserDetailsService` | (via `CustomUserDetailsService`) | Extracts `Bearer` token, validates it, loads the user, populates the `SecurityContext`. |
| `CustomUserDetailsService` (`UserDetailsService`) | — | `UserRepository` | Loads a `UserDetails` by username/email for the `DaoAuthenticationProvider` and the JWT filter. |
| `SecurityConfig` | `JwtAuthenticationFilter`, `UserDetailsService` | — | Defines the filter chain, CORS, `BCryptPasswordEncoder`, `DaoAuthenticationProvider`, `AuthenticationManager`. |

## Service detail

| Service | Repositories injected | Notes on responsibility |
|---|---|---|
| `AuthService` | `UserRepository` + `LocationRepository` | Also injects `JwtUtil`, `PasswordEncoder`, `AuthenticationManager`. `register()` saves a `Location` then a `User`; `login()` authenticates and issues a token. |
| `UserService` | `UserRepository` | No methods implemented yet. |
| `VoyageService` | `VoyageRepository` | `getVoyageById`, `createVoyage`. |
| `ExpenseService` | `ExpenseRepository` | No methods implemented yet. |
| `ImageService` | `ImageRepository` | No methods implemented yet. |
| `ItineraryService` | `ItineraryRepository` | No methods implemented yet. |
| `LocationService` | `LocationRepository` | Constructor injection (no Lombok); no methods implemented yet. |
| `TravelGroupService` | `TravelGroupRepository` | No methods implemented yet. |

All repositories are Spring Data `JpaRepository<Entity, Integer>`; only `UserRepository`
(custom `@Query` lookups + existence checks) and `VoyageRepository` (`findById`) add
queries beyond the CRUD defaults.

## Non-1:1 wirings found

The map is **not** a clean 1 controller -> 1 service -> 1 repository for every domain:

1. **`AuthService` touches two repositories.** It autowires both
   `UserRepository` and `LocationRepository` (registration creates a `Location`
   row before the `User` row). This is the only service that spans more than one
   repository.
2. **`AuthController` -> `AuthService`** is a dedicated auth flow, not one of the
   seven domain CRUD controllers; `AuthService` additionally collaborates with the
   security beans (`JwtUtil`, `PasswordEncoder`, `AuthenticationManager`).
3. **`CustomUserDetailsService` also reads `UserRepository`,** so `UserRepository`
   has two consumers (`AuthService` and the security chain) in addition to `UserService`.
4. **`ImageController` has a stale import.** It imports both
   `com.planner.app.service.ExpenseService` and
   `com.planner.app.service.ImageService`, but only declares a `final ImageService`
   field. There is **no `ExpenseService` dependency** in `ImageController` — the
   import is unused. The diagram therefore wires `ImageController` to `ImageService`
   only. (Minor dead import, noted, not modified.)

The remaining six domain controllers are genuine 1:1:1 chains.

## External integrations

- **PostgreSQL — the only external system.** Configured in
  `src/main/resources/dev/application.properties` and `prod/application.properties`
  via `spring.datasource.url=jdbc:postgresql://...`,
  `spring.datasource.driver-class-name=org.postgresql.Driver`, and the
  `org.hibernate.dialect.PostgreSQLDialect`. Schema/seed SQL lives under
  `src/main/resources/db/`. All seven repositories persist here.
- **No object storage / S3.** Confirmed in code: the `Image` entity
  (`@Table(name = "images")`) stores only `url` (TEXT), `object_type` (TEXT) and
  `object_id` (Integer). `ImageService`/`ImageRepository` perform plain JPA
  persistence of these **URL reference rows** — there is no AWS SDK, `AmazonS3`,
  `S3Client`, or any upload/blob client anywhere in the source. Image handling is
  purely URL-reference based.
- **No other external API / message queue / mail integration.** A source-wide
  search for `RestTemplate`, `WebClient`, `FeignClient`, `RabbitTemplate`,
  `KafkaTemplate`, `JmsTemplate`, `JavaMailSender`, `AmazonS3`/`S3Client`, and
  `HttpClient` returned **zero matches**. The backend makes no outbound HTTP calls,
  publishes to no broker, and sends no email.

**Verdict:** the backend integrates with **PostgreSQL only**. The single external
node in the diagram (PostgreSQL) reflects the complete set of external dependencies.
