package dk.easv.scanova.DAL;

import dk.easv.scanova.BE.Document;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DocumentDAO {

    public Document createDocument(int boxId) {

        String sql = "INSERT INTO documents (box_id, title) VALUES (?, ?)";

        String title = "Document for box " + boxId;

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, boxId);
            ps.setString(2, title);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                int generatedId = rs.getInt(1);

                return new Document(generatedId, boxId);
            }

            throw new RuntimeException("Could not retrieve generated document id");

        } catch (Exception e) {
            throw new RuntimeException("Error creating document", e);
        }
    }
}
