package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {

    public void insertLog(String action, int userId, String details) throws Exception {
        String sql = "INSERT INTO logs (action, userId, details, timestamp) " +
                "VALUES (?, ?, ?, GETDATE())";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setInt(2, userId);
            ps.setString(3, details);
            ps.executeUpdate();
        }
    }

    public List<String[]> getAllLogs() throws Exception {
        String sql = "SELECT l.action, u.username, l.details, l.timestamp " +
                "FROM logs l LEFT JOIN users u ON l.userId = u.id " +
                "ORDER BY l.timestamp DESC";
        List<String[]> logs = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(new String[]{
                        rs.getString("action"),
                        rs.getString("username"),
                        rs.getString("details"),
                        rs.getString("timestamp")
                });
            }
        }
        return logs;
    }

    public List<String[]> getLogsByType(String action) throws Exception {
        String sql = "SELECT l.action, u.username, l.details, l.timestamp " +
                "FROM logs l LEFT JOIN users u ON l.userId = u.id " +
                "WHERE l.action = ? ORDER BY l.timestamp DESC";
        List<String[]> logs = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(new String[]{
                        rs.getString("action"),
                        rs.getString("username"),
                        rs.getString("details"),
                        rs.getString("timestamp")
                });
            }
        }
        return logs;
    }
}
