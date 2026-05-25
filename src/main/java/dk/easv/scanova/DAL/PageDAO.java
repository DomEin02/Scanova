package dk.easv.scanova.DAL;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.ScannedFile;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PageDAO {

    public void insertPageWithDocumentId(ScannedFile file,
                                         int realDocumentId) throws Exception {
        String sql = "INSERT INTO pages " +
                "(document_id, page_number, order_id, rotation, " +
                "scannedBy, scannedAt) " +
                "VALUES (?, ?, ?, ?, ?, GETDATE())";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, realDocumentId);
            ps.setInt(2, file.getReferenceId()); // original API reference
            ps.setInt(3, file.getFileId());      // order within session
            ps.setInt(4, file.getRotation());
            ps.setInt(5, getCurrentUserId());
            ps.executeUpdate();
            System.out.println("Saved page — doc: " + realDocumentId
                    + " ref: " + file.getReferenceId()
                    + " order: " + file.getFileId());
        }
    }

    public void insertPage(ScannedFile file) throws Exception {
        String sql = "INSERT INTO pages " +
                "(document_id, page_number, order_id, rotation, " +
                "scannedBy, scannedAt) " +
                "VALUES (?, ?, ?, ?, ?, GETDATE())";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, 1);
            ps.setInt(2, file.getReferenceId());
            ps.setInt(3, file.getFileId());
            ps.setInt(4, file.getRotation());
            ps.setInt(5, getCurrentUserId());
            ps.executeUpdate();
        }
    }

    // ── Update order after reorder ────────────────────────────────────────────
    // Called by FileManager.updateFileOrder via ScanViewController.saveOrderToDB
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
                    + " doc: " + documentId
                    + " new order: " + newOrder);
        }
    }

    // ── Get pages ordered by order_id ─────────────────────────────────────────
    public List<int[]> getPagesByDocumentId(int documentId) throws Exception {
        String sql = "SELECT page_number, rotation, order_id FROM pages " +
                "WHERE document_id = ? " +
                "ORDER BY COALESCE(order_id, page_number)"; // use order_id, fall back to page_number

        List<int[]> pages = new ArrayList<>();
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                pages.add(new int[]{
                        rs.getInt("page_number"), // [0] API reference id
                        rs.getInt("rotation"),    // [1] rotation
                        rs.getInt("order_id")     // [2] order
                });
            }
        }
        return pages;
    }

    public void clearPages() throws Exception {
        String sql = "DELETE FROM pages WHERE document_id = 1";
        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private int getCurrentUserId() {
        try {
            return SessionManager.getInstance().getCurrentUser().getId();
        } catch (Exception e) {
            return -1;
        }
    }
}