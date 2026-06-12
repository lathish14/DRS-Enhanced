# DRS-Enhanced — Disaster Response System (Three-Tier)

COIT20258 Software Engineering — Assessment 3 (Group Project)

The DRS-Enhanced extends the Assessment 2 single-application DRS into a
**three-tier distributed system**:

| Tier | Project | Role |
|------|---------|------|
| Client | `drs-client` | JavaFX + FXML GUI (SceneBuilder-compatible) |
| Server | `drs-server` | Multi-threaded socket server + MySQL DAO/service |
| Shared | `drs-common` | Serializable models + client/server protocol |
| Database | `database/` | Standalone MySQL script (schema also created in code) |

The three NetBeans projects are independent, so each can be assigned to a
different team member. `drs-common` is the shared contract both other tiers
depend on.

## Enhanced features added in Assessment 3

1. **Resource & Dispatch Management** — each department owns deployable
   resource units (fire trucks, ambulances, rescue teams, etc.). Coordinators
   allocate units to a disaster; the system checks availability, records the
   dispatch, and decrements availability inside a single database transaction.
   Units can be recalled, returning them to the pool. Over-dispatch is rejected
   and rolled back.
2. **Real-time Multi-user Alerts & Status Broadcast** — the multi-threaded
   server pushes live alerts to every connected client. When a disaster is
   reported, assessed as HIGH priority, departments are notified, or resources
   are dispatched, all operators see a live banner update without refreshing.

## Build order

`drs-common` must be built first because it produces `drs-common.jar`, which
the server and client depend on. A pre-built copy of `drs-common.jar` is
already placed in `drs-server/libs/` and `drs-client/libs/`, so the projects
run out of the box. If you change anything in `drs-common`, rebuild it and copy
`drs-common/dist/drs-common.jar` back into both `libs/` folders.

## Prerequisites

- JDK 11 or newer
- MySQL Server (or MariaDB) running on `localhost:3306`
- NetBeans (with the bundled Ant) — or Ant on the command line
- The JavaFX SDK is bundled in `drs-client/javafx-sdk/`

### Database credentials

The server connects as user `root` with an **empty password** by default. To
change this, edit the constants at the top of
`drs-server/src/drs/server/db/DatabaseConfig.java` (`USER`, `PASSWORD`, `HOST`,
`PORT`).

### JDBC driver

A MySQL-compatible JDBC driver (`mariadb-java-client-2.7.6.jar`) is bundled in
`drs-server/libs/`, so the server runs against a MySQL database with no extra
setup. To use the **official MySQL Connector/J** instead, download
`mysql-connector-j-<version>.jar` and drop it into `drs-server/libs/` — the
server auto-detects and prefers it.

## How to run

1. **Start MySQL** so it is listening on `localhost:3306`.
   (Optional: run `database/drs_enhanced_schema.sql` to create the database
   manually. This is not required — the server creates the schema and all
   tables programmatically on first launch.)

2. **Run the server** — open the `drs-server` project and Run it
   (main class `drs.server.net.DrsServer`), or from a terminal:
   ```
   cd drs-server
   ant run
   ```
   On first launch it creates the `drs_enhanced` database, all tables, and
   seeds default departments, users, and resources. It then listens on
   port 5000.

3. **Run one or more clients** — open the `drs-client` project and Run it
   (main class `drs.client.MainApp`), or from a terminal:
   ```
   cd drs-client
   ant run
   ```
   Launch the client several times to see the real-time alert broadcast
   between multiple users.

## Demo accounts

| Username | Password | Role | Capabilities |
|----------|----------|------|--------------|
| `admin` | `admin123` | ADMIN | Full access incl. user management |
| `coordinator` | `coord123` | COORDINATOR | Assess, notify, dispatch, recall |
| `staff` | `staff123` | DEPARTMENT_STAFF | Update response status |
| `public` | `pub123` | PUBLIC | Report disasters only |

## Tests

Each project includes JUnit 4 tests runnable with `ant test`:

- `drs-common` — model and protocol logic (no database required).
- `drs-server` — service-layer integration tests, including the Feature 1
  dispatch transaction. These run when a MySQL server is available and are
  skipped (not failed) otherwise.

## Project layout

```
DRS-Enhanced/
├── README.md
├── database/
│   └── drs_enhanced_schema.sql        Standalone DB script
├── drs-common/                        Shared library (build first)
│   ├── src/drs/common/                Models + protocol
│   ├── test/drs/common/               JUnit tests
│   ├── libs/                          junit, hamcrest
│   ├── nbproject/  build.xml
├── drs-server/                        Server tier
│   ├── src/drs/server/{db,dao,service,net}/
│   ├── test/drs/server/               JUnit tests
│   ├── libs/                          drs-common.jar, JDBC driver, junit
│   ├── nbproject/  build.xml
└── drs-client/                        Client tier
    ├── src/drs/client/{net,controller,view}/
    ├── libs/                          drs-common.jar
    ├── javafx-sdk/                    Bundled JavaFX SDK
    ├── nbproject/  build.xml
```
