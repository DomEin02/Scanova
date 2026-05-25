package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.Box;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BoxDAO {

    public List<Box> getAllBoxes() throws Exception {
        String sql = "SELECT b.id, b.archive_id, b.label, b.profileId, " +
                "a.name as archiveName, p.name as profileName " +
                "FROM boxes b " +
                "LEFT JOIN archives a ON b.archive_id = a.id " +
                "LEFT JOIN profiles p ON b.profileId = p.id " +
                "WHERE b.is_active = 1 ORDER BY b.label";
        List<Box> boxes = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Box box = new Box(
                        rs.getInt("id"),
                        rs.getInt("archive_id"),
                        rs.getString("label"),
                        rs.getInt("profileId"),
                        rs.getString("profileName"),
                        0, 0
                );
                box.setArchiveName(rs.getString("archiveName"));
                boxes.add(box);
            }
        }
        return boxes;
    }

    public List<Box> getBoxesByArchiveId(int archiveId) throws Exception {
        String sql = "SELECT b.id, b.archive_id, b.label, b.profileId, " +
                "a.name as archiveName, p.name as profileName " +
                "FROM boxes b " +
                "LEFT JOIN archives a ON b.archive_id = a.id " +
                "LEFT JOIN profiles p ON b.profileId = p.id " +
                "WHERE b.archive_id = ? AND b.is_active = 1 " +
                "ORDER BY b.label";
        List<Box> boxes = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, archiveId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Box box = new Box(
                        rs.getInt("id"),
                        rs.getInt("archive_id"),
                        rs.getString("label"),
                        rs.getInt("profileId"),
                        rs.getString("profileName"),
                        0, 0
                );
                box.setArchiveName(rs.getString("archiveName"));
                boxes.add(box);
            }
        }
        return boxes;
    }

    public List<Box> getAllBoxesIncludingInactive() throws Exception {
        String sql = "SELECT b.id, b.archive_id, b.label, b.profileId, " +
                "b.is_active, a.name as archiveName, " +
                "p.name as profileName " +
                "FROM boxes b " +
                "LEFT JOIN archives a ON b.archive_id = a.id " +
                "LEFT JOIN profiles p ON b.profileId = p.id " +
                "ORDER BY b.label";
        List<Box> boxes = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Box box = new Box(
                        rs.getInt("id"),
                        rs.getInt("archive_id"),
                        rs.getString("label"),
                        rs.getInt("profileId"),
                        rs.getString("profileName"),
                        0, 0
                );
                box.setActive(rs.getBoolean("is_active"));
                box.setArchiveName(rs.getString("archiveName"));
                boxes.add(box);
            }
        }
        return boxes;
    }

    public Box getBoxByLabel(String label) throws Exception {
        String sql = "SELECT b.id, b.archive_id, b.label, b.profileId, " +
                "p.name as profileName, p.rotation, p.brightness " +
                "FROM boxes b " +
                "LEFT JOIN profiles p ON b.profileId = p.id " +
                "WHERE b.label = ? AND b.is_active = 1";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, label);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Box(
                        rs.getInt("id"),
                        rs.getInt("archive_id"),
                        rs.getString("label"),
                        rs.getInt("profileId"),
                        rs.getString("profileName"),
                        rs.getDouble("rotation"),
                        rs.getDouble("brightness")
                );
            }
        }
        throw new Exception("Box '" + label + "' not found. " +
                "Make sure the Box ID exists in the system.");
    }

    public void createBox(String label, int archiveId,
                          int profileId, int userId) throws Exception {
        String check = "SELECT COUNT(*) FROM boxes WHERE label = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, label);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0)
                throw new Exception("Box label '" + label + "' already exists.");
        }

        String sql = "INSERT INTO boxes (archive_id, label, profileId) " +
                "VALUES (?, ?, ?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, archiveId);
            ps.setString(2, label);
            ps.setInt(3, profileId);
            ps.executeUpdate();
            System.out.println("Box created: " + label);
        }
        log("BOX_CREATED", userId, "Box created: " + label);
    }

    public void updateBox(int id, String label, int archiveId,
                          int profileId, int userId) throws Exception {
        String sql = "UPDATE boxes SET label = ?, archive_id = ?, " +
                "profileId = ? WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, label);
            ps.setInt(2, archiveId);
            ps.setInt(3, profileId);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
        log("BOX_UPDATED", userId, "Box updated: " + label);
    }

    public void deleteBox(int id, int userId, String label) throws Exception {
        String sql = "UPDATE boxes SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        log("BOX_DEACTIVATED", userId, "Box deactivated: " + label);
    }

    public void reactivateBox(int id, int userId, String label) throws Exception {
        String sql = "UPDATE boxes SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        log("BOX_REACTIVATED", userId, "Box reactivated: " + label);
    }

    public boolean isCombinationAlreadyUsed(int boxId,
                                            int profileId) throws Exception {
        String sql = "SELECT COUNT(*) FROM cases c " +
                "JOIN boxes b ON b.id = c.box_id " +
                "WHERE c.box_id = ? AND b.profileId = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, boxId);
            ps.setInt(2, profileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        }
        return false;
    }

    private void log(String action, int userId, String details) {
        try {
            new LogDAO().insertLog(action, userId, details);
        } catch (Exception e) {
            System.out.println("Could not write log: " + e.getMessage());
        }
    }
}