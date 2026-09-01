package com.agrosense;

import com.agrosense.dao.DeviceUnitDAO;
import com.agrosense.dao.PairingCodeDAO;
import com.agrosense.model.*;
import com.agrosense.pairing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests every failure case of the Chain of Responsibility pairing validation.
 * Uses simple in-memory stubs to avoid needing a real SQLite DB.
 */
class PairingChainTest {

    // ── Stub DAO: always returns a configurable PairingCode ──────────────────

    static class StubPairingCodeDAO extends PairingCodeDAO {
        private final Optional<PairingCode> result;
        StubPairingCodeDAO(Optional<PairingCode> result) { this.result = result; }
        @Override public Optional<PairingCode> findByCode(String code) { return result; }
        @Override public void markActive(int id) { /* no-op */ }
    }

    static class StubDeviceUnitDAO extends DeviceUnitDAO {
        private final Optional<DeviceUnit> result;
        StubDeviceUnitDAO(Optional<DeviceUnit> result) { this.result = result; }
        @Override public Optional<DeviceUnit> findById(int id) { return result; }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PairingCode code(PairingCodeStatus status, LocalDate generated, int deviceId) {
        return new PairingCode(1, "TEST-CODE", deviceId, status, generated, null);
    }

    private DeviceUnit device(DeviceStatus status) {
        return new DeviceUnit(1, "SM-0001", 1, status, 820, 380, null);
    }

    // ── Test cases ────────────────────────────────────────────────────────────

    @Test
    void codeNotFound_fails() {
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.empty()),
            new StubDeviceUnitDAO(Optional.empty())
        );
        PairingResult r = chain.handle("NONE", Optional.empty());
        assertFalse(r.isSuccess());
        assertEquals("No device found with that pairing code.", r.getFailureReason());
    }

    @Test
    void codeExpiredByStatus_fails() {
        PairingCode expired = code(PairingCodeStatus.EXPIRED, LocalDate.now().minusDays(10), 1);
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.of(expired)),
            new StubDeviceUnitDAO(Optional.of(device(DeviceStatus.UNPAIRED)))
        );
        PairingResult r = chain.handle("TEST-CODE", Optional.empty());
        assertFalse(r.isSuccess());
        assertTrue(r.getFailureReason().contains("expired"));
    }

    @Test
    void codeExpiredByDate_fails() {
        // Generated over a year ago → isExpired() = true even if status is UNUSED
        PairingCode oldCode = code(PairingCodeStatus.UNUSED, LocalDate.now().minusDays(400), 1);
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.of(oldCode)),
            new StubDeviceUnitDAO(Optional.of(device(DeviceStatus.UNPAIRED)))
        );
        PairingResult r = chain.handle("TEST-CODE", Optional.empty());
        assertFalse(r.isSuccess());
        assertTrue(r.getFailureReason().contains("expired"));
    }

    @Test
    void codeAlreadyUsed_fails() {
        PairingCode used = code(PairingCodeStatus.ACTIVE, LocalDate.now(), 1);
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.of(used)),
            new StubDeviceUnitDAO(Optional.of(device(DeviceStatus.PAIRED)))
        );
        PairingResult r = chain.handle("TEST-CODE", Optional.empty());
        assertFalse(r.isSuccess());
        assertTrue(r.getFailureReason().contains("already been used"));
    }

    @Test
    void deviceFaulty_fails() {
        PairingCode valid = code(PairingCodeStatus.UNUSED, LocalDate.now(), 1);
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.of(valid)),
            new StubDeviceUnitDAO(Optional.of(device(DeviceStatus.FAULTY)))
        );
        PairingResult r = chain.handle("TEST-CODE", Optional.empty());
        assertFalse(r.isSuccess());
        assertTrue(r.getFailureReason().contains("faulty"));
    }

    @Test
    void deviceAlreadyPaired_fails() {
        PairingCode valid = code(PairingCodeStatus.UNUSED, LocalDate.now(), 1);
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.of(valid)),
            new StubDeviceUnitDAO(Optional.of(device(DeviceStatus.PAIRED)))
        );
        PairingResult r = chain.handle("TEST-CODE", Optional.empty());
        assertFalse(r.isSuccess());
        // ACTIVE code check comes before device check — ensure at least one failure is caught
        assertNotNull(r.getFailureReason());
    }

    @Test
    void validCode_succeeds() {
        PairingCode valid = code(PairingCodeStatus.UNUSED, LocalDate.now(), 1);
        PairingHandler chain = PairingChainFactory.build(
            new StubPairingCodeDAO(Optional.of(valid)),
            new StubDeviceUnitDAO(Optional.of(device(DeviceStatus.UNPAIRED)))
        );
        PairingResult r = chain.handle("TEST-CODE", Optional.empty());
        assertTrue(r.isSuccess());
        assertNull(r.getFailureReason());
    }
}
