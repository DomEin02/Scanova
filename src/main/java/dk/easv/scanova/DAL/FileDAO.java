package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {

    public void insertFile(int documentId,
                           int fileReferenceId,
                           int fileOrderId,
                           String filePath,
                           int rotation,
                           boolean barcodeDetected,

                           int scannedBy) throws Exception {

        String sql =
                "INSERT INTO files " +
                        "(document_id, file_reference_id, file_order_id, file_path, rotation, barcode_Detected, scanned_by) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
            ps.setInt(2, fileReferenceId);
            ps.setInt(3, fileOrderId);
            ps.setString(4, filePath);
            ps.setInt(5, rotation);
            ps.setBoolean(6, barcodeDetected);
            ps.setInt(7, scannedBy);

            ps.executeUpdate();
        }
    }

    public void markBarcodeDetected(int fileId) {

        String sql =
                "UPDATE files " +
                        "SET barcode_detected = 1, barcode_detected_at = GETDATE() " +
                        "WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fileId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // GET all file paths for a case
    public List<String> getFilesByDocumentId(int documentId) throws Exception {
        String sql = "SELECT file_path FROM files WHERE document_id = ?";
        List<String> paths = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
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