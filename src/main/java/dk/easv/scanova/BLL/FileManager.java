package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.FileDAO;

public class FileManager {

    private final FileDAO fileDAO = new FileDAO();

    public void updateRotation(int fileId, int rotation) {
        fileDAO.updateRotation(fileId, rotation);
    }

    public int getRotation(int fileId) {
        return fileDAO.getRotation(fileId);
    }
}