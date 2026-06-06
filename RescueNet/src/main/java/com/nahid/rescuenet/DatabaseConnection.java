package com.nahid.rescuenet;
import java.sql.*;
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/rescuenet_db";
    private static final String USER = "root";
    private static final String PASSWORD = "nahid777";
    private static DatabaseConnection instance;
    private Connection connection;
    private DatabaseConnection() {
        try { connection = DriverManager.getConnection(URL, USER, PASSWORD); }
        catch (SQLException e) { e.printStackTrace(); }
    }
    public static DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }
    public Connection getConnection() { return connection; }
}