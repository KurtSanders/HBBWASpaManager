# [RELEASE] HB BWA Spa Manager - Local Control (Direct TCP / Elfin EW11) v2.3.0

I've released a minor feature and optimization update (**v2.3.0**) for **BWA Spa Manager - Local Control** (`Balboa Hot Tub Local Driver` v2.3.0, `Balboa Hot Tub Local Child Switch` v2.1.0, `BWA Spa Manager App - Local`, and `Balboa-Hot-Tub-API-Library`).

If you're running a Balboa BP-series spa with an **Elfin EW11 RS485-to-WiFi** adapter (or local TCP bridge on port `4257`), this update adds a stateful High/Low preset temperature child switch, makes polling significantly faster, fixes several protocol/bitmask edge cases, and adds real-time child switch synchronization for Alexa and Hubitat Dashboards.

---

### What's New & Improved

* **Preset High/Low Temperature Child Switch (`Spa Preset Temp (High/Low)`):**
  * Two new numeric preferences (**Preset High Temperature**, default `102`, and **Preset Low Temperature**, default `80`) let you define your preferred soak and standby setpoints.
  * Turning the new **Spa Preset Temp (High/Low)** child switch **ON** sets the spa's target temperature to your High preset; turning it **OFF** sets it to your Low preset, and the switch state stays synced with live spa setpoint updates.

* **~0.5s Fast-Close Polling:** Routine `refresh()` polls now close the TCP socket immediately after receiving and validating the first fresh `Panel Status Update` (`0x13`) frame instead of waiting for 5 redundant broadcasts. `configure()` and `getFilterSchedule()` still keep the socket open for `0x2E` / `0x23` responses.
* **Real-Time Component Switch Sync (Alexa / Google Home / Dashboards):**
  * `Generic Component Switch` child devices created via **Create Child Devices** (`Pump1`–`Pump6`, `Light1`–`Light2`) now stay synced with the spa's live state when changed from the topside panel.
  * Turning a pump or light `on`/`off` checks the current state first (including 2-press cycling to turn a `Low` 2-speed pump `off`) instead of blindly toggling.
* **Filter Cycle Polling (`getFilterSchedule`):** Added a `getFilterSchedule` command and chained it after `configure()` (`0x2E`) to populate `Filter1Start`, `Filter1Duration`, `isFilter2Active`, `Filter2Start`, and `Filter2Duration` (with proper `H:mm` duration formatting).
* **Optional Auto-Clock Sync:** New driver preferences (`autoSyncClock` and `clockDriftThresholdMins`) automatically sync the spa's internal clock to your Hubitat hub's local time if drift exceeds your threshold (with a 1-hour cooldown).
* **Full Celsius (`°C`) Support:** Fixed Celsius setpoint validation (`10.0–26.5°C` low, `26.5–40.0°C` high) and `0.5°C` protocol encoding (`setpoint * 2`).
* **Protocol & Stability Fixes:**
  * Fixed signed-byte (`& 0xFF`) handling across panel, filter, and configuration parsers (including `255` unknown temp and Pump 4 high-speed bitmasks).
  * Fixed `Pump3`, `Pump4`, and `Pump6` speed-detection bitmasks in `ParseDeviceConfigurationData()`.
  * Fixed `setHeatMode()`, `thermostatOperatingState` (`pending heat` ordering), `Lights`, `Pumps`, and `Mister` attribute events.
  * Fixed `parse()` `maxResponsesMessages` typo, per-session CRC cache reset on `socket_connect()`, and `unschedule('socket_close')` on early disconnect.
  * Deduplicated unchanged attribute events in `makeEvent()` and compacted `spaPanelHTML` to stay well under Hubitat's 1024-byte attribute limit.
* **Local App Enhancements (`BWA Spa Manager App - Local`):**
  * Added IPv4 address validation, TCP `port` (`4257`), `pollInterval`, and a toggle for `createMomentarySwitches` directly in the app UI.
  * Fixed `ipaddress` sync type (`text` instead of `enum`) and `checkLogLevel()` in `updated()`.

---

### Upgrading / Installing

1. **Via Hubitat Package Manager (HPM):** Run **Update** in HPM (or **Install** → search for **BWA Spa Manager - Local Control**).
2. **Manual Install:** Update the App, Parent Driver, Child Switch Driver, and `Balboa-Hot-Tub-API-Library` from the [GitHub repository](https://github.com/KurtSanders/HBBWASpaManager).
3. After updating, open your **Balboa Hot Tub Local Driver** device, click **Save Preferences**, and run **Configure** followed by **Refresh**.

*Note: Setup is managed through **Apps → BWA Spa Manager App - Local**, which automatically creates and configures your parent `Balboa Hot Tub Local Driver` device—no manual virtual device creation needed.*

Feedback, logs, and feature requests are always welcome in this thread!
