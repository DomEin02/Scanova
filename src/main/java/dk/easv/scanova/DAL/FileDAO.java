package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {

     //Saves a file path reference to the files table.
     //Called when a file is exported — stores where the exported TIFF ended up.
     //case_id = 1 hardcoded until the full chain is built.
    public void insertFile(String filePath) throws Exception {
        String sql = "INSERT INTO files (case_id, file_path) VALUES (?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, 1); // hardcoded until case chain is built
            ps.setString(2, filePath);
            ps.executeUpdate();

            System.out.println("Saved file path to DB: " + filePath);
        }
    }

     //Fetches all files for a given case_id.
    public List<String> getFilesByCaseId(int caseId) throws Exception {
        String sql = "SELECT file_path FROM files WHERE case_id = ?";
        List<String> paths = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, caseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                paths.add(rs.getString("file_path"));
            }
        }
        return paths;
    }
}