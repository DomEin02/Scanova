package dk.easv.scanova.DAL;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnector {

    private static final String PROP_FILE = "config/config.settings";
    private static SQLServerDataSource dataSource;

    static {
        try {
            Properties databaseProperties = new Properties();
            databaseProperties.load(new FileInputStream(new File(PROP_FILE)));

            dataSource = new SQLServerDataSource();
            dataSource.setServerName(databaseProperties.getProperty("Server"));
            dataSource.setDatabaseName(databaseProperties.getProperty("Database"));
            dataSource.setUser(databaseProperties.getProperty("User"));
            dataSource.setPassword(databaseProperties.getProperty("Password"));
            dataSource.setPortNumber(1433);
            dataSource.setTrustServerCertificate(true);

            System.out.println("DBConnector initialized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize DBConnector", e);
        }
    }

    // Returnerer altid en connection
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Test main
    public static void main(String[] args) throws Exception {
        try (Connection connection = DBConnector.getConnection()) {
            System.out.println("Is it open? " + !connection.isClosed());
        }
    }
}