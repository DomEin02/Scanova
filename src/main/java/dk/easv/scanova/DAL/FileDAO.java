package dk.easv.scanova.DAL;

import java.sql.*;

public class FileDAO {

    // UPDATE rotation
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

    // GET rotation
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
}