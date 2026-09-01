package com.agrosense.state;

import com.agrosense.model.AlertEventStatus;

public class OpenState implements AlertEventState {
    @Override public AlertEventState acknowledge() { return new AcknowledgedState(); }
    @Override public AlertEventState resolve()     { return new ResolvedState(); }
    @Override public AlertEventStatus getStatus()  { return AlertEventStatus.OPEN; }
}
