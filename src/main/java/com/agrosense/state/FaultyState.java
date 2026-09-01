package com.agrosense.state;

import com.agrosense.model.DeviceStatus;

/** Terminal state — no transitions allowed from FAULTY. */
public class FaultyState implements DeviceUnitState {
    @Override public DeviceUnitState pair()       { return this; }
    @Override public DeviceUnitState markFaulty() { return this; }
    @Override public DeviceUnitState unpair()     { return this; }
    @Override public DeviceStatus getStatus()     { return DeviceStatus.FAULTY; }
}
