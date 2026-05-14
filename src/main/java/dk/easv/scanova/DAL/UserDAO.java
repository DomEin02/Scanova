package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, passwordHash, role FROM users";

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

    public User getUserByUsername(String username) throws Exception {
        String sql = "SELECT id, username, passwordHash, role " +
                "FROM users WHERE username = ?";

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
        String sql = "INSERT INTO users (username, passwordHash, role) " +
                "VALUES (?, ?, ?)";

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
        String sql = "UPDATE users SET username = ?, passwordHash = ?, " +
                "role = ? WHERE id = ?";

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
        // First remove profile assignments — otherwise FK constraint blocks delete
        String deleteProfiles = "DELETE FROM user_profiles WHERE userId = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteProfiles)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }

        // Then delete the user
        String deleteUser = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteUser)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}