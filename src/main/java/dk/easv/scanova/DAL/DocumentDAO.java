package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {

    // Create document under a case — used by ScanManager
    public int createDocument(int caseId, String title) throws Exception {
        String sql = "INSERT INTO documents (case_id, title, status) " +
                "OUTPUT INSERTED.id VALUES (?, ?, 'In Progress')";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Created document id: " + id
                        + " title: " + title);
                return id;
            }
        }
        throw new Exception("Could not create document for case: " + caseId);
    }

    // Get documents for a case — used for history loading
    public List<String[]> getDocumentsByCaseId(int caseId) throws Exception {
        String sql = "SELECT id, title, status FROM documents " +
                "WHERE case_id = ? ORDER BY id";

        List<String[]> docs = new ArrayList<>();
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                docs.add(new String[]{
                        String.valueOf(rs.getInt("id")), // [0] document id
                        rs.getString("title"),            // [1] title
                        rs.getString("status")            // [2] status
                });
            }
        }
        return docs;
    }
}