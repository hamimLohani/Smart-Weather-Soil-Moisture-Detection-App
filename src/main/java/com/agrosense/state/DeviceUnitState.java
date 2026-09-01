package com.agrosense.state;

import com.agrosense.model.DeviceStatus;

/**
 * State interface for DeviceUnit lifecycle.
 */
public interface DeviceUnitState {
    DeviceUnitState pair();
    DeviceUnitState markFaulty();
    DeviceUnitState unpair();
    DeviceStatus getStatus();
}
