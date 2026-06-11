# Key Architectural Decisions

Each decision is grounded in a concrete artifact in the repository.

## 1. No `spring-boot-starter-parent`; BOM imported via `dependencyManagement`

- **Evidence:** `pom.xml` has the comment `<!-- NO PARENT - Use dependency management instead -->`
  and imports `org.springframework.boot:spring-boot-dependencies:3.5.6` with
  `<type>pom</type><scope>import</scope>` instead of inheriting from the starter parent.
- **Rationale:** Keeps the POM free of an inherited parent so build plugins, properties, and
  packaging are configured explicitly rather than inherited; useful when the project should not
  adopt all parent defaults.
- **Tradeoff:** Loses the parent's preconfigured plugin management and conventions (e.g. the
  `spring-boot-maven-plugin` version and `maven-compiler-plugin` must be declared/pinned by hand,
  as they are here), increasing POM maintenance.

## 2. Hibernate `ddl-auto` drives the schema while hand-written SQL exists, with no migration tool

- **Evidence:** `dev` sets `spring.jpa.hibernate.ddl-auto=update`; `prod` sets `validate`. A
  hand-written `src/main/resources/db/V1__Init_Setup.sql` (Flyway-style filename) defines the full
  schema, but **no Flyway or Liquibase dependency** is present in `pom.xml`, and nothing wires the
  script into startup.
- **Rationale:** In dev, `update` lets Hibernate auto-evolve the schema from JPA entities for fast
  iteration; the `V*.sql` file documents/seeds the intended schema manually.
- **Tradeoff:** Schema drift risk. The `V1__` file is not executed by any migration tool, so the
  live schema (Hibernate-generated) can diverge from the SQL. `prod`'s `validate` will fail if the
  DB was not provisioned to exactly match the entities. No versioned, repeatable migrations.

## 3. Stateless JWT authentication instead of server-side sessions

- **Evidence:** `spring-boot-starter-security` plus `com.auth0:java-jwt:4.4.0` (pinned). Both
  property files define `jwt.secret` and `jwt.expiration=86400000` (24h). No session-store
  dependency (e.g. Spring Session) is present.
- **Rationale:** Stateless tokens let the API scale horizontally without sticky sessions or a
  shared session store, and suit a SPA/mobile client consuming `http://localhost:8080/api`.
- **Tradeoff:** Tokens cannot be trivially revoked before expiry; secret-key management matters,
  and the secret currently lives in the (untracked) property files. The 24h expiry is a fixed
  window with no refresh-token mechanism visible in config.

## 4. Dev/prod split via Maven profiles + filtered, git-ignored credential folders

- **Evidence:** `pom.xml` defines `dev` (`activeByDefault=true`) and `prod` profiles, each
  enabling Maven resource filtering and packaging only the matching
  `src/main/resources/{dev,prod}` folder. `.gitignore` excludes `**/src/main/**/dev/` and
  `**/src/main/**/prod/` under `### Credentials ###`.
- **Rationale:** Environment-specific configuration (DB URL/credentials, JWT secret, logging,
  `ddl-auto`) is isolated per profile and kept out of version control.
- **Tradeoff:** A fresh checkout has no `dev`/`prod` property files, so the build/run is not
  reproducible without recreating them out-of-band. Secrets are managed manually rather than via a
  secrets manager.

## 5. Polymorphic associations in the schema instead of typed foreign keys

- **Evidence:** `V1__Init_Setup.sql`: `Images(object_type TEXT CHECK (object_type IN ('users','voyages')), object_id INT)`
  and `Voyages(object_type TEXT CHECK (object_type IN ('users','travel_groups')), object_id INT)`.
  The owner is identified by a `(object_type, object_id)` pair rather than separate nullable FKs.
- **Rationale:** One table can attach to multiple entity types (e.g. an image belongs to either a
  user or a voyage) without adding a column per relationship.
- **Tradeoff:** `object_id` cannot be a real foreign key, so referential integrity for those links
  is not enforced by the database (only the `object_type` value is constrained by a CHECK). Joins
  require filtering on `object_type`.

## 6. Database-computed `STORED` generated column for expense totals

- **Evidence:** `V1__Init_Setup.sql`: `Expenses.total_amount NUMERIC(12,2) GENERATED ALWAYS AS
  (transport_amount + hotel_amount + restaurant_amount + activities_amount) STORED`.
- **Rationale:** The total is always consistent with its components and is computed/persisted by
  PostgreSQL, so application code cannot produce a wrong or stale total.
- **Tradeoff:** Couples the calculation to PostgreSQL (generated columns are DB-specific) and the
  column is read-only from JPA's perspective; the entity mapping must mark it non-insertable /
  non-updatable. Conflicts with `ddl-auto` if Hibernate tries to manage this column.
