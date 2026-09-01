package com.agrosense.pairing;

import com.agrosense.dao.PairingCodeDAO;
import com.agrosense.model.PairingCode;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Handler 1: Verifies the pairing code exists in the database.
 */
public class CodeExistsHandler extends PairingHandler {

    private final PairingCodeDAO pairingCodeDAO;

    public CodeExistsHandler(PairingCodeDAO pairingCodeDAO) {
        this.pairingCodeDAO = pairingCodeDAO;
    }

    @Override
    public PairingResult handle(String code, Optional<PairingCode> resolved) {
        try {
            Optional<PairingCode> found = pairingCodeDAO.findByCode(code.trim().toUpperCase());
            if (found.isEmpty()) {
                return PairingResult.fail("No device found with that pairing code.");
            }
            return passToNext(code, found);
        } catch (SQLException e) {
            return PairingResult.fail("Database error while validating code: " + e.getMessage());
        }
    }
}
