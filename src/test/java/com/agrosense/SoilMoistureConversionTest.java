package com.agrosense;

import com.agrosense.model.DeviceUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests soil moisture raw → percentage conversion using DeviceUnit.convertSoilMoistureToPercent().
 * Capacitive sensor: dry = higher ADC value, wet = lower ADC value.
 *  percent = ((dry - raw) / (dry - wet)) * 100, clamped [0, 100]
 */
class SoilMoistureConversionTest {

    private DeviceUnit device(int dry, int wet) {
        DeviceUnit d = new DeviceUnit();
        d.setDryCalibrationValue(dry);
        d.setWetCalibrationValue(wet);
        return d;
    }

    /** Fully dry raw value → 0% */
    @Test
    void rawAtDry_returns0Percent() {
        DeviceUnit d = device(820, 380);
        assertEquals(0.0, d.convertSoilMoistureToPercent(820), 0.01);
    }

    /** Fully wet raw value → 100% */
    @Test
    void rawAtWet_returns100Percent() {
        DeviceUnit d = device(820, 380);
        assertEquals(100.0, d.convertSoilMoistureToPercent(380), 0.01);
    }

    /** Midpoint raw → 50% */
    @Test
    void rawAtMidpoint_returns50Percent() {
        DeviceUnit d = device(820, 380); // range = 440
        int mid = (820 + 380) / 2;      // = 600
        double result = d.convertSoilMoistureToPercent(mid);
        assertEquals(50.0, result, 0.5);
    }

    /** Raw below wet (over-wet sensor) → clamped to 100% */
    @Test
    void rawBelowWet_clampedTo100() {
        DeviceUnit d = device(820, 380);
        assertEquals(100.0, d.convertSoilMoistureToPercent(200), 0.01);
    }

    /** Raw above dry (very dry, outside range) → clamped to 0% */
    @Test
    void rawAboveDry_clampedTo0() {
        DeviceUnit d = device(820, 380);
        assertEquals(0.0, d.convertSoilMoistureToPercent(900), 0.01);
    }

    /** Zero-range calibration (dry == wet) → returns 0 safely, no division by zero */
    @Test
    void zeroRange_returnsZeroSafely() {
        DeviceUnit d = device(500, 500);
        assertDoesNotThrow(() -> d.convertSoilMoistureToPercent(500));
        assertEquals(0.0, d.convertSoilMoistureToPercent(500), 0.01);
    }

    /** Known raw value — 480 in range 820-380 → ~77.3% */
    @Test
    void knownRaw480_correctPercent() {
        DeviceUnit d = device(820, 380);
        // percent = (820 - 480) / (820 - 380) * 100 = 340/440*100 ≈ 77.27
        double result = d.convertSoilMoistureToPercent(480);
        assertEquals(77.27, result, 0.5);
    }
}
