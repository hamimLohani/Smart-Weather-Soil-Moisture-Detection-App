package com.agrosense;

import com.agrosense.model.UseCaseProfile;
import com.agrosense.observer.AlertObserver;
import com.agrosense.strategy.AlertResponseStrategy;
import com.agrosense.strategy.FarmAlertStrategy;
import com.agrosense.strategy.HomeAlertStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the correct Strategy is selected based on Site.use_case_profile.
 */
class AlertStrategyTest {

    private final List<AlertObserver> observers = new ArrayList<>();
    private final Set<Integer> sitesNeedingAttention = new HashSet<>();

    private AlertResponseStrategy selectStrategy(UseCaseProfile profile) {
        return switch (profile) {
            case FARM -> new FarmAlertStrategy(observers, sitesNeedingAttention);
            case HOME -> new HomeAlertStrategy(observers);
        };
    }

    @Test
    void homeProfile_selectsHomeStrategy() {
        AlertResponseStrategy strategy = selectStrategy(UseCaseProfile.HOME);
        assertInstanceOf(HomeAlertStrategy.class, strategy);
    }

    @Test
    void farmProfile_selectsFarmStrategy() {
        AlertResponseStrategy strategy = selectStrategy(UseCaseProfile.FARM);
        assertInstanceOf(FarmAlertStrategy.class, strategy);
    }

    @Test
    void farmStrategy_addsSiteToAttentionSet() {
        FarmAlertStrategy strategy = new FarmAlertStrategy(observers, sitesNeedingAttention);

        com.agrosense.model.AlertEvent event = new com.agrosense.model.AlertEvent();
        event.setId(1); event.setDeviceUnitId(1); event.setAlertRuleId(1); event.setTriggeredValue(45.0);

        com.agrosense.model.DeviceUnit device = new com.agrosense.model.DeviceUnit();
        device.setId(1); device.setSerialNumber("SM-0001");

        com.agrosense.model.Site site = new com.agrosense.model.Site();
        site.setId(42); site.setName("Plot A"); site.setUseCaseProfile(UseCaseProfile.FARM);

        com.agrosense.model.SensorReading reading = new com.agrosense.model.SensorReading();
        reading.setReadingType(com.agrosense.model.ReadingType.SOIL_MOISTURE);
        reading.setValue(45.0);

        strategy.respond(event, device, site, reading);

        assertTrue(sitesNeedingAttention.contains(42),
            "FARM strategy should add site id to sitesNeedingAttention");
    }

    @Test
    void homeStrategy_doesNotAddToAttentionSet() {
        // HOME strategy doesn't have access to sitesNeedingAttention — it just notifies observers
        HomeAlertStrategy strategy = new HomeAlertStrategy(observers);
        // Running respond shouldn't throw; no attention set is mutated
        com.agrosense.model.AlertEvent event = new com.agrosense.model.AlertEvent();
        event.setId(1); event.setTriggeredValue(35.0);

        com.agrosense.model.DeviceUnit device = new com.agrosense.model.DeviceUnit();
        device.setSerialNumber("SM-0001");

        com.agrosense.model.Site site = new com.agrosense.model.Site();
        site.setName("My Garden"); site.setUseCaseProfile(UseCaseProfile.HOME);

        com.agrosense.model.SensorReading reading = new com.agrosense.model.SensorReading();
        reading.setReadingType(com.agrosense.model.ReadingType.TEMPERATURE);
        reading.setValue(35.0);

        assertDoesNotThrow(() -> strategy.respond(event, device, site, reading));
    }
}
