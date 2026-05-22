package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.ScannedFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PageDAO {

    public void insertPage(ScannedFile file) throws Exception {
        String sql = "INSERT INTO pages (document_id, page_number, rotation) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, 1); // hardcoded until document chain built
            ps.setInt(2, file.getReferenceId());
            ps.setInt(3, file.getRotation());
            ps.executeUpdate();

            System.out.println("Saved to pages — referenceId: " + file.getReferenceId());
        }
    }

    public List<ScannedFile> getPagesByDocumentId(int documentId) throws Exception {
        String sql = "SELECT id, document_id, page_number, rotation " +
                "FROM pages WHERE document_id = ? ORDER BY page_number";
        List<ScannedFile> pages = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();

            int fileId = 0;
            while (rs.next()) {
                fileId++;
                ScannedFile file = new ScannedFile(
                        fileId,
                        rs.getInt("page_number"),
                        null,
                        rs.getInt("document_id")
                );
                file.setRotation(rs.getInt("rotation"));
                pages.add(file);
            }
        }
        return pages;
    }

    // Only called to clean up test data — not called during normal scanning
    public void clearPages() throws Exception {
        String sql = "DELETE FROM pages WHERE document_id = 1";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
            System.out.println("Cleared pages table");
        }
    }
}