package com.agrosense.service;

import com.agrosense.dao.SiteDAO;
import com.agrosense.model.Site;
import com.agrosense.model.UseCaseProfile;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SiteService {

    private final SiteDAO siteDAO;
    private final com.agrosense.dao.DevicePairingDAO devicePairingDAO;

    public SiteService(SiteDAO siteDAO, com.agrosense.dao.DevicePairingDAO devicePairingDAO) {
        this.siteDAO = siteDAO;
        this.devicePairingDAO = devicePairingDAO;
    }

    public List<Site> getSitesForCustomer(int customerId) throws SQLException {
        return siteDAO.findByCustomer(customerId);
    }

    public Optional<Site> getSiteById(int id) throws SQLException {
        return siteDAO.findById(id);
    }

    public Site createSite(int customerId, String name, UseCaseProfile profile) throws SQLException {
        return siteDAO.insert(customerId, name, profile);
    }

    public void updateSite(Site site) throws SQLException {
        siteDAO.update(site);
    }

    public void deleteSite(int siteId) throws SQLException {
        siteDAO.delete(siteId);
    }
}
