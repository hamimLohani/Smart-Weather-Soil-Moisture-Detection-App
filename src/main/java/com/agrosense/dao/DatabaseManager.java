package com.agrosense.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Central database connection factory.
 * Every connection has PRAGMA foreign_keys = ON applied automatically.
 */
public class DatabaseManager {

    private static final String CONFIG_FILE = "/agrosense.properties";
    private static final String DEFAULT_DB_PATH = "agrosense.db";

    private static String dbUrl;

    static {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getResourceAsStream(CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("[DatabaseManager] Could not load config, using defaults: " + e.getMessage());
        }
        String dbPath = props.getProperty("db.path", DEFAULT_DB_PATH);
        dbUrl = "jdbc:sqlite:" + dbPath;
    }

    private DatabaseManager() {}

    /**
     * Opens and returns a new SQLite connection with foreign keys enabled.
     * Callers are responsible for closing via try-with-resources.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }
}
