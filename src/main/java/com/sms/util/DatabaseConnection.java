package com.sms.util;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        String configuredPath = System.getProperty("sms.db.path", "student_management.db");
        String url = "jdbc:sqlite:" + Path.of(configuredPath).toAbsolutePath();
        Connection c = DriverManager.getConnection(url);
        try (var s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }
}
