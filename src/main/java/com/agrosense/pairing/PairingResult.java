package com.agrosense.pairing;

/**
 * Result object returned from the Chain of Responsibility.
 */
public class PairingResult {
    private final boolean success;
    private final String failureReason;

    private PairingResult(boolean success, String failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    public static PairingResult ok() {
        return new PairingResult(true, null);
    }

    public static PairingResult fail(String reason) {
        return new PairingResult(false, reason);
    }

    public boolean isSuccess() { return success; }
    public String getFailureReason() { return failureReason; }
}
