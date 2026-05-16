package com.codeanalyzer.database;

import com.codeanalyzer.util.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that manages a single MySQL connection.
 * Expects the configured MySQL database to already exist.
 * Call DatabaseConnection.getInstance().getConnection() anywhere in the app.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() {
        connect();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
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
