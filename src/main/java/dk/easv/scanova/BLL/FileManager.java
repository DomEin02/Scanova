package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.DAL.PageDAO;

public class FileManager {

    private final FileDAO fileDAO = new FileDAO();
    private final PageDAO pageDAO = new PageDAO();

    // Called when rotation slider is released in ImagePreviewController
    public void updateRotation(int dbFileId, int rotation) {
        try {
            fileDAO.updateRotation(dbFileId, rotation);
        } catch (Exception e) {
            System.out.println("Could not update rotation: " + e.getMessage());
        }
    }

    // Called after export to update file path in DB
    public void updateFilePath(int dbFileId, String filePath) {
        try {
            fileDAO.updateFilePath(dbFileId, filePath);
        } catch (Exception e) {
            System.out.println("Could not update file path: " + e.getMessage());
        }
    }

    // Called by saveOrderToDB in ScanViewController — updates files table
    public void updateFileOrder(int dbFileId, int newOrder) {
        try {
            fileDAO.updateFileOrder(dbFileId, newOrder);
        } catch (Exception e) {
            System.out.println("Could not update file order: " + e.getMessage());
        }
    }

    // Called by saveOrderToDB in ScanViewController — updates pages table
    public void updatePageOrder(int referenceId, int documentId, int newOrder) {
        try {
            pageDAO.updatePageOrder(referenceId, documentId, newOrder);
        } catch (Exception e) {
            System.out.println("Could not update page order: " + e.getMessage());
        }
    }
}