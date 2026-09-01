package com.agrosense.state;

import com.agrosense.model.AlertEventStatus;

/**
 * State interface for AlertEvent lifecycle.
 */
public interface AlertEventState {
    AlertEventState acknowledge();
    AlertEventState resolve();
    AlertEventStatus getStatus();
}
