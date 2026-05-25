package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CaseDAO {

    public int createCase(int boxId, String title) throws Exception {
        String sql = "INSERT INTO cases (box_id, title, status) " +
                "OUTPUT INSERTED.id VALUES (?, ?, 'In Progress')";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, boxId);
            ps.setString(2, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Created case id: " + id);
                return id;
            }
        }
        throw new Exception("Could not create case for box: " + boxId);
    }

    // ── Get cases scanned by this user — queries through pages table ──────────
    public List<String[]> getCasesForUser(int userId) throws Exception {
        String sql = "SELECT DISTINCT c.id, c.title, b.label, c.status " +
                "FROM cases c " +
                "JOIN boxes b ON c.box_id = b.id " +
                "JOIN documents d ON d.case_id = c.id " +
                "JOIN pages p ON p.document_id = d.id " +
                "WHERE p.scannedBy = ? " +
                "ORDER BY c.id DESC";

        List<String[]> cases = new ArrayList<>();
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cases.add(new String[]{
                        String.valueOf(rs.getInt("id")),  // [0] case id
                        rs.getString("title"),             // [1] title
                        rs.getString("label"),             // [2] box label
                        rs.getString("status")             // [3] status
                });
            }
        }
        return cases;
    }
}