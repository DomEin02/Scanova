package dk.easv.scanova.DAL;

import dk.easv.scanova.BE.ScanCase;

import java.sql.*;

public class ScanCaseDAO {

    public ScanCase createCase(int boxId) {

        String sql = "INSERT INTO cases (box_id, title) VALUES (?, ?)";

        String title = "Case for box " + boxId;

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, boxId);
            ps.setString(2, title);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                int generatedId = rs.getInt(1);

                return new ScanCase(
                        generatedId,
                        boxId,
                        title
                );
            }

            throw new RuntimeException("Could not retrieve generated case id");

        } catch (Exception e) {
            throw new RuntimeException("Error creating case", e);
        }
    }
}
