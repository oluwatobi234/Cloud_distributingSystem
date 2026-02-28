/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */
package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 *
 * @author ntu-user
 */
public class DB {
    private String fileName = "jdbc:sqlite:comp20081.db";
    private int timeout = 30;
    private String dataBaseName = "COMP20081";
    private String dataBaseTableName = "Users";
    Connection connection = null;
    private Random random = new SecureRandom();
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private int iterations = 10000;
    private int keylength = 256;
    private String saltValue;
    
    /**
     * @brief constructor - generates the salt if it doesn't exists or load it from the file .salt
     */
    DB() {
        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = this.getSaltvalue(30);
                FileWriter myWriter = new FileWriter(fp);
                myWriter.write(saltValue);
                myWriter.close();
            } else {
                Scanner myReader = new Scanner(fp);
                while (myReader.hasNextLine()) {
                    saltValue = myReader.nextLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    /**
     * @brief create a new table
     * @param tableName name of type String
     */
    public void createTable(String tableName) throws ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);
        
        // Create table with role column included
        statement.executeUpdate("create table if not exists " + tableName
                + "(id integer primary key autoincrement, "
                + "name string UNIQUE, "
                + "password string, "
                + "role string default 'standard')");
        
        System.out.println("Table created/verified with role column");

        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException cnfe) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, cnfe);
        } finally {
            closeConn();
        }

    }
    private void closeConn() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    public String getUserRole(String username) {
        String role = "standard";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT role FROM " + dataBaseTableName + " WHERE name='" + username + "'");
            if (rs.next()) {
                role = rs.getString("role");
                System.out.println("DEBUG DB: Found role '" + role + "' for user '" + username + "'");
            }else { 
            System.out.println("DEBUG DB: User '" + username + "' NOT FOUND in database");

        };
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConn();
        }
        return role;
    }

    public void promoteToAdmin(String username) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var stmt = connection.createStatement();
            stmt.executeUpdate("UPDATE " + dataBaseTableName + " SET role='admin' WHERE name='" + username + "'");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConn();
        }
    }
    // meathod for sharing a file with another user
    public void shareFile(String filename, String owner, String sharedWith, String permission) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            // Check if access_control table exists, create if not
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS access_control (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "filename TEXT, " +
                    "owner TEXT, " +
                    "shared_with TEXT, " +
                    "permission TEXT)");

            // Insert share record
            statement.executeUpdate("INSERT INTO access_control (filename, owner, shared_with, permission) " +
                    "VALUES ('" + filename + "', '" + owner + "', '" + sharedWith + "', '" + permission + "')");

            System.out.println("Shared " + filename + " with " + sharedWith);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConn();
        }
    }
    // this meathod is for the receving user 
    public ObservableList<FileRecord> getSharedFilesForUser(String username) {
    ObservableList<FileRecord> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT filename, owner FROM access_control WHERE shared_with='" + username + "'");
            while (rs.next()) {
                result.add(new FileRecord(rs.getString("filename"), rs.getString("owner")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConn();
        }
        return result;
    }
    // check permission for users 
    public boolean checkPermission(String filename, String username, String requiredPerm) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT permission FROM access_control " +
                    "WHERE filename='" + filename + "' AND shared_with='" + username + "'");
            if (rs.next()) {
                String perm = rs.getString("permission");
                return perm.equals("write") || perm.equals(requiredPerm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConn();
        }
        return false;
    }
    
    public void deleteUser(String username) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var stmt = connection.createStatement();
            stmt.executeUpdate("DELETE FROM " + dataBaseTableName + " WHERE name='" + username + "'");
            System.out.println("Deleted user: " + username);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConn();
        }
    }

/**
     * @brief delete table
     * @param tableName of type String
     */
    public void delTable(String tableName) throws ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("drop table if exists " + tableName);
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * @brief add data to the database method
     * @param user name of type String
     * @param password of type String
     */
        public void addDataToDB(String user, String password, String role) throws InvalidKeySpecException, ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
//            System.out.println("Adding User: " + user + ", Password: " + password);
            statement.executeUpdate("insert into " + dataBaseTableName + " (name, password, role) values('" + user + "','" + generateSecurePassword(password) + "','" + role + "')");
            System.out.println("DEBUG DB: Saved user '" + user + "' with role '" + role + "'");
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    // connection close failed.
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    /**
     * @brief get data from the Database method
     * @retunr results as ResultSet
     */
    public ObservableList<User> getDataFromTable() throws ClassNotFoundException {
    ObservableList<User> result = FXCollections.observableArrayList();
    try {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(fileName);
        var statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("select * from " + this.dataBaseTableName);
        while (rs.next()) {
            // Include role in constructor
            result.add(new User(rs.getString("name"), rs.getString("password"), rs.getString("role")));
        }
    } catch (SQLException ex) {
        Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        closeConn();
    }
    return result;
}

    /**
     * @brief decode password method
     * @param user name as type String
     * @param pass plain password of type String
     * @return true if the credentials are valid, otherwise false
     */
    public boolean validateUser(String user, String pass) throws InvalidKeySpecException, ClassNotFoundException {
        Boolean flag = false;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(fileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select name, password from " + this.dataBaseTableName);
            String inPass = generateSecurePassword(pass);
            // Let's iterate through the java ResultSet
            while (rs.next()) {
                if (user.equals(rs.getString("name")) && rs.getString("password").equals(inPass)) {
                    flag = true;
                    break;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

        return flag;
    }

    private String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            finalval.append(characters.charAt(random.nextInt(characters.length())));
        }

        return new String(finalval);
    }

    /* Method to generate the hash value */
    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public String generateSecurePassword(String password) throws InvalidKeySpecException {
        String finalval = null;

        byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());

        finalval = Base64.getEncoder().encodeToString(securePassword);

        return finalval;
    }

    /**
     * @brief get table name
     * @return table name as String
     */
    public String getTableName() {
        return this.dataBaseTableName;
    }

    /**
     * @brief print a message on screen method
     * @param message of type String
     */
    public void log(String message) {
        System.out.println(message);

    }

//    public static void main(String[] args) throws InvalidKeySpecException {
//        DB myObj = new DB();
//        myObj.log("-------- Simple Tutorial on how to make JDBC connection to SQLite DB ------------");
//        myObj.log("\n---------- Drop table ----------");
//        myObj.delTable(myObj.getTableName());
//        myObj.log("\n---------- Create table ----------");
//        myObj.createTable(myObj.getTableName());
//        myObj.log("\n---------- Adding Users ----------");
//        myObj.addDataToDB("ntu-user", "12z34");
//        myObj.addDataToDB("ntu-user2", "12yx4");
//        myObj.addDataToDB("ntu-user3", "a1234");
//        myObj.log("\n---------- get Data from the Table ----------");
//        myObj.getDataFromTable(myObj.getTableName());
//        myObj.log("\n---------- Validate users ----------");
//        String[] users = new String[]{"ntu-user", "ntu-user", "ntu-user1"};
//        String[] passwords = new String[]{"12z34", "1235", "1234"};
//        String[] messages = new String[]{"VALID user and password",
//            "VALID user and INVALID password", "INVALID user and VALID password"};
//
//        for (int i = 0; i < 3; i++) {
//            System.out.println("Testing " + messages[i]);
//            if (myObj.validateUser(users[i], passwords[i], myObj.getTableName())) {
//                myObj.log("++++++++++VALID credentials!++++++++++++");
//            } else {
//                myObj.log("----------INVALID credentials!----------");
//            }
//        }
//    }
}
