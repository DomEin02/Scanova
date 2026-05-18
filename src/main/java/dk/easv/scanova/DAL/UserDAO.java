package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() throws Exception {
        List<User> users = new ArrayList<>();
        // Only return active users
        String sql = "SELECT id, username, passwordHash, role FROM users WHERE is_active = 1";

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("role")
                ));
            }
        }
        return users;
    }

    // Get ALL users including inactive — for admin view with status column
    public List<User> getAllUsersIncludingInactive() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, passwordHash, role, is_active FROM users";

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("role")
                );
                user.setActive(rs.getBoolean("is_active"));
                users.add(user);
            }
        }
        return users;
    }

    public User getUserByUsername(String username) throws Exception {
        // Only active users can log in
        String sql = "SELECT id, username, passwordHash, role FROM users " +
                "WHERE username = ? AND is_active = 1";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("role")
                );
            }
        }
        return null;
    }

    public void createUser(User user) throws Exception {
        String sql = "INSERT INTO users (username, passwordHash, role) VALUES (?, ?, ?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
            System.out.println("User created: " + user.getUsername());
        }
    }

    public void updateUser(User user) throws Exception {
        String sql = "UPDATE users SET username = ?, passwordHash = ?, role = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.setInt(4, user.getId());
            ps.executeUpdate();
        }
    }

    public void deleteUser(int userId) throws Exception {
        // Remove profile assignments first
        String deleteProfiles = "DELETE FROM user_profiles WHERE userId = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteProfiles)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }

        // Soft delete
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void reactivateUser(int userId) throws Exception {
        String sql = "UPDATE users SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}