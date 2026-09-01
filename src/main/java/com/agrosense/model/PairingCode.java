package com.agrosense.model;

import java.time.LocalDate;

public class PairingCode {
    private int id;
    private String code;
    private int deviceUnitId;
    private PairingCodeStatus status;
    private LocalDate generatedDate;
    private LocalDate usedDate;

    public PairingCode() {}

    public PairingCode(int id, String code, int deviceUnitId, PairingCodeStatus status,
                       LocalDate generatedDate, LocalDate usedDate) {
        this.id = id;
        this.code = code;
        this.deviceUnitId = deviceUnitId;
        this.status = status;
        this.generatedDate = generatedDate;
        this.usedDate = usedDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getDeviceUnitId() { return deviceUnitId; }
    public void setDeviceUnitId(int deviceUnitId) { this.deviceUnitId = deviceUnitId; }

    public PairingCodeStatus getStatus() { return status; }
    public void setStatus(PairingCodeStatus status) { this.status = status; }

    public LocalDate getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }

    public LocalDate getUsedDate() { return usedDate; }
    public void setUsedDate(LocalDate usedDate) { this.usedDate = usedDate; }

    public boolean isExpired() {
        // Codes expire after 365 days from generation
        return generatedDate != null && LocalDate.now().isAfter(generatedDate.plusDays(365));
    }
}
