package com.hotelmanagement.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:hotel_management.db";

    private DatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(getDatabaseUrl());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public void closeConnection() {
        // Connections are opened per operation, so there is nothing persistent to close.
    }

    public static String getDatabaseUrl() {
        String configuredUrl = System.getProperty("hotel.db.url");
        return (configuredUrl == null || configuredUrl.isBlank()) ? DEFAULT_DB_URL : configuredUrl;
    }
}
