package com.agrosense.model;

public class AlertRule {
    private int id;
    private int siteId;
    private ReadingType readingType;
    private ComparisonOperator comparisonOperator;
    private double thresholdValue;
    private boolean active;

    public AlertRule() {}

    public AlertRule(int id, int siteId, ReadingType readingType,
                     ComparisonOperator comparisonOperator, double thresholdValue, boolean active) {
        this.id = id;
        this.siteId = siteId;
        this.readingType = readingType;
        this.comparisonOperator = comparisonOperator;
        this.thresholdValue = thresholdValue;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSiteId() { return siteId; }
    public void setSiteId(int siteId) { this.siteId = siteId; }

    public ReadingType getReadingType() { return readingType; }
    public void setReadingType(ReadingType readingType) { this.readingType = readingType; }

    public ComparisonOperator getComparisonOperator() { return comparisonOperator; }
    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(double thresholdValue) { this.thresholdValue = thresholdValue; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Evaluate whether the given sensor value triggers this rule. */
    public boolean isTriggered(double sensorValue) {
        return switch (comparisonOperator) {
            case GREATER_THAN          -> sensorValue >  thresholdValue;
            case LESS_THAN             -> sensorValue <  thresholdValue;
            case GREATER_THAN_OR_EQUAL -> sensorValue >= thresholdValue;
            case LESS_THAN_OR_EQUAL    -> sensorValue <= thresholdValue;
            case EQUAL                 -> sensorValue == thresholdValue;
        };
    }

    @Override
    public String toString() {
        return readingType + " " + comparisonOperator.getSymbol() + " " + thresholdValue;
    }
}
