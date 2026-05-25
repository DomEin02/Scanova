package dk.easv.scanova.DAL;

import dk.easv.scanova.Model.Client;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public List<Client> getAllClients() throws Exception {
        String sql = "SELECT id, name FROM clients " +
                "WHERE is_active = 1 ORDER BY name";
        List<Client> clients = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt("id"),
                        rs.getString("name")));
            }
        }
        return clients;
    }

    public List<Client> getAllClientsIncludingInactive() throws Exception {
        String sql = "SELECT id, name, is_active FROM clients ORDER BY name";
        List<Client> clients = new ArrayList<>();

        try (Connection conn = DBConnector.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Client c = new Client(
                        rs.getInt("id"),
                        rs.getString("name"));
                c.setActive(rs.getBoolean("is_active"));
                clients.add(c);
            }
        }
        return clients;
    }

    public void createClient(String name, int userId) throws Exception {
        String sql = "INSERT INTO clients (name) VALUES (?)";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
        log("CLIENT_CREATED", userId, "Client created: " + name);
    }

    public void updateClient(int id, String name, int userId) throws Exception {
        String sql = "UPDATE clients SET name = ? WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
        log("CLIENT_UPDATED", userId, "Client updated: " + name);
    }

    public void deleteClient(int id, int userId, String name) throws Exception {
        // Check for linked profiles before deactivating
        String check = "SELECT COUNT(*) FROM profiles WHERE clientId = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0)
                throw new Exception(
                        "Cannot deactivate client — delete their profiles first.");
        }

        String sql = "UPDATE clients SET is_active = 0 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        log("CLIENT_DEACTIVATED", userId, "Client deactivated: " + name);
    }

    public void reactivateClient(int id, int userId, String name) throws Exception {
        String sql = "UPDATE clients SET is_active = 1 WHERE id = ?";
        try (Connection conn = DBConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        log("CLIENT_REACTIVATED", userId, "Client reactivated: " + name);
    }

    private void log(String action, int userId, String details) {
        try {
            new LogDAO().insertLog(action, userId, details);
        } catch (Exception e) {
            System.out.println("Could not write log: " + e.getMessage());
        }
    }
}