# Entity Lifecycle Detection Notes — Voyage (Trip)

## Approach used: Operational CRUD lifecycle (NOT an internal status state machine)

This is honest grounding, not invention. Every state and transition in
`docs/diagrams/entity-lifecycle.mmd` maps to a real `VoyageController` endpoint
or a real DB foreign-key rule in `V1__Init_Setup.sql`. No states or transitions
were invented.

## CRITICAL FINDING: no status / state / phase field exists

I scanned all 7 entities and the SQL schema for an explicit status/state/phase/
enum field. There is none.

Entities checked (no status-like column on any of them):

- `entity/Voyage.java`     — id, objectType, objectId, destination, budgetTotal, durationDays, startDate, createdAt, coverImage
- `entity/Expense.java`    — id, voyage, transport/hotel/restaurant/activities/total amounts, currency, createdAt
- `entity/Itinerary.java`  — id, voyage, dayNumber, activity, createdAt
- `entity/TravelGroup.java`— id, name, voyage, createdAt, users
- `entity/User.java`       — id, names, username, password, mail, phone, birthday, image, location, friends, travelGroups
- `entity/Location.java`   — id, city, country
- `entity/Image.java`      — id, url, objectType, objectId

A grep for `status|state|phase|stage|lifecycle|is_active|active|archived|published|draft`
across `src/main` returned only:

- `HttpStatus.*` usages in controllers (HTTP response codes, not a domain field)
- `spring.profiles.active=prod` (Spring config) and `getActiveProfiles()` (startup log)
- A French code comment ("active CORS") and a data string

None of these is a domain status/state column. The `voyages` table DDL
(`V1__Init_Setup.sql`, lines 37-47) has no status column. Confirmed: there is
**NO status field**, so per the instructions I fell back to the operational CRUD
lifecycle and did NOT invent internal states.

## Real VoyageController endpoints that justify each transition

`controller/VoyageController.java` (`@RequestMapping("/api/voyages")`) defines
exactly TWO endpoints:

| Transition (diagram)        | HTTP method + path        | Controller method                | Service call                  |
|-----------------------------|---------------------------|----------------------------------|-------------------------------|
| `[*] --> Created`           | `POST /api/voyages`       | `VoyageController.createVoyage`  | `VoyageService.createVoyage` -> `voyageRepository.save` |
| `Created --> Created` (read)| `GET /api/voyages/{id}`   | `VoyageController.getVoyage`     | `VoyageService.getVoyageById` -> `voyageRepository.findById` |

- `POST /api/voyages` returns `201 CREATED` (or `400 BAD_REQUEST` on failure).
  This is the only transition that creates the entity. Modeled as `[*] --> Created`.
- `GET /api/voyages/{id}` returns `200 OK` (or `404 NOT_FOUND`). A pure read; it
  does not change persisted state. Modeled as the self-loop `Created --> Created`.

There is **NO update endpoint** (no `@PutMapping` / `@PatchMapping`) and **NO
delete endpoint** (no `@DeleteMapping`) in `VoyageController`. Therefore the
diagram intentionally contains no API-driven "Updated" state and no API-driven
"Deleted" transition. Adding them would be invention, which is forbidden.

## The Deleted transition is DB-level only (not an API endpoint)

Since no `DELETE /api/voyages/{id}` exists, a Voyage row can only be removed by a
direct DB-level `DELETE` (raw SQL / DBA action). I modeled this as
`Created --> Deleted` and labeled it explicitly "DB-level DELETE on voyages
(no API endpoint)" so it is not mistaken for an application capability.

The transition is included specifically to document the cascade effect required
by the task.

## Cascade relationships (from V1__Init_Setup.sql) carried by the Deleted transition

Foreign keys referencing `voyages(id)`:

- `expenses.voyage_id  INT NOT NULL REFERENCES voyages(id) ON DELETE CASCADE`   (line 65) — child Expense row is deleted with the Voyage.
- `itinerary.voyage_id INT NOT NULL REFERENCES voyages(id) ON DELETE CASCADE`   (line 81) — child Itinerary rows are deleted with the Voyage.
- `travel_groups.voyage_id INT REFERENCES voyages(id) ON DELETE SET NULL`        (line 52) — TravelGroup is NOT deleted; its `voyage_id` is set to NULL (detached).

So deleting a Voyage CASCADE-removes its Expenses and Itinerary rows, and
SET-NULLs the `voyage_id` of any associated TravelGroups. These rules are encoded
both in the `Created --> Deleted` transition label and in the note attached to the
`Created` state in the diagram.

(For completeness: `voyages.cover_image_id REFERENCES Images(id) ON DELETE SET NULL`
is the inbound direction — deleting an Image nulls the Voyage's cover image; it is
not part of deleting a Voyage, so it is not on the lifecycle diagram.)

## Diagram validity

`docs/diagrams/entity-lifecycle.mmd` is a `stateDiagram-v2` and was validated with
the Mermaid render/validate tool (`valid: true`, no validation error). Note: the
renderer in use has a bug that fails when a `note` is attached to a state that
feeds the final `[*]` (the `Deleted` state), emitting "No such shape: undefined".
To keep the file renderable by mermaid-cli, the explanatory note (including the
cascade rules) is attached to the `Created` state, and the cascade is also stated
inline on the `Created --> Deleted` transition label.
