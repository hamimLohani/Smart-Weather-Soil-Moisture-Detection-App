package com.agrosense.service;

import com.agrosense.dao.AlertRuleDAO;
import com.agrosense.model.AlertRule;
import com.agrosense.model.ComparisonOperator;
import com.agrosense.model.ReadingType;

import java.sql.SQLException;
import java.util.List;

public class AlertRuleService {

    private final AlertRuleDAO alertRuleDAO;

    public AlertRuleService(AlertRuleDAO alertRuleDAO) {
        this.alertRuleDAO = alertRuleDAO;
    }

    public AlertRule createRule(int siteId, ReadingType type, ComparisonOperator op,
                                double threshold) throws SQLException {
        return alertRuleDAO.insert(siteId, type, op, threshold);
    }

    public List<AlertRule> getRulesForSite(int siteId) throws SQLException {
        return alertRuleDAO.findBySite(siteId);
    }

    public List<AlertRule> getActiveRulesForSite(int siteId) throws SQLException {
        return alertRuleDAO.findActiveBySite(siteId);
    }

    public void updateRule(AlertRule rule) throws SQLException {
        alertRuleDAO.update(rule);
    }

    public void deleteRule(int ruleId) throws SQLException {
        alertRuleDAO.delete(ruleId);
    }
}
