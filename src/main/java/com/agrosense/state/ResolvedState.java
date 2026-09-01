package com.agrosense.state;

import com.agrosense.model.AlertEventStatus;

/** Terminal state — no transitions from RESOLVED. */
public class ResolvedState implements AlertEventState {
    @Override public AlertEventState acknowledge() { return this; }
    @Override public AlertEventState resolve()     { return this; }
    @Override public AlertEventStatus getStatus()  { return AlertEventStatus.RESOLVED; }
}
