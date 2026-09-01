CREATE TABLE IF NOT EXISTS Customer (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    phone      TEXT,
    email      TEXT NOT NULL UNIQUE,
    password_hash TEXT,
    salt          TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Product (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    model_name  TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS Site (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id      INTEGER NOT NULL REFERENCES Customer(id),
    name             TEXT NOT NULL,
    use_case_profile TEXT NOT NULL CHECK(use_case_profile IN ('HOME','FARM')),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS DeviceUnit (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    serial_number        TEXT NOT NULL UNIQUE,
    product_id           INTEGER NOT NULL REFERENCES Product(id),
    status               TEXT NOT NULL DEFAULT 'UNPAIRED'
                             CHECK(status IN ('UNPAIRED','PAIRED','FAULTY')),
    dry_calibration_value INTEGER NOT NULL DEFAULT 820,
    wet_calibration_value INTEGER NOT NULL DEFAULT 380,
    last_seen            DATETIME
);

CREATE TABLE IF NOT EXISTS PairingCode (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    code           TEXT NOT NULL UNIQUE,
    device_unit_id INTEGER NOT NULL REFERENCES DeviceUnit(id),
    status         TEXT NOT NULL DEFAULT 'UNUSED'
                       CHECK(status IN ('UNUSED','ACTIVE','EXPIRED','REVOKED')),
    generated_date DATE NOT NULL DEFAULT CURRENT_DATE,
    used_date      DATE
);

CREATE TABLE IF NOT EXISTS DevicePairing (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    device_unit_id INTEGER NOT NULL REFERENCES DeviceUnit(id),
    customer_id    INTEGER NOT NULL REFERENCES Customer(id),
    site_id        INTEGER NOT NULL REFERENCES Site(id),
    paired_date    DATETIME DEFAULT CURRENT_TIMESTAMP,
    unpaired_date  DATETIME,
    is_active      INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS SensorReading (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    device_unit_id INTEGER NOT NULL REFERENCES DeviceUnit(id),
    reading_type   TEXT NOT NULL CHECK(reading_type IN ('TEMPERATURE','HUMIDITY','SOIL_MOISTURE')),
    value          REAL NOT NULL,
    timestamp      DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sensor_device_time
    ON SensorReading(device_unit_id, timestamp);

CREATE TABLE IF NOT EXISTS AlertRule (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    site_id             INTEGER NOT NULL REFERENCES Site(id),
    reading_type        TEXT NOT NULL,
    comparison_operator TEXT NOT NULL,
    threshold_value     REAL NOT NULL,
    is_active           INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS AlertEvent (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    device_unit_id  INTEGER NOT NULL REFERENCES DeviceUnit(id),
    alert_rule_id   INTEGER NOT NULL REFERENCES AlertRule(id),
    triggered_value REAL NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OPEN'
                        CHECK(status IN ('OPEN','ACKNOWLEDGED','RESOLVED')),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at     DATETIME
);
