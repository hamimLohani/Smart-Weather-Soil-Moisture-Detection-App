package com.agrosense.state;

import com.agrosense.model.AlertEventStatus;

public class AcknowledgedState implements AlertEventState {
    @Override public AlertEventState acknowledge() { return this; } // already ack'd
    @Override public AlertEventState resolve()     { return new ResolvedState(); }
    @Override public AlertEventStatus getStatus()  { return AlertEventStatus.ACKNOWLEDGED; }
}
