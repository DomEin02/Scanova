package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.ArchiveDAO;
import dk.easv.scanova.Model.Archive;
import java.util.List;

public class ArchiveManager {

    private final ArchiveDAO archiveDAO = new ArchiveDAO();

    public List<Archive> getAllArchives() throws Exception {
        return archiveDAO.getAllArchives();
    }

    public List<Archive> getAllArchivesIncludingInactive() throws Exception {
        return archiveDAO.getAllArchivesIncludingInactive();
    }

    public List<Archive> getArchivesByClientId(int clientId) throws Exception {
        return archiveDAO.getArchivesByClientId(clientId);
    }

    public void createArchive(String name, int clientId,
                              int userId) throws Exception {
        validateName(name);
        if (clientId <= 0)
            throw new Exception("Please select a client.");
        archiveDAO.createArchive(name, clientId, userId);
    }

    public void updateArchive(int id, String name,
                              int userId) throws Exception {
        validateName(name);
        archiveDAO.updateArchive(id, name, userId);
    }

    public void deleteArchive(int id, int userId,
                              String name) throws Exception {
        archiveDAO.deleteArchive(id, userId, name);
    }

    public void reactivateArchive(int id, int userId,
                                  String name) throws Exception {
        archiveDAO.reactivateArchive(id, userId, name);
    }

    private void validateName(String name) throws Exception {
        if (name == null || name.isBlank())
            throw new Exception("Archive name cannot be empty.");
        if (name.length() > 200)
            throw new Exception("Archive name cannot exceed 200 characters.");
    }
}