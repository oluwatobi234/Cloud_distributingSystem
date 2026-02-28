/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;
import java.sql.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author ntu-user
 */
public class MySQLSync {
    
    private static final String MYSQL_HOST = 
        System.getenv().getOrDefault("MYSQL_URL", "localhost:3306");
    private static final String MYSQL_USER = 
        System.getenv().getOrDefault("MYSQL_USER", "root");
    private static final String MYSQL_PASS = 
        System.getenv().getOrDefault("MYSQL_PASSWORD", "rootpass");
    
    private static final String MYSQL_URL = "jdbc:mysql://" + MYSQL_HOST + "/comp20081";
    
    private Connection mysqlConn;
    
    public MySQLSync() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            mysqlConn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
            System.out.println("MySQL connection established");
        } catch (Exception e) {
            System.err.println("MySQL connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Sync user from SQLite to MySQL
     */
    public void syncUserToMySQL(String username, String password, String role) {
        try {
            // Check if user exists in MySQL
            PreparedStatement checkStmt = mysqlConn.prepareStatement(
                "SELECT id FROM users WHERE username = ?"
            );
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Update existing user
                PreparedStatement updateStmt = mysqlConn.prepareStatement(
                    "UPDATE users SET password = ?, role = ? WHERE username = ?"
                );
                updateStmt.setString(1, password);
                updateStmt.setString(2, role);
                updateStmt.setString(3, username);
                updateStmt.executeUpdate();
                System.out.println("Updated user in MySQL: " + username);
            } else {
                // Insert new user
                PreparedStatement insertStmt = mysqlConn.prepareStatement(
                    "INSERT INTO users (username, password, role) VALUES (?, ?, ?)"
                );
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.setString(3, role);
                insertStmt.executeUpdate();
                System.out.println("Inserted user to MySQL: " + username);
            }
        } catch (SQLException e) {
            System.err.println("Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Sync all users from SQLite to MySQL
     */
    public void syncAllUsers() {
        try {
            DB sqlite = new DB();
            List<User> users = sqlite.getDataFromTable();
            
            for (User user : users) {
                syncUserToMySQL(user.getUser(), user.getPass(), user.getRole());
            }
            System.out.println("Synced " + users.size() + " users to MySQL");
        } catch (Exception e) {
            System.err.println("Bulk sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Sync session from SQLite to MySQL
     */
    public void syncSession(String token, String username, Timestamp created, Timestamp expires) {
        try {
            PreparedStatement stmt = mysqlConn.prepareStatement(
                "INSERT INTO sessions (token, username, created_at, expires_at) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE expires_at = ?"
            );
            stmt.setString(1, token);
            stmt.setString(2, username);
            stmt.setTimestamp(3, created);
            stmt.setTimestamp(4, expires);
            stmt.setTimestamp(5, expires);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Session sync failed: " + e.getMessage());
        }
    }
    
    public void close() {
        try {
            if (mysqlConn != null) mysqlConn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
