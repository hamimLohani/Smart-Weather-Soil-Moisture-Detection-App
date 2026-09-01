package com.agrosense.state;

import com.agrosense.model.DeviceStatus;

public class UnpairedState implements DeviceUnitState {
    @Override public DeviceUnitState pair()       { return new PairedState(); }
    @Override public DeviceUnitState markFaulty() { return new FaultyState(); }
    @Override public DeviceUnitState unpair()     { return this; } // already unpaired
    @Override public DeviceStatus getStatus()     { return DeviceStatus.UNPAIRED; }
}
