# Harmonia

A modular desktop management system built for a hospital's cafeteria, IT asset
disposal, equipment technical-support, and internal phone-directory workflows.

**Stack:** Java 17 · JavaFX 21 · PostgreSQL · JasperReports · HikariCP · Maven

> Full step-by-step environment setup for Windows/Eclipse, database
> configuration, and a scalability discussion are also available in the
> companion document **Guia_Harmonia.docx** shipped alongside this project.

## Overview

Harmonia is a single-workstation JavaFX desktop application with four
independent modules, each gated by a per-account access flag (see
**Access control** below):

| Module | Status | What it does |
|---|---|---|
| **Cafeteria** (Refeitorio) | Fully implemented | Registers contracted employees (grouped by a dedicated category registry), logs their meals (type/price auto-derived from the time of day), configures meal prices, bulk-imports employees/meals from `.txt` files, and generates PDF and XLSX spend reports per period, filterable by category, meal type and employee. |
| **Technical Support** (Assistência Técnica) | Fully implemented | Tracks any equipment (printers, notebooks, projectors, monitors, ...) sent out for repair, its warranty (in days) and attachment, and its return. Formerly a printers-only module ("Impressoras") -- generalized and renamed; the old printer-only classes were removed. |
| **Phone Directory** (Agenda Telefônica) | Fully implemented | Hospital sectors, doctors, partner institutions, ambulance/transport services and any other useful phone number, with its own category registry. Replaces the standalone "Agenda v2.0" Excel spreadsheet the front desk used before. |
| **Asset Disposal** (Descarte de Ativos) | Service layer implemented; screen is a placeholder | Looks up an asset in an external database, issues a technical disposal report, and exports it to PDF. |

There is also a **Gerenciar Usuários** screen (CIAU accounts only) for
creating accounts and editing which of the four modules each one can open.

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

Both the Cafeteria's employee categories and the Phone Directory's contact
categories are backed by their own registry table (`categorias` /
`categorias_agenda`), picked from a dropdown instead of typed as free text --
this is what keeps "Alunos", "ALUNOS" and "Alunos " from becoming three
different groups on a billing report or a contact list.

## Access control

Each account (`usuarios`) has three independent flags --
`acesso_refeitorio`, `acesso_agenda_telefonica` and
`acesso_assistencia_tecnica` -- editable per account on the **Gerenciar
Usuários** screen. `perfil` (RH/FINANCEIRO/CIAU) no longer determines module
access by itself the way it originally did; it now only labels the account
(shown in the users table) and gates the Gerenciar Usuários screen itself,
which stays role-based and CIAU-only on purpose -- it is the screen that
hands out every other permission, so letting it be self/other-granted
through the same checkbox list it controls would be a privilege-escalation
hole.

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
Maven installed. An optional `mvn package -Ppackage-msi` profile builds a
Windows `.msi` installer instead (requires the WiX Toolset v3 -- see the
comment on that profile in `pom.xml`).

`src/main/resources/db/migrations/` has the one-off SQL scripts for bringing
an existing database up to date with a schema change that isn't covered by
`schema.sql`'s own `CREATE TABLE IF NOT EXISTS` statements (e.g. a new column
on a table that already exists). Run whichever ones apply to your database,
in date order; each one documents, in its own header comment, what it does
and whether it's safe to run more than once.

## Logging

Logging uses **SLF4J + Logback** (`src/main/resources/logback.xml`). Every
DAO, service and controller logs through it instead of `System.out`/
`printStackTrace`, so operational history is always available even though the
UI only ever shows the user a short, friendly message. An uncaught exception
on any thread is also caught globally (`App.handleUncaughtException`) and
shown as a Portuguese alert instead of failing silently.

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
instead of a real database connection -- this is why most services expose a
package-private constructor that accepts a DAO, alongside their normal no-arg
constructor used by the rest of the application. Coverage today includes:

- `MealTypeTest` -- the cafeteria's operating-hours boundaries (`byTime`).
- `PasswordUtilTest` -- password hashing/verification, including
  null-safety and a corrupted-hash edge case.
- `EmployeeServiceTest`, `MealServiceTest`, `MealPriceServiceTest` -- input
  validation, the total-spent calculation, and price configuration.
- `AuthServiceTest` -- successful login, wrong password, unknown username.
- `CategoryServiceTest` -- the category registry shared by employees.
- `EquipmentServiceTest` -- Assistência Técnica's validation rules.
- `UserServiceTest` -- account registration and per-module permission updates.
- `AssetDisposalServiceTest` -- report generation against the external asset
  lookup, ahead of the screen itself being implemented (see **Known
  limitations** below).

Not yet covered: any controller, any DAO implementation (the tests above use
fakes, so the real SQL is never exercised by an automated test), and the
Phone Directory's `ContactService`/`ContactCategoryService`, and
`ReportFilters`. The same fake-DAO pattern used above can be extended to
those.

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
  model/                      Entities and enums (Employee, Meal, Contact, User, ...)
  dao/ and dao/impl/          Data access (plain JDBC)
  service/                    Business rules and validation
  controller/                 JavaFX FXML screen controllers
  util/                       Password hashing, alerts, session, PDF/XLSX reports

src/main/resources/
  fxml/          Screens (login, hub, cafeteria, technical support, phone
                 directory, asset disposal, user management, preferences)
  css/           Visual styling
  reports/       JasperReports templates (.jrxml)
  db/            schema.sql, plus one-off migration scripts in migrations/
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
- The **Asset Disposal** screen is a placeholder; its service/DAO layers are
  fully implemented and tested at the persistence level (see
  `AssetDisposalServiceTest`), only the JavaFX screen wiring is pending.
- Locally attached files (profile photos, warranty attachments) are stored on
  the local filesystem (`fotos_perfil/`, `anexos_impressoras/`); moving to
  shared storage (a network folder, or an S3/MinIO bucket) is recommended
  before running this on more than one workstation.
- **Schema migrations are plain SQL scripts**, applied by hand in date order
  (see **Running** above) -- there's no Flyway/Liquibase-style tracking of
  which migration already ran against which database. Fine for a single
  production database with one person applying changes; worth revisiting if
  that stops being true.
- The Phone Directory's ~5,100 legacy contacts (from the old "Agenda v2.0"
  spreadsheet) were imported directly against the production database; the
  import/standardization scripts are intentionally **not** committed to this
  repository, since they contain real contact data (names, phone numbers) for
  people and institutions tied to the hospital.
