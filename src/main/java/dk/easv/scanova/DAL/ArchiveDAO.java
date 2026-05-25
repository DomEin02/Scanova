package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.Archive;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ArchiveDAO {

    public List<Archive> getAllArchives() throws Exception {
        String sql = "SELECT a.id, a.client_id, a.name, c.name as clientName " +
                "FROM archives a " +
                "LEFT JOIN clients c ON a.client_id = c.id " +
                "WHERE a.is_active = 1 ORDER BY a.name";
        List<Archive> archives = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Archive a = new Archive(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getString("name")
                );
                a.setClientName(rs.getString("clientName"));
                archives.add(a);
            }
        }
        return archives;
    }

    public List<Archive> getArchivesByClientId(int clientId) throws Exception {
        String sql = "SELECT a.id, a.client_id, a.name, c.name as clientName " +
                "FROM archives a " +
                "LEFT JOIN clients c ON a.client_id = c.id " +
                "WHERE a.client_id = ? AND a.is_active = 1 ORDER BY a.name";
        List<Archive> archives = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Archive a = new Archive(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getString("name")
                );
                a.setClientName(rs.getString("clientName"));
                archives.add(a);
            }
        }
        return archives;
    }

    public List<Archive> getAllArchivesIncludingInactive() throws Exception {
        String sql = "SELECT a.id, a.client_id, a.name, a.is_active, " +
                "c.name as clientName " +
                "FROM archives a " +
                "LEFT JOIN clients c ON a.client_id = c.id " +
                "ORDER BY a.name";
        List<Archive> archives = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Archive a = new Archive(
                        rs.getInt("id"),
                        rs.getInt("client_id"),
                        rs.getString("name")
                );
                a.setActive(rs.getBoolean("is_active"));
                a.setClientName(rs.getString("clientName"));
                archives.add(a);
            }
        }
        return archives;
    }

    public void createArchive(String name, int clientId,
                              int userId) throws Exception {
        String sql = "INSERT INTO archives (client_id, name) VALUES (?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            ps.setString(2, name);
            ps.executeUpdate();
            System.out.println("Archive created: " + name);
        }
        log("ARCHIVE_CREATED", userId, "Archive created: " + name);
    }

    public void updateArchive(int id, String name,
                              int userId) throws Exception {
        String sql = "UPDATE archives SET name = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
        log("ARCHIVE_UPDATED", userId, "Archive updated: " + name);
    }

    public void deleteArchive(int id, int userId, String name) throws Exception {
        String check = "SELECT COUNT(*) FROM boxes " +
                "WHERE archive_id = ? AND is_active = 1";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new Exception(
                        "Cannot deactivate archive — deactivate its boxes first.");
            }
        }

        String sql = "UPDATE archives SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        log("ARCHIVE_DEACTIVATED", userId, "Archive deactivated: " + name);
    }

    public void reactivateArchive(int id, int userId,
                                  String name) throws Exception {
        String sql = "UPDATE archives SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        log("ARCHIVE_REACTIVATED", userId, "Archive reactivated: " + name);
    }

    private void log(String action, int userId, String details) {
        try {
            new LogDAO().insertLog(action, userId, details);
        } catch (Exception e) {
            System.out.println("Could not write log: " + e.getMessage());
        }
    }
}