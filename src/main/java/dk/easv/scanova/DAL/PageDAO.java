package dk.easv.scanova.DAL;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.ScannedFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PageDAO {

    // Called by ScanManager.saveFileToDB for every scanned file
    public void insertPageWithDocumentId(ScannedFile file,
                                         int realDocumentId) throws Exception {
        String sql = "INSERT INTO pages " +
                "(document_id, page_number, order_id, rotation, " +
                "scannedBy, scannedAt) " +
                "VALUES (?, ?, ?, ?, ?, GETDATE())";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, realDocumentId);
            ps.setInt(2, file.getReferenceId()); // API reference id
            ps.setInt(3, file.getFileId());      // in-session order
            ps.setInt(4, file.getRotation());
            ps.setInt(5, getCurrentUserId());
            ps.executeUpdate();
        }
    }

    // Called by FileManager.updatePageOrder after reorder
    public void updatePageOrder(int pageNumber, int documentId,
                                int newOrder) throws Exception {
        String sql = "UPDATE pages SET order_id = ? " +
                "WHERE page_number = ? AND document_id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newOrder);
            ps.setInt(2, pageNumber);
            ps.setInt(3, documentId);
            ps.executeUpdate();
            System.out.println("Updated page order — ref: " + pageNumber
                    + " doc: " + documentId + " order: " + newOrder);
        }
    }

    // Called by ScanHistoryManager — loads pages ordered by order_id
    public List<int[]> getPagesByDocumentId(int documentId) throws Exception {
        String sql = "SELECT page_number, rotation FROM pages " +
                "WHERE document_id = ? " +
                "ORDER BY COALESCE(order_id, page_number)";

        List<int[]> pages = new ArrayList<>();
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                pages.add(new int[]{
                        rs.getInt("page_number"), // [0] API reference id
                        rs.getInt("rotation")     // [1] rotation
                });
            }
        }
        return pages;
    }

    private int getCurrentUserId() {
        try {
            return SessionManager.getInstance().getCurrentUser().getId();
        } catch (Exception e) {
            return -1;
        }
    }
}