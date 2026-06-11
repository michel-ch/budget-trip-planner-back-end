# REST API Surface — Endpoint Inventory (internal notes)

Source of truth: controllers under `src/main/java/com/planner/app/controller/` and
`src/main/java/com/planner/app/auth/api/AuthController.java`.

Auth rules are derived from `SecurityConfig.securityFilterChain(...)`:

```java
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // CORS preflight
.requestMatchers("/api/auth/**").permitAll()              // login / register
.anyRequest().authenticated()                             // everything else needs JWT
```

So: **only `/api/auth/**` (and OPTIONS preflight) is public. Every other route requires a valid `Authorization: Bearer <JWT>` validated by `JwtAuthenticationFilter`.**

> NOTE: `ExpenseController`, `ImageController`, `ItineraryController`, `LocationController`,
> `TravelGroupController`, and `UserController` currently declare only a class-level
> `@RequestMapping` base path and contain **no method-level mappings** — they expose
> **zero endpoints** at this time. They are listed below for completeness with their
> reserved base paths. Per the strict no-inference rule, no conventional/CRUD endpoints
> are assumed for them.

## Master endpoint table

| Method | Route | Controller#method | Request body / params | Description | Auth required |
|--------|-------|-------------------|-----------------------|-------------|---------------|
| **Auth** (`AuthController`, base `/api/auth`) ||||||
| POST | `/api/auth/signin` | `AuthController#loginUser` | Body: `LoginDTO` | Authenticate a user; returns `LoginResponseDTO` (JWT + message). Returns 401 on bad credentials. | No (permitAll) |
| POST | `/api/auth/signup` | `AuthController#registerUser` | Body: `RegisterRequest` | Register a new user; returns 201 with `UserDTO`. Returns 400 on failure. | No (permitAll) |
| **Voyages** (`VoyageController`, base `/api/voyages`) ||||||
| GET | `/api/voyages/{id}` | `VoyageController#getVoyage` | Path var: `id` (Integer) | Fetch one voyage by id; returns 200 with `Voyage`, or 404 if not found. | Yes (JWT) |
| POST | `/api/voyages` | `VoyageController#createVoyage` | Body: `VoyageDTO` | Create a voyage; returns 201 with `Voyage`, or 400 on failure. | Yes (JWT) |
| **Expenses** (`ExpenseController`, base `/api/expenses`) ||||||
| — | (none) | — | — | No method mappings declared (stub controller). | n/a |
| **Images** (`ImageController`, base `/api/images`) ||||||
| — | (none) | — | — | No method mappings declared (stub controller). | n/a |
| **Itineraries** (`ItineraryController`, base `/api/itineraries`) ||||||
| — | (none) | — | — | No method mappings declared (stub controller). | n/a |
| **Locations** (`LocationController`, base `/api/locations`) ||||||
| — | (none) | — | — | No method mappings declared (stub controller). | n/a |
| **Travel groups** (`TravelGroupController`, base `/api/travelgroups`) ||||||
| — | (none) | — | — | No method mappings declared (stub controller). | n/a |
| **Users** (`UserController`, base `/api/users`) ||||||
| — | (none) | — | — | No method mappings declared (stub controller). | n/a |

## Summary

- **Live endpoints: 4** — 2 in `AuthController` (`/api/auth/signin`, `/api/auth/signup`),
  2 in `VoyageController` (`GET /api/voyages/{id}`, `POST /api/voyages`).
- **Public:** both `/api/auth/**` POST endpoints (and any OPTIONS preflight).
- **Protected (JWT required):** both `/api/voyages` endpoints, and any future mapping
  added to the six stub controllers (they fall under `anyRequest().authenticated()`).
- Six controllers (`Expense`, `Image`, `Itinerary`, `Location`, `TravelGroup`, `User`)
  are scaffolding only — no routes yet.
