package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.ClientDAO;
import dk.easv.scanova.BE.Client;
import java.util.List;

public class ClientManager {

    private final ClientDAO clientDAO = new ClientDAO();

    public List<Client> getAllClients() throws Exception {
        return clientDAO.getAllClients();
    }

    public List<Client> getAllClientsIncludingInactive() throws Exception {
        return clientDAO.getAllClientsIncludingInactive();
    }

    public void createClient(String name, int userId) throws Exception {
        validateName(name);
        clientDAO.createClient(name, userId);
    }

    public void updateClient(int id, String name, int userId) throws Exception {
        validateName(name);
        clientDAO.updateClient(id, name, userId);
    }

    public void deleteClient(int id, int userId, String name) throws Exception {
        clientDAO.deleteClient(id, userId, name);
    }

    public void reactivateClient(int id, int userId, String name) throws Exception {
        clientDAO.reactivateClient(id, userId, name);
    }

    private void validateName(String name) throws Exception {
        if (name == null || name.isBlank())
            throw new Exception("Client name cannot be empty.");
        if (name.length() > 100)
            throw new Exception("Client name cannot exceed 100 characters.");
    }
}
