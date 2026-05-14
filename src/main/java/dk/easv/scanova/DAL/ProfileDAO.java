package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileDAO {

    public List<String> getAllProfileNames() throws Exception {
        String sql = "SELECT name FROM profiles";
        List<String> names = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    public int getProfileIdByName(String name) throws Exception {
        String sql = "SELECT id FROM profiles WHERE name = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void assignProfileToUser(int userId, int profileId) throws Exception {
        // Only insert if not already assigned
        String sql = "IF NOT EXISTS " +
                "(SELECT 1 FROM user_profiles WHERE userId = ? AND profileId = ?) " +
                "INSERT INTO user_profiles (userId, profileId) VALUES (?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, profileId);
            ps.setInt(3, userId);
            ps.setInt(4, profileId);
            ps.executeUpdate();

            System.out.println("Assigned profile " + profileId + " to user " + userId);
        }
    }

    public List<String> getProfilesForUser(int userId) throws Exception {
        String sql = "SELECT p.name FROM profiles p " +
                "JOIN user_profiles up ON p.id = up.profileId " +
                "WHERE up.userId = ?";
        List<String> names = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }
}