# Database Detection Notes

Internal artifact for the README-assembly agent. Documents only what exists in the entity classes and SQL scripts.

## Stack

- **Engine:** PostgreSQL (driver `org.postgresql.Driver`, dialect `org.hibernate.dialect.PostgreSQLDialect`).
- **ORM:** Spring Data JPA over Hibernate (`spring-boot-starter-data-jpa`).
- **Schema management mode:** `spring.jpa.hibernate.ddl-auto=update`. The live/runtime schema is generated and evolved by Hibernate from the `@Entity` classes. `update` adds missing tables/columns but does not drop or alter existing ones.
- **Connection pool:** HikariCP (Spring Boot's default `DataSource` pool, pulled in transitively by the JPA starter). No explicit pool config in `application.properties`.
- **JDBC URL (default):** `jdbc:postgresql://localhost:5432/tripbudgetplanner` (overridable via `SPRING_DATASOURCE_URL`). Database name: `tripbudgetplanner`, port `5432`.

## Migration tooling: NONE

There is **no migration tool wired in**. `pom.xml` contains no Flyway and no Liquibase dependency (the only DB-related dependencies are `spring-boot-starter-data-jpa` and the `postgresql` runtime driver). The files under `src/main/resources/db/` (`V1__Init_Setup.sql`, `V1__Add_Data.sql`) use Flyway-style `V1__` naming but are **manual scripts only** — nothing executes them automatically. The live schema is governed entirely by `ddl-auto=update` driven from the entities; the SQL files document the intended/initial schema and seed data.

## Per-table breakdown

Table/column names below are the real PostgreSQL names from `V1__Init_Setup.sql`, cross-checked against the JPA entities.

### locations  (entity: `Location`)
- `id` INT — PK, `GENERATED ALWAYS AS IDENTITY` (entity: `@GeneratedValue(IDENTITY)`).
- `city` VARCHAR(150) NOT NULL.
- `country` VARCHAR(100) NOT NULL.
- No FKs.

### images  (entity: `Image`)
- `id` INT — PK, `GENERATED ALWAYS AS IDENTITY`.
- `url` TEXT NOT NULL.
- `object_type` TEXT NOT NULL — **CHECK** `object_type IN ('users','voyages')`.
- `object_id` INT NOT NULL — polymorphic soft reference (no FK).
- No FKs. The `object_type`/`object_id` pair is a polymorphic/soft pointer resolved in app code, not a database FK.

### users  (entity: `User`)
- `id` INT — PK, `GENERATED ALWAYS AS IDENTITY`.
- `last_name` VARCHAR(250), nullable.
- `first_name` VARCHAR(250), nullable.
- `username` VARCHAR(250) NOT NULL, **UNIQUE**.
- `password` VARCHAR(250) NOT NULL.
- `mail` VARCHAR(250) NOT NULL, **UNIQUE**.
- `phone_numb` VARCHAR(20), nullable (entity field `phoneNumber`).
- `birthday` DATE, nullable.
- `profile_image_id` INT — **FK** -> `images(id)`, `ON DELETE SET NULL` (entity: `@ManyToOne` `profile_image_id`).
- `location_id` INT — **FK** -> `locations(id)`, `ON DELETE SET NULL` (entity: `@ManyToOne` `location_id`).

### friends  (join table; no dedicated entity)
- `user_id` INT — FK -> `users(id)`, `ON DELETE CASCADE`.
- `friend_id` INT — FK -> `users(id)`, `ON DELETE CASCADE`.
- **Composite PK** `(user_id, friend_id)`.
- Represented in JPA only as `User.friends` `@ManyToMany` with `@JoinTable(name="friends", joinColumns=user_id, inverseJoinColumns=friend_id)`. Self-referencing, unidirectional (no inverse side).

### voyages  (entity: `Voyage`)
- `id` INT — PK, `GENERATED ALWAYS AS IDENTITY`.
- `object_type` TEXT NOT NULL — **CHECK** `object_type IN ('users','travel_groups')`.
- `object_id` INT NOT NULL — polymorphic soft reference (no FK).
- `destination` TEXT NOT NULL.
- `budget_total` NUMERIC(10,2) NOT NULL.
- `duration_days` INT NOT NULL — **CHECK** `duration_days > 0`.
- `start_date` DATE, nullable.
- `created_at` TIMESTAMPTZ NOT NULL, **DEFAULT NOW()** (entity sets it via field initializer / `@PrePersist`).
- `cover_image_id` INT — **FK** -> `images(id)`, `ON DELETE SET NULL` (entity: `@ManyToOne coverImage`).

### travel_groups  (entity: `TravelGroup`)
- `id` INT — PK, `GENERATED ALWAYS AS IDENTITY`.
- `voyage_id` INT — **FK** -> `voyages(id)`, `ON DELETE SET NULL` (entity: `@ManyToOne voyage`).
- `name` VARCHAR(100) NOT NULL.
- `created_at` TIMESTAMPTZ NOT NULL, **DEFAULT NOW()** (entity field initializer / `@PrePersist`).

### group_memberships  (join table; no dedicated entity)
- `group_id` INT — FK -> `travel_groups(id)`, `ON DELETE CASCADE`.
- `user_id` INT — FK -> `users(id)`, `ON DELETE CASCADE`.
- **Composite PK** `(group_id, user_id)`.
- Represented in JPA as `User.travelGroups` `@ManyToMany` `@JoinTable(name="group_memberships", joinColumns=user_id, inverseJoinColumns=group_id)`, with the inverse side `TravelGroup.users` (`mappedBy="travelGroups"`).

### expenses  (entity: `Expense`)
- `id` INT — PK, `GENERATED ALWAYS AS IDENTITY`.
- `voyage_id` INT NOT NULL — **FK** -> `voyages(id)`, `ON DELETE CASCADE`; also **UNIQUE** (one expenses row per voyage => effectively 1:1). Entity: `@ManyToOne` + `@JoinColumn(unique=true)`.
- `transport_amount` NUMERIC(10,2) NOT NULL, **DEFAULT 0**, **CHECK** `>= 0`.
- `hotel_amount` NUMERIC(10,2) NOT NULL, **DEFAULT 0**, **CHECK** `>= 0`.
- `restaurant_amount` NUMERIC(10,2) NOT NULL, **DEFAULT 0**, **CHECK** `>= 0`.
- `activities_amount` NUMERIC(10,2) NOT NULL, **DEFAULT 0**, **CHECK** `>= 0`.
- `total_amount` NUMERIC(12,2) — **generated column**: `GENERATED ALWAYS AS (transport_amount + hotel_amount + restaurant_amount + activities_amount) STORED`. Entity maps it read-only (`insertable=false, updatable=false`).
- `currency` CHAR(3) NOT NULL, **DEFAULT 'EUR'** (entity default `"EUR"`, `length=3`).
- `created_at` TIMESTAMPTZ NOT NULL, **DEFAULT NOW()**.
- **UNIQUE (voyage_id)**.

### itinerary  (entity: `Itinerary`)
- `id` — PK, **SERIAL** (note: this is the only table using `SERIAL` rather than `GENERATED ALWAYS AS IDENTITY` in SQL).
- `voyage_id` INT NOT NULL — **FK** -> `voyages(id)`, `ON DELETE CASCADE` (entity: `@ManyToOne voyage`).
- `day_number` INT NOT NULL — **CHECK** `day_number > 0`.
- `activity` TEXT NOT NULL.
- `created_at` TIMESTAMPTZ NOT NULL, **DEFAULT NOW()**.
- **UNIQUE (voyage_id, day_number)** — SQL constraint `unique_voyage_day`; entity declares the same via `@UniqueConstraint(columnNames={"voyage_id","day_number"})`.

## Relationships and ON DELETE behavior

| From (FK side) | To | Column | Cardinality | ON DELETE | JPA association |
|---|---|---|---|---|---|
| users | locations | `location_id` | many users -> one location | SET NULL | `User.location_id` `@ManyToOne` |
| users | images | `profile_image_id` | many users -> one image | SET NULL | `User.profile_image_id` `@ManyToOne` |
| voyages | images | `cover_image_id` | many voyages -> one image | SET NULL | `Voyage.coverImage` `@ManyToOne` |
| friends | users | `user_id` | join -> users | CASCADE | `User.friends` `@ManyToMany` join |
| friends | users | `friend_id` | join -> users | CASCADE | `User.friends` `@ManyToMany` join |
| travel_groups | voyages | `voyage_id` | many groups -> one voyage | SET NULL | `TravelGroup.voyage` `@ManyToOne` |
| group_memberships | travel_groups | `group_id` | join -> groups | CASCADE | `User.travelGroups` / `TravelGroup.users` `@ManyToMany` |
| group_memberships | users | `user_id` | join -> users | CASCADE | `User.travelGroups` / `TravelGroup.users` `@ManyToMany` |
| expenses | voyages | `voyage_id` | one-to-one (UNIQUE) | CASCADE | `Expense.voyage` `@ManyToOne(unique)` |
| itinerary | voyages | `voyage_id` | many itinerary rows -> one voyage | CASCADE | `Itinerary.voyage` `@ManyToOne` |

**Polymorphic soft references (NOT FKs):**
- `images.object_type` + `images.object_id` point at either a `users` row or a `voyages` row depending on `object_type`. No FK constraint exists; resolution is in application code.
- `voyages.object_type` + `voyages.object_id` point at either a `users` row or a `travel_groups` row depending on `object_type`. No FK constraint exists.
These are intentionally not modeled as relationship lines in the ER diagram.

## Entity vs SQL discrepancies

1. **CHECK constraints not in entities.** The SQL CHECKs (`voyages.duration_days > 0`, all four `expenses.*_amount >= 0`, `itinerary.day_number > 0`) and the CHECKs on `object_type` enums exist only in the SQL script. The JPA entities do not declare these, so under `ddl-auto=update` (entity-driven) these CHECKs are not guaranteed to be created on a fresh Hibernate-generated schema.

2. **`expenses.total_amount` generated column.** SQL defines it as a STORED generated column. The entity maps it as a plain read-only `BigDecimal` (`insertable=false, updatable=false`) — Hibernate will not recreate the `GENERATED ALWAYS AS ... STORED` expression from the entity; the computed-column behavior only exists if the table was created from the SQL script.

3. **`itinerary.id` uses `SERIAL`** in SQL while every other table uses `GENERATED ALWAYS AS IDENTITY`. The entity uses `@GeneratedValue(strategy = IDENTITY)` uniformly, so Hibernate's generated DDL would differ in identity mechanism from the hand-written SQL.

4. **Column defaults (`DEFAULT 0`, `DEFAULT 'EUR'`, `DEFAULT NOW()`).** Defined as SQL column defaults in the script, but in the entities they are Java-side field initializers / `@PrePersist` logic, not `columnDefinition` defaults. Hibernate-generated DDL would not include the SQL `DEFAULT` clauses.

5. **Join tables have no entity classes.** `friends` and `group_memberships` exist only as `@ManyToMany` `@JoinTable` mappings on `User` (and the inverse `TravelGroup.users` for memberships). There are no `@Entity` classes or repositories for them.

6. **`friends` is mapped unidirectionally and self-referential.** Only `User.friends` defines it; there is no inverse collection, so the relationship is directional at the JPA level (`user_id` -> `friend_id`) even though the SQL composite-PK join table itself is symmetric in structure.

7. **Entity field naming quirks.** `User` names its association fields `profile_image_id` (type `Image`) and `location_id` (type `Location`) — these are object references, not raw id columns, despite the id-suffixed field names. They still map to the `profile_image_id` / `location_id` FK columns.

8. **`spring.jpa.show-sql=true` / `ddl-auto=update`** means the schema is entity-driven at runtime; the `db/*.sql` files are not applied by the application. Discrepancies 1-4 only matter if a database is bootstrapped from the SQL script vs. left to Hibernate.

## Repositories (Spring Data JPA)

All extend `JpaRepository<Entity, Integer>`:
- `UserRepository` — custom JPQL: `findByUsernameOrEmailDTO`, `findByUsernameOrEmail`, `existsByUsername`, `existsByMail`.
- `VoyageRepository` — custom JPQL `findById`.
- `ExpenseRepository`, `ItineraryRepository`, `LocationRepository`, `TravelGroupRepository`, `ImageRepository` — CRUD only, no custom queries.
