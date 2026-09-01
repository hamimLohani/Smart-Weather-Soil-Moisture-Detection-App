package com.agrosense.session;

import com.agrosense.model.Customer;

/**
 * Application-wide session holder (singleton).
 * Stores the currently authenticated Customer.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private Customer currentCustomer;

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
    }

    public boolean isLoggedIn() {
        return currentCustomer != null;
    }

    public void logout() {
        currentCustomer = null;
    }
}
