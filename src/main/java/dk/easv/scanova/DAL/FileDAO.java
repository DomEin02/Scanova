package dk.easv.scanova.DAL;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.ScannedFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {

    // Save a scanned file to the files table
    public int insertFile(ScannedFile file,
                          int documentId,
                          boolean barcodeDetected) throws Exception {
        String sql = "INSERT INTO files " +
                "(document_id, file_order_id, file_reference_id, " +
                "file_path, rotation, barcode_detected, scanned_by, " +
                "scanned_at, barcode_detected_at) " +
                "OUTPUT INSERTED.id " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, GETDATE(), ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
            ps.setInt(2, file.getFileId());      // file_order_id
            ps.setInt(3, file.getReferenceId()); // file_reference_id
            ps.setString(4, "");                 // file_path empty until export
            ps.setInt(5, file.getRotation());
            ps.setBoolean(6, barcodeDetected);
            ps.setInt(7, getCurrentUserId());

            if (barcodeDetected) {
                ps.setTimestamp(8,
                        new Timestamp(System.currentTimeMillis()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Saved file — id: " + id
                        + " order: " + file.getFileId()
                        + " ref: " + file.getReferenceId()
                        + " barcode: " + barcodeDetected);
                return id;
            }
        }
        throw new Exception(
                "Could not insert file for document: " + documentId);
    }

    // Mark barcode detected — updates barcode_detected_at timestamp
    public void markBarcodeDetected(int fileId) throws Exception {
        String sql = "UPDATE files " +
                "SET barcode_detected = 1, barcode_detected_at = GETDATE() " +
                "WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.executeUpdate();
        }
    }

    // Update file order after reorder
    public void updateFileOrder(int dbFileId, int newOrder) throws Exception {
        String sql = "UPDATE files SET file_order_id = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newOrder);
            ps.setInt(2, dbFileId);
            ps.executeUpdate();
            System.out.println("Updated file order — id: " + dbFileId
                    + " new order: " + newOrder);
        }
    }

    // Update file path after export
    public void updateFilePath(int fileId, String filePath) throws Exception {
        String sql = "UPDATE files SET file_path = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, filePath);
            ps.setInt(2, fileId);
            ps.executeUpdate();
        }
    }

    // Update rotation
    public void updateRotation(int fileId, int rotation) throws Exception {
        String sql = "UPDATE files SET rotation = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rotation);
            ps.setInt(2, fileId);
            ps.executeUpdate();
        }
    }

    // Get rotation
    public int getRotation(int fileId) throws Exception {
        String sql = "SELECT rotation FROM files WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("rotation");
        }
        return 0;
    }

    // Get all file ids for a document
    public List<Integer> getFileIdsByDocumentId(int documentId) throws Exception {
        String sql = "SELECT id FROM files WHERE document_id = ? " +
                "ORDER BY file_order_id";
        List<Integer> ids = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("id"));
        }
        return ids;
    }

    // Get files with full details for a document
    public List<int[]> getFilesByDocumentId(int documentId) throws Exception {
        String sql = "SELECT id, file_reference_id, file_order_id, rotation " +
                "FROM files " +
                "WHERE document_id = ? " +
                "ORDER BY file_order_id";

        List<int[]> files = new ArrayList<>();
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                files.add(new int[]{
                        rs.getInt("id"),               // [0] db file id
                        rs.getInt("file_reference_id"), // [1] api reference id
                        rs.getInt("file_order_id"),    // [2] order
                        rs.getInt("rotation")          // [3] rotation
                });
            }
        }
        return files;
    }

    private int getCurrentUserId() {
        try {
            return SessionManager.getInstance().getCurrentUser().getId();
        } catch (Exception e) {
            return -1;
        }
    }
}