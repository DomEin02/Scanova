package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.DAL.PageDAO;
import dk.easv.scanova.Model.SidebarItem;

import java.util.List;

public class FileManager {

    private final FileDAO fileDAO = new FileDAO();
    private final PageDAO pageDAO = new PageDAO();

    public void updateRotation(int dbFileId, int rotation) {
        try {
            fileDAO.updateRotation(dbFileId, rotation);
        } catch (Exception e) {
            System.out.println("Could not update rotation: " + e.getMessage());
        }
    }

    public int getRotation(int dbFileId) {
        try {
            return fileDAO.getRotation(dbFileId);
        } catch (Exception e) {
            return 0;
        }
    }

    public void updateFilePath(int dbFileId, String filePath) {
        try {
            fileDAO.updateFilePath(dbFileId, filePath);
        } catch (Exception e) {
            System.out.println("Could not update file path: " + e.getMessage());
        }
    }

    // Update order in both files and pages tables
    public void updateFileOrder(int dbFileId, int newOrder) {
        try {
            fileDAO.updateFileOrder(dbFileId, newOrder);
        } catch (Exception e) {
            System.out.println("Could not update file order in files: "
                    + e.getMessage());
        }
    }

    // Update order in pages table using reference id and document id
    public void updatePageOrder(int referenceId, int documentId, int newOrder) {
        try {
            pageDAO.updatePageOrder(referenceId, documentId, newOrder);
        } catch (Exception e) {
            System.out.println("Could not update page order: " + e.getMessage());
        }
    }
}