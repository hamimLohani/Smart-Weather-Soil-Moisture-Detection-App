module com.agrosense {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;
    requires jdk.httpserver;

    opens com.agrosense to javafx.fxml;
    opens com.agrosense.ui to javafx.fxml;
    opens com.agrosense.model to javafx.base;

    exports com.agrosense;
    exports com.agrosense.model;
    exports com.agrosense.dao;
    exports com.agrosense.service;
    exports com.agrosense.session;
    exports com.agrosense.pairing;
    exports com.agrosense.state;
    exports com.agrosense.strategy;
    exports com.agrosense.observer;
    exports com.agrosense.ingest;
    exports com.agrosense.ui;
}
