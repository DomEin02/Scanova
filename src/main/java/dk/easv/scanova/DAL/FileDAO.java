package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {

    // INSERT file and return generated id
    public int createFileAndGetId(int caseId, String filePath) {
        String sql = "INSERT INTO files (case_id, file_path) OUTPUT INSERTED.id VALUES (?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, caseId);
            stmt.setString(2, filePath);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Simple insert without returning id
    // case_id = 1 hardcoded until full chain is built
    public void insertFile(String filePath) throws Exception {
        String sql = "INSERT INTO files (case_id, file_path) VALUES (?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, 1);
            ps.setString(2, filePath);
            ps.executeUpdate();

            System.out.println("Saved file path to DB: " + filePath);
        }
    }

    // GET all file paths for a case
    public List<String> getFilesByCaseId(int caseId) throws Exception {
        String sql = "SELECT file_path FROM files WHERE case_id = ?";
        List<String> paths = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, caseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                paths.add(rs.getString("file_path"));
            }
        }
        return paths;
    }

    // UPDATE rotation for a file
    public void updateRotation(int fileId, int rotation) {
        String sql = "UPDATE files SET rotation = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, rotation);
            stmt.setInt(2, fileId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // GET rotation for a file
    public int getRotation(int fileId) {
        String sql = "SELECT rotation FROM files WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rotation");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}