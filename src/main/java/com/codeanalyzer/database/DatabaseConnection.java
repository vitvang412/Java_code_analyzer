package com.codeanalyzer.database;

import com.codeanalyzer.util.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton that manages a single MySQL connection.
 * Automatically creates the 'code_analyzer' database if it does not exist.
 * Call DatabaseConnection.getInstance().getConnection() anywhere in the app.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        ensureDatabaseExists();
        connect();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    /**
     * Connects to MySQL WITHOUT specifying a database, then runs
     * CREATE DATABASE IF NOT EXISTS so the app never crashes on first run.
     */
    private void ensureDatabaseExists() {
        AppConfig cfg = AppConfig.getInstance();
        // Build a URL that points only to the server (no database name)
        String serverUrl = buildServerUrl(cfg.dbUrl());
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection c = DriverManager.getConnection(serverUrl, cfg.dbUsername(), cfg.dbPassword());
                 Statement st = c.createStatement()) {
                st.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS `code_analyzer` " +
                    "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                System.out.println("[DB] Database 'code_analyzer' is ready.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] Could not ensure database exists: " + e.getMessage());
        }
    }

    /**
     * Strips the database name from a JDBC URL so we can connect to the server root.
     * e.g. "jdbc:mysql://localhost:3306/code_analyzer?..." -> "jdbc:mysql://localhost:3306/?..."
     */
    private String buildServerUrl(String fullUrl) {
        // Find the path part between host:port/ and the query string
        int schemeEnd = fullUrl.indexOf("://");
        if (schemeEnd == -1) return fullUrl;
        int slashAfterHost = fullUrl.indexOf('/', schemeEnd + 3);
        if (slashAfterHost == -1) return fullUrl;
        int queryStart = fullUrl.indexOf('?', slashAfterHost);
        if (queryStart == -1) {
            return fullUrl.substring(0, slashAfterHost + 1);
        }
        return fullUrl.substring(0, slashAfterHost + 1) + fullUrl.substring(queryStart);
    }

    private void connect() {
        AppConfig cfg = AppConfig.getInstance();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    cfg.dbUrl(), cfg.dbUsername(), cfg.dbPassword());
            System.out.println("[DB] Connected to MySQL successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] MySQL driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
        }
    }

    /** Returns a live connection, reconnecting if needed. */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("[DB] Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("[DB] isClosed() check failed: " + e.getMessage());
            connect();
        }
        return connection;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }
}
