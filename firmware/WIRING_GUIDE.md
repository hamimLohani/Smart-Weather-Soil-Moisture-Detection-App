# AgroSense Node — Hardware Wiring Guide

**Board:** ESP8266 NodeMCU v1.0 (CP2102)  
**Sensors:** DHT11 (Temperature & Humidity) + Capacitive Soil Moisture Sensor v1.2

---

## Components List

| Component | Qty |
|---|---|
| ESP8266 NodeMCU v1.0 | 1 |
| DHT11 Sensor (3-pin module) | 1 |
| Capacitive Soil Moisture Sensor v1.2 | 1 |
| Micro USB cable (power + programming) | 1 |
| Jumper wires (Female-to-Female) | ~8 |
| Breadboard (optional, for stability) | 1 |

---

## Pin Reference (NodeMCU Labels → GPIO)

| NodeMCU Label | GPIO | Notes |
|---|---|---|
| D0 | GPIO16 | No interrupt support |
| D1 | GPIO5 | I2C SCL |
| D2 | GPIO4 | I2C SDA |
| **D3** | **GPIO0** | **Factory Reset Button (firmware)** |
| **D4** | **GPIO2** | **DHT11 Data (firmware)** |
| **A0** | **ADC0** | **Soil Moisture Analog In (firmware)** |
| 3V3 | — | 3.3V power output |
| 5V / VIN | — | 5V power from USB |
| GND | — | Ground |

---

## Wiring Diagram

```
NodeMCU                     DHT11 Module (3-pin)
─────────────────           ─────────────────────
3V3  ─────────────────────► VCC  (power)
GND  ─────────────────────► GND  (ground)
D4   ─────────────────────► DATA (signal)


NodeMCU                     Capacitive Soil Sensor v1.2
─────────────────           ─────────────────────────────
3V3  ─────────────────────► VCC  (power)
GND  ─────────────────────► GND  (ground)
A0   ─────────────────────► AOUT (analog signal)


NodeMCU                     Factory Reset Button
─────────────────           ─────────────────────
D3   ─────────────────────► One leg of button
GND  ─────────────────────► Other leg of button
(Internal pull-up enabled in firmware — button reads LOW when pressed)
```

---

## Visual Layout

```
                    ┌─────────────────────────────────────────┐
                    │         ESP8266 NodeMCU v1.0             │
                    │                                          │
        DHT11 VCC ──┤ 3V3                               VIN   │
        DHT11 GND ──┤ GND                               GND   │
       DHT11 DATA ──┤ D4 (GPIO2)                        RST   │
                    │                                   EN    │
Reset Button Leg ───┤ D3 (GPIO0)                        3V3   │
     Reset GND  ────┤ GND                               GND   │
                    │                                          │
 Soil Sensor VCC ───┤ 3V3                               CLK   │
 Soil Sensor GND ───┤ GND                               SD0   │
Soil Sensor AOUT ───┤ A0 ──────────────────────────────────── │
                    │                                   CMD   │
                    │                             [USB PORT]  │
                    └─────────────────────────────────────────┘
```

---

## Step-by-Step Wiring

### 1. DHT11 → NodeMCU

> If your DHT11 is a **bare 4-pin sensor** (not a 3-pin breakout module),  
> connect only pins 1 (VCC), 2 (DATA), 4 (GND) and add a **10kΩ pull-up  
> resistor between DATA and VCC**. The 3-pin module already has this built-in.

| DHT11 Pin | Wire Color (suggested) | NodeMCU Pin |
|---|---|---|
| VCC (+) | Red | 3V3 |
| GND (-) | Black | GND |
| DATA (S) | Yellow | D4 |

### 2. Capacitive Soil Moisture Sensor → NodeMCU

> Use the **capacitive** sensor (not resistive). It's the long PCB that you  
> insert into soil. It has 3 pins on the top edge.

| Sensor Pin | Wire Color (suggested) | NodeMCU Pin |
|---|---|---|
| VCC | Red | 3V3 |
| GND | Black | GND |
| AOUT | Yellow/Blue | A0 |

> ⚠️ **Important:** The NodeMCU has only **one analog pin (A0)**, and it  
> accepts **0–1V** max (not 3.3V). The capacitive sensor v1.2 outputs  
> 0–3V. If your readings are always at extremes (0 or 1023), add a  
> **voltage divider (2×10kΩ)** between AOUT and A0.  
> Most v1.2 breakouts are fine powered at 3.3V and output a safe range.

### 3. Factory Reset Button → NodeMCU

| Button | NodeMCU Pin |
|---|---|
| Leg 1 | D3 (GPIO0) |
| Leg 2 | GND |

> The firmware uses `INPUT_PULLUP` mode on D3, so no resistor is needed.  
> Hold button during power-on → clears EEPROM → enters Provisioning Mode.

---

## Power

Power the NodeMCU via the **Micro USB port** — from a laptop (for programming/demo)  
or a standard 5V USB power adapter (for field deployment).  
**Do not** power via 3V3 pin and USB simultaneously.

---

## Calibration Values (Important for Soil Readings)

The firmware sends **raw ADC values** (0–1023) to the JavaFX app, which  
converts them to a percentage using device-specific calibration stored in  
the database:

| State | Sensor in Air (Dry) | Sensor in Water (Wet) |
|---|---|---|
| Typical ADC Value | ~820 | ~380 |

**To calibrate your specific sensor:**
1. Power the circuit and open the Arduino Serial Monitor at 115200 baud
2. Hold the sensor **in open air** → note the `soil_moisture_raw` value → this is your `dry_calibration_value`
3. Submerge the tip **in water** → note the value → this is your `wet_calibration_value`
4. Update these values in the JavaFX app's database for your device (`DeviceUnit` table)

---

## Quick Sanity Check (Before Flashing)

1. Open Arduino Serial Monitor at **115200 baud**
2. On boot you should see:
   ```
   --- AgroSense Sensor Node Starting ---
   Chip ID (raw):  A1B2C3
   Device Serial:  SM-A1B2C3
   Pairing Code:   00A1-B2C3
   ```
3. A WiFi network named `AgroSense-00A1-B2C3` should appear in your WiFi list
4. Connect → go to `192.168.4.1` → enter your WiFi credentials and server IP
