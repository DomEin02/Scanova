package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.BoxDAO;
import dk.easv.scanova.Model.Box;

import java.util.List;

public class BoxManager {

    private final BoxDAO boxDAO = new BoxDAO();

    public List<Box> getAllBoxes() throws Exception {
        return boxDAO.getAllBoxes();
    }

    public List<Box> getAllBoxesIncludingInactive() throws Exception {
        return boxDAO.getAllBoxesIncludingInactive();
    }

    public List<Box> getBoxesByArchiveId(int archiveId) throws Exception {
        return boxDAO.getBoxesByArchiveId(archiveId);
    }

    public void createBox(String label, int archiveId,
                          int profileId, int userId) throws Exception {
        validateLabel(label);
        if (archiveId <= 0)
            throw new Exception("Please select an archive.");
        if (profileId <= 0)
            throw new Exception("Please select a profile.");
        boxDAO.createBox(label, archiveId, profileId, userId);
    }

    public void updateBox(int id, String label, int archiveId,
                          int profileId, int userId) throws Exception {
        validateLabel(label);
        boxDAO.updateBox(id, label, archiveId, profileId, userId);
    }

    public void deleteBox(int id, int userId, String label) throws Exception {
        boxDAO.deleteBox(id, userId, label);
    }

    public void reactivateBox(int id, int userId, String label) throws Exception {
        boxDAO.reactivateBox(id, userId, label);
    }

    private void validateLabel(String label) throws Exception {
        if (label == null || label.isBlank())
            throw new Exception("Box label cannot be empty.");
        if (label.length() > 100)
            throw new Exception("Box label cannot exceed 100 characters.");
    }
}