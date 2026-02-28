package com.mycompany.javafxapplication1;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class SessionManager {
    private final String fileName = "jdbc:sqlite:comp20081.db";
    private String username;

    public void ensureSessionTable() {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(fileName);
                 Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS Sessions("
                        + "token TEXT PRIMARY KEY, username TEXT, created_at TEXT, expires_at TEXT)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String createSession(String username, int minutesValid) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plusMinutes(minutesValid);
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(fileName);
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO Sessions(token, username, created_at, expires_at) VALUES(?,?,?,?)")) {
                ps.setString(1, token);
                ps.setString(2, username);
                ps.setString(3, now.toString());
                ps.setString(4, exp.toString());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    public boolean validateSession(String token) {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(fileName);
                 PreparedStatement ps = conn.prepareStatement("SELECT expires_at FROM Sessions WHERE token=?")) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LocalDateTime exp = LocalDateTime.parse(rs.getString("expires_at"));
                        return LocalDateTime.now().isBefore(exp);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void destroySession(String token) {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection(fileName);
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM Sessions WHERE token=?")) {
                ps.setString(1, token);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void setUsername(String username){
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }
}
