package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.CaseDAO;
import dk.easv.scanova.DAL.DocumentDAO;
import dk.easv.scanova.DAL.PageDAO;
import dk.easv.scanova.DAL.ScannerClient;
import dk.easv.scanova.Model.ScannedFile;
import dk.easv.scanova.Model.SidebarItem;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.List;

public class ScanHistoryManager {

    private final CaseDAO       caseDAO       = new CaseDAO();
    private final DocumentDAO   documentDAO   = new DocumentDAO();
    private final PageDAO       pageDAO       = new PageDAO();
    private final ScannerClient scannerClient = new ScannerClient();

    // Get all previous scan sessions for this user
    public List<String[]> getCasesForUser(int userId) throws Exception {
        return caseDAO.getCasesForUser(userId);
    }

    // Load a previous scan session into the sidebar
    public boolean loadCaseIntoSidebar(int caseId,
                                       ObservableList<SidebarItem> sidebarItems,
                                       int[] fileCounter) throws Exception {
        Platform.runLater(sidebarItems::clear);

        List<String[]> documents = documentDAO.getDocumentsByCaseId(caseId);
        if (documents.isEmpty()) return false;

        int inMemoryFileId = 1;

        for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
            String[] doc      = documents.get(docIndex);
            int documentId    = Integer.parseInt(doc[0]);
            int docNumber     = docIndex + 1;

            // Add document header on UI thread
            final int finalDocNumber = docNumber;
            Platform.runLater(() ->
                    sidebarItems.add(new SidebarItem(finalDocNumber)));

            // Load pages for this document from pages table
            List<int[]> pages = pageDAO.getPagesByDocumentId(documentId);
            System.out.println("Loading doc " + docNumber
                    + " — " + pages.size() + " pages from DB");

            for (int[] page : pages) {
                int referenceId = page[0]; // page_number = API reference id
                int rotation    = page[1]; // rotation

                try {
                    // Fetch image from API
                    List<byte[]> tiffs =
                            scannerClient.fetchTiffsById(referenceId);
                    if (!tiffs.isEmpty()) {
                        final int fFileId = inMemoryFileId;
                        final int fDocNum = docNumber;
                        final int fRot    = rotation;
                        final int fRef    = referenceId;
                        final byte[] fData = tiffs.get(0);

                        Platform.runLater(() -> {
                            ScannedFile file = new ScannedFile(
                                    fFileId, fRef, fData, fDocNum);
                            file.setRotation(fRot);
                            file.setDbFileId(-1); // no files table entry
                            sidebarItems.add(new SidebarItem(file));
                        });
                        inMemoryFileId++;

                        System.out.println("  Loaded ref: " + referenceId
                                + " rotation: " + rotation);
                    }
                } catch (Exception e) {
                    System.out.println("Could not load ref "
                            + referenceId + ": " + e.getMessage());
                }
            }
        }

        fileCounter[0] = inMemoryFileId - 1;
        return fileCounter[0] > 0;
    }
}