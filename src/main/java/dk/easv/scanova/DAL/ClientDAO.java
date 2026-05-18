package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.Client;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public List<Client> getAllClients() throws Exception {
        // Only return active clients
        String sql = "SELECT id, name FROM clients WHERE is_active = 1 ORDER BY name";
        List<Client> clients = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt("id"),
                        rs.getString("name")
                ));
            }
        }
        return clients;
    }

    public void createClient(String name, int createdByUserId) throws Exception {
        String sql = "INSERT INTO clients (name) VALUES (?)";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.executeUpdate();
            System.out.println("Client created: " + name);
        }

        // Log the action
        log("CLIENT_CREATED", createdByUserId, "Client created: " + name);
    }

    public void updateClient(int id, String name, int updatedByUserId) throws Exception {
        String sql = "UPDATE clients SET name = ? WHERE id = ?";

        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        // Log the action
        log("CLIENT_UPDATED", updatedByUserId, "Client updated: " + name);
    }

    public void deleteClient(int id, int deletedByUserId, String clientName) throws Exception {
        // Check if client has active profiles linked to it
        String check = "SELECT COUNT(*) FROM profiles WHERE clientId = ? AND is_active = 1";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new Exception(
                        "Cannot deactivate client — deactivate their profiles first.");
            }
        }

        // Soft delete
        String sql = "UPDATE clients SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Client deactivated: " + clientName);
        }

        // Log the action
        log("CLIENT_DEACTIVATED", deletedByUserId, "Client deactivated: " + clientName);
    }

    // ── Internal log helper ───────────────────────────────────────────────────
    private void log(String action, int userId, String details) {
        try {
            LogDAO logDAO = new LogDAO();
            logDAO.insertLog(action, userId, details);
        } catch (Exception e) {
            System.out.println("Could not write log: " + e.getMessage());
        }
    }
}