# AgroSense Design Patterns

This document justifies the major design patterns used in the AgroSense platform.

## 1. Chain of Responsibility Pattern
**Location:** `com.agrosense.pairing.*`

**Problem:** The process of pairing a device using a code requires multiple sequential validations:
1. Does the code exist?
2. Is the code expired?
3. Has the code already been used?
4. Is the physical device available (not faulty)?
5. Is the device already paired?

**Solution:** The Chain of Responsibility pattern is used to decouple these checks. Each validation step is encapsulated in its own handler (e.g., `CodeExistsHandler`, `CodeNotExpiredHandler`). 
**Benefit:** We can easily add, remove, or reorder validation steps without modifying a monolithic `if-else` block in the `PairingService`.

## 2. State Pattern
**Location:** `com.agrosense.state.*`

**Problem:** Entities like `DeviceUnit` have lifecycles that dictate their behavior (e.g. `UNPAIRED` -> `PAIRED` -> `FAULTY`). Hardcoding conditionals based on status strings creates brittle code that's hard to maintain.

**Solution:** The State pattern represents each lifecycle phase as a distinct class (`UnpairedState`, `PairedState`, `FaultyState`).
**Benefit:** State transitions are controlled explicitly by the state classes. Illegal transitions (like moving an already `PAIRED` device directly back to `UNPAIRED` without unpairing) throw standard exceptions defined by the interface.

## 3. Strategy Pattern
**Location:** `com.agrosense.strategy.*`

**Problem:** An alert behaves differently depending on the `UseCaseProfile` of the `Site` (`HOME` vs `FARM`). A home garden might just log an event, while a farm plot might trigger immediate escalation.

**Solution:** The Strategy pattern encapsulates the alert response logic into profile-specific classes (`HomeAlertStrategy`, `FarmAlertStrategy`).
**Benefit:** When a new customer tier is added (e.g. `COMMERCIAL_GREENHOUSE`), we simply implement a new `AlertResponseStrategy` without touching the core `AlertService` evaluation loop.

## 4. Observer Pattern
**Location:** `com.agrosense.observer.*`

**Problem:** When a new `AlertEvent` is generated, multiple disconnected components need to react. For example, the live JavaFX Dashboard needs to increment a counter, and a background logger needs to record the event.

**Solution:** The Observer pattern (`AlertObserver`) allows the `AlertService` to broadcast events to a list of subscribers (`DashboardAlertObserver`, `LogAlertObserver`).
**Benefit:** The `AlertService` doesn't need to know anything about the UI or the logging system. It simply notifies registered observers, keeping the domains strictly decoupled.
