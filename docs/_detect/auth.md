# Auth Detection Notes

Internal notes derived directly from the source code. Documents only what is implemented.

## Mechanism

- **Type:** JWT (stateless bearer token) on top of **Spring Security**.
- **JWT library:** `com.auth0:java-jwt` 4.4.0 (`com.auth0.jwt.JWT`, `Algorithm`, `JWTVerifier`, `DecodedJWT`).
- **Implementing classes:**
  - `com.planner.app.auth.jwt.JwtUtil` — token creation / validation / username extraction.
  - `com.planner.app.auth.jwt.JwtAuthenticationFilter` — per-request bearer-token authentication.
  - `com.planner.app.config.SecurityConfig` — filter chain, encoder, auth provider.
  - `com.planner.app.service.AuthService` — login/register business logic.
  - `com.planner.app.service.api.CustomUserDetailsService` — `UserDetailsService` backed by the DB.

## Endpoints (AuthController)

`@RestController` mapped at base path `/api/auth` (`AuthController`).

| Method | Path | Handler | Request body | Success | Failure |
|--------|------|---------|--------------|---------|---------|
| POST | `/api/auth/signin` | `loginUser` | `LoginDTO { username, password }` | `200 OK` → `LoginResponseDTO { token, user, message }` | `401 UNAUTHORIZED` with `message` set, on `RuntimeException` |
| POST | `/api/auth/signup` | `registerUser` | `RegisterRequest { firstName, lastName, username, mail, password, birthday, city, country }` | `201 CREATED` → `UserDTO` | `400 BAD_REQUEST` (empty body) on `RuntimeException` |

> No refresh-token endpoint and no logout endpoint exist in the code. Tokens are simply allowed to expire.

## Token details (`JwtUtil`)

- **Signing algorithm:** `Algorithm.HMAC256(secret)` → **HS256** (symmetric).
- **Secret:** read from `jwt.secret` (configured; value not reproduced here).
- **Claims actually written** (`generateToken(username)`):
  - `sub` (subject) = username
  - `iss` (issuer) = `"planner-app"` (constant `ISSUER`)
  - `iat` (issued-at) = current time
  - `exp` (expires-at) = now + `jwt.expiration`
  - No roles/authorities, email, or user id are embedded in the token.
- **Expiration:** `jwt.expiration` = **86400000 ms = 24 hours** (configured in both `dev` and `prod` `application.properties`). Code default if unset: `86400000` (same).
- **Validation** (`validateToken`): verifies signature with HS256 **and** requires `iss == "planner-app"`. Returns `false` on any `JWTVerificationException`.
- **Username extraction** (`getUsernameFromToken`): verifies the same way, returns `sub`, or `null` if verification fails.
- `isTokenExpired(token)` exists but is **not referenced** by the filter or service (dead/unused helper).
- **Token transport:** HTTP request header `Authorization: Bearer <token>`. The filter strips the `"Bearer "` prefix (`substring(7)`). The token is returned to clients in the `LoginResponseDTO.token` field; CORS also exposes the `Authorization` response header.

## Password hashing

- **PasswordEncoder bean:** `new BCryptPasswordEncoder()` in `SecurityConfig.passwordEncoder()` → **BCrypt** (default strength 10).
- **Registration:** `AuthService.register` stores `passwordEncoder.encode(rawPassword)`.
- **Login verification:** delegated to Spring's `DaoAuthenticationProvider`, which calls the same BCrypt encoder's `matches()` against the stored hash. `CustomUserDetailsService` supplies the hashed password from the DB.

## Authentication flow

- **Login:** `AuthService.login` calls `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password))`. The configured `DaoAuthenticationProvider` (wired with `CustomUserDetailsService` + `BCryptPasswordEncoder`) performs the credential check. On success a JWT is generated; on `BadCredentialsException` it throws `RuntimeException("Invalid username or password")`, which the controller maps to `401`.
- **User lookup:** `CustomUserDetailsService.loadUserByUsername` → `UserRepository.findByUsernameOrEmail` (matches on `username` OR `mail`). Built `UserDetails` has an **empty authorities list** (no roles).
- **Per-request auth:** `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) reads `Authorization`, and only if the header starts with `Bearer ` does it extract the username. If a username is present and `SecurityContextHolder` has no existing authentication, it loads the user, validates the token, and sets a `UsernamePasswordAuthenticationToken` (with web auth details) into the `SecurityContext`. The chain always continues via `filterChain.doFilter` regardless.

## Session policy

- **Stateless.** `SessionCreationPolicy.STATELESS` is set in `SecurityConfig.securityFilterChain`. No HTTP session is created or used for auth.

## Filter chain registration (`SecurityConfig`)

- `@Configuration @EnableWebSecurity`.
- `csrf` **disabled** (appropriate for stateless token API).
- `cors` enabled via the `corsConfigurationSource` bean.
- `JwtAuthenticationFilter` registered with `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)` — runs before the standard username/password filter.
- `authenticationProvider(authenticationProvider())` registered: `DaoAuthenticationProvider` with `CustomUserDetailsService` + `BCryptPasswordEncoder`.
- `AuthenticationManager` exposed via `AuthenticationConfiguration.getAuthenticationManager()`.

## Route authorization (exactly as configured)

`authorizeHttpRequests` rules, in order:

| Matcher | Rule |
|---------|------|
| `OPTIONS /**` (any path) | `permitAll` (CORS preflight) |
| `/api/auth/**` | `permitAll` (covers `/api/auth/signin`, `/api/auth/signup`) |
| `anyRequest()` | `authenticated` (everything else requires a valid Bearer token) |

**Public (no token):** all `OPTIONS` preflight requests; the entire `/api/auth/**` subtree.
**Protected (Bearer token required):** every other endpoint in the application.

## CORS configuration (`corsConfigurationSource`)

- Allowed origin: `http://localhost:4200` (single hard-coded front-end origin).
- Allowed methods: `GET, POST, PUT, DELETE, PATCH, OPTIONS`.
- Allowed headers: `Authorization, Content-Type, X-Requested-With`.
- Exposed headers: `Authorization`.
- `allowCredentials = true`.

## Security-relevant / unusual notes

- **JWT secret is committed** in `application.properties` (both `dev` and `prod`) as a short, weak HS256 key. It should be externalized (env var / secret manager) and lengthened; not reproduced here.
- **No authorities/roles** anywhere: `CustomUserDetailsService` returns an empty authority list and no claims carry roles. Authorization is effectively all-or-nothing (authenticated vs not).
- `JwtAuthenticationFilter` loads the user from the DB **before** calling `validateToken`, i.e. a DB hit occurs even for tokens that later fail validation (minor inefficiency, not a vulnerability).
- `registerUser` swallows the exception message and returns an empty `400` body, so the client cannot distinguish "username exists" vs "email exists".
- `isTokenExpired` in `JwtUtil` is unused; expiry is enforced implicitly by `JWTVerifier.verify` inside `validateToken` / `getUsernameFromToken`.
