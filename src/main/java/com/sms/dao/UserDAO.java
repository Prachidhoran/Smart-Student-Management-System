package com.sms.dao;

import com.sms.model.User;
import com.sms.util.DatabaseConnection;
import com.sms.util.PasswordUtil;
import com.sms.util.AppLogger;

public class UserDAO {
    public User authenticate(String username, String password) {
        String sql = """
            SELECT id, username, password, role, student_db_id
            FROM users
            WHERE username = ?
            """;

        try (var c = DatabaseConnection.getConnection();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                char[] candidate = password == null ? null : password.toCharArray();
                try {
                    if (!PasswordUtil.matches(candidate, rs.getString("password"))) return null;
                } finally {
                    if (candidate != null) java.util.Arrays.fill(candidate, '\0');
                }
                Integer studentId = rs.getObject("student_db_id") == null
                        ? null : rs.getInt("student_db_id");
                return new User(rs.getInt("id"), rs.getString("username"),
                        rs.getString("role"), studentId);
            }
        } catch (Exception e) {
            AppLogger.error(UserDAO.class, "Authentication query failed", e);
            throw new RuntimeException("Could not sign in right now. Please try again.", e);
        }
    }
}
