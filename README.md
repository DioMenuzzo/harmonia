# Harmonia

A modular desktop management system built for a hospital's cafeteria, IT asset
disposal, and printer maintenance workflows.

**Stack:** Java 17 · JavaFX 21 · PostgreSQL · JasperReports · HikariCP · Maven

> Full step-by-step environment setup for Windows/Eclipse, database
> configuration, and a scalability discussion are also available in the
> companion document **Guia_Harmonia.docx** shipped alongside this project.

## Overview

Harmonia is a single-workstation JavaFX desktop application with three
independent modules, gated by role-based access control:

| Module | Status | What it does |
|---|---|---|
| **Cafeteria** (Refeitorio) | Fully implemented | Registers contracted employees, logs their meals (type/price auto-derived from the time of day), configures meal prices, bulk-imports employees/meals from `.txt` files, and generates PDF spend reports per period. |
| **Asset Disposal** (Descarte de Ativos) | Service layer implemented; screen is a placeholder | Looks up an asset in an external database, issues a technical disposal report, and exports it to PDF. |
| **Printers** (Impressoras) | Service layer implemented; screen is a placeholder | Tracks printers sent out for repair, their warranty status and attachment, and their return. |

Every screen and every user-facing message is in Portuguese (the system's
audience is Brazilian hospital staff); the codebase itself — class, method and
variable names, comments — is in English, so the project can be read,
reviewed, and contributed to by any Java developer regardless of language.

## Architecture

Harmonia follows a straightforward layered architecture, which keeps each
concern testable and replaceable in isolation:

```
controller/   JavaFX controllers bound to FXML screens -- UI logic only,
              delegates every business rule to a service.
service/      Business rules and validation. Framework-agnostic: no JavaFX,
              no JDBC. This is what the JUnit test suite targets.
dao/ + impl/  Data access (plain JDBC + HikariCP). One interface per
              aggregate, so a service can be tested against an in-memory
              fake instead of a real database (see src/test).
model/        Plain data classes (entities / enums).
util/         Cross-cutting helpers: password hashing, alerts, PDF/HTML
              report generation, session state.
config/       Database connection pooling (HikariCP), lazily and
              independently for the main and the external database.
```

A request flows one direction only: `controller -> service -> dao -> database`.
Controllers never touch JDBC directly, and DAOs never contain business rules
-- this is what makes the service layer unit-testable without a database (see
**Testing** below).

## Prerequisites

- JDK 17+
- Maven 3.8+ (or an IDE with Maven support -- the project was developed in
  Eclipse with the "Enterprise Java and Web Developers" package)
- PostgreSQL 13+

## Configuration

Database credentials are read from `src/main/resources/db.properties`, with
optional overrides from environment variables (`DB_URL`, `DB_USER`,
`DB_PASSWORD`, `EXTERNAL_DB_URL`, `EXTERNAL_DB_USER`, `EXTERNAL_DB_PASSWORD`) --
**prefer the environment variables in any shared or production environment**,
so credentials never need to be committed to version control.

```properties
db.url=jdbc:postgresql://localhost:5432/sysgest_hospital
db.user=postgres
db.password=your_password_here
```

The external database (used only by the Asset Disposal module to look up
existing assets) is configured independently and is allowed to be unavailable
without breaking the rest of the application -- see the comment on
`DatabaseConfig` for why the two connection pools are initialized lazily and
separately.

## Running

```bash
# 1. Create the database and load the schema
createdb sysgest_hospital
psql sysgest_hospital < src/main/resources/db/schema.sql

# 2. Generate the bcrypt password hash for each seed user and update
#    the "usuarios" table with it (see the class's own instructions)
mvn compile exec:java -Dexec.mainClass=com.hospital.harmonia.util.InitialHashGenerator

# 3. Run the application
mvn javafx:run
```

Alternatively, `mvn package` produces a self-contained "fat jar"
(`target/harmonia.jar`, via `maven-shade-plugin`) that can be run directly
with `java -jar target/harmonia.jar` on any machine with a JDK 17+ and no
Maven installed.

## Logging

Logging uses **SLF4J + Logback** (`src/main/resources/logback.xml`). Every
DAO, service and controller logs through it instead of `System.out`/
`printStackTrace`, so operational history is always available even though the
UI only ever shows the user a short, friendly message.

- Console output while developing (`mvn javafx:run` / running from the IDE).
- `logs/harmonia.log`, rotated daily and at 10MB, 30 days of history kept --
  this is what to check first when a user reports "something went wrong".
- The application's own package logs at `DEBUG` by default; turn it down to
  `INFO` for a leaner production log by editing the single `<logger>` element
  in `logback.xml` -- no code changes needed.

## Testing

```bash
mvn test
```

The test suite (`src/test/java`) targets the **service layer** with plain
JUnit 5, using small hand-written in-memory fakes of the DAO interfaces
instead of a real database connection -- this is why `EmployeeService`,
`MealService` and `AuthService` expose a package-private constructor that
accepts a DAO, alongside their normal no-arg constructor used by the rest of
the application. Coverage today includes:

- `MealTypeTest` -- the cafeteria's operating-hours boundaries (`byTime`).
- `PasswordUtilTest` -- password hashing/verification, including
  null-safety and a corrupted-hash edge case.
- `EmployeeServiceTest`, `MealServiceTest` -- input validation and the
  total-spent calculation.
- `AuthServiceTest` -- successful login, wrong password, unknown username.

The same fake-DAO pattern can be extended to the remaining services
(`PrinterService`, `AssetDisposalService`, `MealPriceService`, `UserService`)
by adding the same package-private constructor to each.

## Error handling

- The DAO layer wraps every `SQLException` in a dedicated unchecked
  `com.hospital.harmonia.dao.DataAccessException`, logged with full context
  before being rethrown -- callers can catch this specific type instead of a
  generic `RuntimeException` if they need to react to a persistence failure
  differently.
- A duplicate employee registration number is detected at the database
  constraint level (Postgres `unique_violation`, SQLSTATE `23505`) and turned
  into a clear, user-facing message instead of a raw SQL error.
- Business validation (required fields, negative prices, invalid date
  ranges, etc.) is centralized in the service layer and raises
  `IllegalArgumentException`, which the controllers catch and show through
  `AlertUtil` -- validation never reaches the DAO layer.

## Project structure

```
src/main/java/com/hospital/harmonia/
  App.java, Launcher.java     JavaFX application bootstrap
  config/                     Database connection pooling (HikariCP)
  model/                      Entities and enums (Employee, Meal, User, ...)
  dao/ and dao/impl/          Data access (plain JDBC)
  service/                    Business rules and validation
  controller/                 JavaFX FXML screen controllers
  util/                       Password hashing, alerts, session, PDF reports

src/main/resources/
  fxml/          Screens (login, hub, cafeteria, asset disposal, printers, preferences)
  css/           Visual styling
  reports/       JasperReports templates (.jrxml)
  db/            Database schema.sql
  images/        Icons and default images
  logback.xml    Logging configuration

src/test/java/com/hospital/harmonia/
  model/, util/, service/     JUnit 5 tests for the framework-independent layers
```

## Known limitations & roadmap

- **Database naming is still in Portuguese.** Table and column names
  (`colaboradores`, `refeicoes`, `nome`, `cpf`, ...) predate this codebase's
  English rename and are intentionally left untouched for now -- the database
  is still in a testing phase and this is planned as a follow-up migration.
  Every DAO carries a `NOTE:` comment pointing at exactly which SQL strings
  will need to change together with that migration.
- **`Employee.registrationNumber`** is stored in a database column still
  named `cpf`, even though it holds an internal registration/badge number,
  not an actual CPF -- renaming that column is part of the same future
  migration.
- The **Asset Disposal** and **Printers** screens are placeholders; their
  service/DAO layers are fully implemented and tested at the persistence
  level, only the JavaFX screen wiring is pending.
- Locally attached files (profile photos, warranty attachments) are stored on
  the local filesystem (`fotos_perfil/`, `anexos_garantia/`); moving to
  shared storage (a network folder, or an S3/MinIO bucket) is recommended
  before running this on more than one workstation.
