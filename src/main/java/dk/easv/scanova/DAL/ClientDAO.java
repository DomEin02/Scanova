package dk.easv.scanova.DAL;

import dk.easv.scanova.BE.Client;
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
        // Check if client has profiles linked to it
        // Note: is_active not yet on profiles table — check all profiles
        String check = "SELECT COUNT(*) FROM profiles WHERE clientId = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                throw new Exception(
                        "Cannot deactivate client — delete their profiles first.");
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

        log("CLIENT_DEACTIVATED", deletedByUserId, "Client deactivated: " + clientName);
    }

    public void reactivateClient(int id, int reactivatedByUserId,
                                 String clientName) throws Exception {
        String sql = "UPDATE clients SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Client reactivated: " + clientName);
        }
        log("CLIENT_REACTIVATED", reactivatedByUserId,
                "Client reactivated: " + clientName);
    }

    // Get ALL clients including inactive
    public List<Client> getAllClientsIncludingInactive() throws Exception {
        String sql = "SELECT id, name, is_active FROM clients ORDER BY name";
        List<Client> clients = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Client c = new Client(
                        rs.getInt("id"),
                        rs.getString("name")
                );
                c.setActive(rs.getBoolean("is_active"));
                clients.add(c);
            }
        }
        return clients;
    }

    // Internal log helper
    private void log(String action, int userId, String details) {
        try {
            LogDAO logDAO = new LogDAO();
            logDAO.insertLog(action, userId, details);
        } catch (Exception e) {
            System.out.println("Could not write log: " + e.getMessage());
        }
    }
}