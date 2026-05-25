package dk.easv.scanova.DAL;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import dk.easv.scanova.Model.Profile;

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

    public List<Profile> getAllProfiles() throws Exception {
        String sql = "SELECT id, name, rotation, brightness, clientId, is_active " + "FROM profiles WHERE is_active = 1";
        List<Profile> profiles = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Profile p = new Profile(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getFloat("rotation"),
                        rs.getFloat("brightness"),
                        rs.getInt("clientId"),
                        rs.getBoolean("is_active")
                );
                profiles.add(p);
            }
        }
        return profiles;
    }

    public void createProfile(Profile profile) throws Exception {
        String sql = "INSERT INTO profiles (name, rotation, brightness, clientId) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, profile.getName());
            ps.setFloat(2, profile.getRotation());
            ps.setFloat(3, profile.getBrightness());
            ps.setInt(4, profile.getClientId());

            ps.executeUpdate();
        }
    }

    public void updateProfile(Profile profile) throws Exception {
        String sql = "UPDATE profiles SET name = ?, rotation = ?, " +
                "brightness = ?, clientId = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, profile.getName());
            ps.setFloat(2, profile.getRotation());
            ps.setFloat(3, profile.getBrightness());
            ps.setInt(4, profile.getClientId());
            ps.setInt(5, profile.getId());

            ps.executeUpdate();
        }
    }

    public void deleteProfile(int id) throws Exception {
        String sql = "UPDATE profiles SET is_active = 0 WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    public List<Profile> getAllProfilesIncludingInactive() throws Exception {
        String sql = "SELECT id, name, rotation, brightness, clientId, is_active FROM profiles";
        List<Profile> profiles = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                profiles.add(new Profile(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getFloat("rotation"),
                        rs.getFloat("brightness"),
                        rs.getInt("clientId"),
                        rs.getBoolean("is_active")
                ));
            }
        }
        return profiles;
    }

    public void reactivateProfile(int id) throws Exception {
        String sql = "UPDATE profiles SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}