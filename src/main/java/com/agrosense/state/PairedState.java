package com.agrosense.state;

import com.agrosense.model.DeviceStatus;

public class PairedState implements DeviceUnitState {
    @Override public DeviceUnitState pair()       { return this; } // already paired
    @Override public DeviceUnitState markFaulty() { return new FaultyState(); }
    @Override public DeviceUnitState unpair()     { return new UnpairedState(); }
    @Override public DeviceStatus getStatus()     { return DeviceStatus.PAIRED; }
}
