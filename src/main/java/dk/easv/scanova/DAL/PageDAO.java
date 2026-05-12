package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.ScannedFile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PageDAO {
     //Inserts a scanned page into the pages table.
     //We use a hardcoded document_id = 1 for now until the full
     //client → archive → box → case → document chain is built in Sprint 2.
    public void insertPage(ScannedFile file) throws Exception {
        String sql = "INSERT INTO pages (document_id, page_number, rotation) VALUES (?, ?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, 1); // hardcoded until document chain is built
            ps.setInt(2, file.getReferenceId());
            ps.setInt(3, file.getRotation());
            ps.executeUpdate();

            System.out.println("Saved to pages — referenceId: " + file.getReferenceId());
        }
    }

     //Fetches all pages for a given document_id and maps them to ScannedFile objects.
     //imageData will be null — we only store metadata, not image bytes.
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
                        null, // image data not stored in DB
                        rs.getInt("document_id")
                );
                file.setRotation(rs.getInt("rotation"));
                pages.add(file);
            }
        }
        return pages;
    }

    public void clearPages() throws Exception {
        String sql = "DELETE FROM pages WHERE document_id = 1";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
            System.out.println("Cleared pages table");
        }
    }
}