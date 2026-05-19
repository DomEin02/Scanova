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

    // Filter by multiple types and optional date range
    public List<String[]> getFilteredLogs(List<String> types,
                                          String fromDate,
                                          String toDate) throws Exception {
        StringBuilder sql = new StringBuilder(
                "SELECT l.action, u.username, l.details, l.timestamp " +
                        "FROM logs l LEFT JOIN users u ON l.userId = u.id WHERE 1=1 ");

        if (types != null && !types.isEmpty()) {
            sql.append("AND l.action IN (");
            for (int i = 0; i < types.size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
            }
            sql.append(") ");
        }

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append("AND CAST(l.timestamp AS DATE) >= ? ");
        }
        if (toDate != null && !toDate.isBlank()) {
            sql.append("AND CAST(l.timestamp AS DATE) <= ? ");
        }

        sql.append("ORDER BY l.timestamp DESC");

        List<String[]> logs = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;

            if (types != null) {
                for (String type : types) {
                    ps.setString(paramIndex++, type);
                }
            }
            if (fromDate != null && !fromDate.isBlank()) {
                ps.setString(paramIndex++, fromDate);
            }
            if (toDate != null && !toDate.isBlank()) {
                ps.setString(paramIndex++, toDate);
            }

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
