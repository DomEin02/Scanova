package dk.easv.scanova.DAL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {
    private static final String URL =
            "jdbc:sqlserver://localhost:1433;databaseName=elevah01_SQLInjectionDB;encrypt=true;trustServerCertificate=true";

    private static final String USER = "YOUR_USERNAME";
    private static final String PASSWORD = "YOUR_PASSWORD";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}