# Critical Crash Investigation Report: RideConnectX

## 1. Symptoms
* App launches and requests permissions normally.
* Connection to scooter initiates from the Scanner.
* Dashboard appears for ~1 second.
* App crashes immediately.
* Subsequent launches result in immediate crashes or "App isn't responding" (ANR).

## 2. Identified Root Causes

### A. Navigation Route Mismatch (Primary Crash)
The `StartupViewModel` was retrieving the raw Bluetooth MAC address from `SessionDataStore` and providing it directly as the `startDestination` to the `NavHost`.
* **Issue:** A MAC address (e.g., `AA:BB:CC...`) is not a valid route in the navigation graph.
* **Result:** `IllegalArgumentException` thrown by Jetpack Navigation because it couldn't find a destination matching the raw address.

### B. Infinite Recomposition Loop (Primary ANR)
The `NavHost` in `NavGraph.kt` was directly observing the `startDestination` from the `StartupViewModel`.
* **Flow:** 
    1. User connects successfully.
    2. `BleRepositoryImpl` calls `saveSession(address)`.
    3. `SessionDataStore` updates the flow.
    4. `StartupViewModel` emits a new `startDestination`.
    5. `NavGraph` recomposes, destroying and recreating the `NavHost`.
    6. `NavHost` starts again, recreating the `DashboardViewModel`.
    7. `DashboardViewModel` calls `connect()` again.
    8. Loop repeats indefinitely.
* **Result:** Application Not Responding (ANR) due to UI thread exhaustion.

### C. Foreground Service Lifecycle (Secondary Risk)
On Android 14+, starting a foreground service requires strict adherence to types. If `onStartCommand` was called while the service was already running but not in the foreground (due to system kills), it could lead to a crash if `startForeground` wasn't re-invoked.

---

## 3. Fixes Implemented

### Navigation & Session Logic (`NavGraph.kt`)
* **Route Mapping:** Added a `map` operator to `StartupViewModel` to transform the raw MAC address into a proper route: `dashboard/$address`.
* **Initialization Guard:** Added a check for `null` in `NavGraph` to ensure the `NavHost` doesn't attempt to start until the `DataStore` has provided the initial state.
* **Recomposition Lock:** Wrapped the `startDestination` in a `remember { ... }` block. This ensures that the `NavHost` only uses the *first* valid destination it receives at app startup, preventing runtime session updates from resetting the UI.

### BLE Service Stability (`BleForegroundService.kt`)
* **Null Safety:** Added explicit checks for the `BluetoothAdapter`.
* **Lifecycle Enforcement:** Updated `onStartCommand` to ensure `startForeground` is always called if the service is triggered by an intent other than a user disconnect. This satisfies Android's requirement that a service started via `startForegroundService` must call `startForeground` within 5 seconds.

---

## 4. Verification
* **Static Analysis:** Verified that `NavHost` now receives `dashboard/{address}` or `scan`.
* **Trace Analysis:** The `remember` block in `NavGraph` successfully breaks the loop between `SessionDataStore` updates and UI recreation.
* **Android 14 Compliance:** Added `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` logic checks.

## 5. Next Steps
1. **Test Connection:** Verify that the "1 second" window is now stable telemetry data.
2. **Permission Flow:** Proceed with refining the location/Bluetooth permission edge cases.
3. **Phase 4:** Begin actual telemetry parsing in `BleRepositoryImpl`.
