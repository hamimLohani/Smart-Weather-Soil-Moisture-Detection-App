# AgroSense — Smart Weather & Soil Moisture Detection Platform

A JavaFX desktop application for managing a fleet of IoT weather/soil-moisture
monitoring devices, built for the Design Patterns Lab Final Project.

## Team

| Name | Roll |
|------------------------------|---------|
| Md Inzamamul Lohani | 1639 |
| Akeaid Moonjin Dayeen | 1640 |

## Project Overview

AgroSense is the business/operations-side desktop application for a company
that manufactures and sells IoT weather and soil-moisture detection devices
(ESP8266 + DHT11 + capacitive soil sensor + OLED display). Each physical
device ships with a unique **pairing code**. Customers pair their device
through this application, assign it to a **Site** (e.g. a garden or a farm
plot), and monitor live readings, configure alert rules, and review
historical trends.

The application is not a simple CRUD tool — the pairing process, alert
evaluation, and device lifecycle all involve real multi-step business rules,
which is where the design patterns below are applied.

## Tech Stack

- **Java 17+**
- **JavaFX** (Controls + FXML) — desktop UI
- **Maven** — build and dependency management
- **SQLite** (via `sqlite-jdbc`) — persistent local database
- **JDK built-in `HttpServer`** (`com.sun.net.httpserver`) — embedded HTTP
  listener that receives live sensor readings from real ESP8266 hardware
- **ArduinoJson / ESP8266WiFi / DHT** (firmware side, C++) — not part of the
  Java build, included separately under `/firmware`

## How to Run

```bash
# 1. Clone the repository
git clone <repo-url>
cd agrosense

# 2. Build with Maven
mvn clean install

# 3. Run the application
mvn javafx:run
```

On first launch, the app automatically creates the SQLite database file
(`agrosense.db`) and runs the **Seeder**, which creates all tables and
inserts sample Customers, Sites, DeviceUnits, and unused PairingCodes so the
app is immediately explorable without manual setup. Use the **"Reset Demo
Data"** option in the Settings screen to wipe and re-seed at any time.

The embedded sensor-ingestion HTTP server starts automatically on
`localhost:8080` and listens at `POST /api/readings` for data from real
ESP8266 devices on the same network.

## Database Schema

SQLite database with 8 core tables:

- **Customer** — end users who purchase devices (segment: `HOME` / `COMMERCIAL`)
- **Product** — device models sold
- **Site** — a customer's monitored location (garden, farm plot, etc.),
  tagged with a `use_case_profile` (`HOME` / `FARM`) that drives alert behavior
- **DeviceUnit** — a physical device, tracked by unique serial number and
  lifecycle status (`UNPAIRED` / `PAIRED` / `FAULTY`)
- **PairingCode** — one-time code shipped with each device, used to link it
  to a customer during onboarding
- **DevicePairing** — the active (or historical) link between a DeviceUnit,
  a Customer, and a Site
- **SensorReading** — time-series data (`TEMPERATURE` / `HUMIDITY` /
  `SOIL_MOISTURE`) reported by paired devices
- **AlertRule** / **AlertEvent** — per-site threshold rules and the alert
  instances they generate

Full schema with constraints and foreign keys is in
[`/db/schema.sql`](./db/schema.sql). An ER diagram is provided in
[`/docs/er-diagram.md`](./docs/er-diagram.md).

## Architecture

Strict layered separation:

```
model/      → plain data classes (Customer, Site, DeviceUnit, ...)
dao/        → SQLite access only (one DAO per entity), PreparedStatement-based
service/    → business logic; UI never talks to DAOs directly
pairing/    → Chain of Responsibility validation handlers
state/      → State pattern implementations
strategy/   → Strategy pattern implementations
observer/   → Observer pattern implementations
ingest/     → embedded HttpServer receiving ESP8266 sensor POSTs
ui/         → JavaFX controllers
resources/fxml/ → screen layouts
```

## Core Workflows

**1. Device Pairing**
Customer enters a pairing code → validated through a Chain of Responsibility
(code exists → not expired → not already used → device available) → on
success, a `DevicePairing` is created linking the device to the customer and
a named Site.

**2. Sensor Reading Ingestion & Alerting**
ESP8266 device POSTs a JSON reading → server checks the device has an active
pairing (rejects with `403` if not) → raw soil-moisture value is converted to
a percentage using the device's stored calibration values → readings are
stored → applicable `AlertRule`s are evaluated → if breached, an `AlertEvent`
is created via a Strategy chosen by the Site's use-case profile, and
registered Observers are notified.

## Design Patterns

Four patterns are used, each addressing a specific real design problem:

| Pattern | Location | Problem it solves |
|---|---|---|
| **Chain of Responsibility** | `pairing/` | Pairing-code validation requires several independent checks (existence, expiry, usage, device availability); each is a separate handler so new validation rules can be added without touching existing ones |
| **State** | `state/` | `DeviceUnit` and `AlertEvent` each have a lifecycle with transition-dependent behavior; modeled as State classes rather than status flags with conditional logic |
| **Strategy** | `strategy/` | Alert response differs by Site profile (`HOME` vs `FARM`); new customer segments can be added as new strategies without modifying alert-evaluation logic |
| **Observer** | `observer/` | Alert notifications need to reach multiple independent consumers (dashboard, log) without coupling alert-creation logic to notification channels |

Full justification for each pattern (problem, alternatives considered, and
future extensibility) is documented in
[`/docs/design-patterns.md`](./docs/design-patterns.md).

## Testing

JUnit test cases cover the core business logic, located in `src/test/java`:

- Chain of Responsibility pairing validation (all failure paths)
- Strategy selection logic per Site profile
- Soil-moisture raw-to-percentage calibration conversion

Run tests with:
```bash
mvn test
```

## Hardware Integration (Demonstration Layer)

While not required by the course rubric, this project includes real ESP8266
firmware (`/firmware`) for end-to-end demonstration:

- ESP8266 + DHT11 + capacitive soil moisture sensor + OLED display
- First-boot WiFi provisioning via a self-hosted access point, where the
  device's pairing code doubles as the hotspot password
- Live sensor data POSTed as JSON to the embedded ingestion server described
  above

This layer is a demo enhancement; all graded deliverables (schema, patterns,
workflows, reporting, and JavaFX UI) function independently of any connected
hardware.

## Repository & Git Practices

- Feature branches per task, merged via pull request into `main`
- Individual contributions traceable through commit history
- See `/docs` for UML class diagrams, ER diagram, and full design
  documentation

## Documentation

- [`/docs/design-patterns.md`](./docs/design-patterns.md) — pattern
  justifications
- [`/docs/er-diagram.md`](./docs/er-diagram.md) — database ER diagram
- [`/docs/class-diagrams/architecture.md`](./docs/class-diagrams/architecture.md) — UML class diagrams for
  major design decisions
- [`/db/schema.sql`](./db/schema.sql) — full SQLite schema
