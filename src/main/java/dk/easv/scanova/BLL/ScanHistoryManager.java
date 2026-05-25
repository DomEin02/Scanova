package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.CaseDAO;
import dk.easv.scanova.DAL.DocumentDAO;
import dk.easv.scanova.DAL.PageDAO;
import dk.easv.scanova.DAL.ScannerClient;
import dk.easv.scanova.Model.ScannedFile;
import dk.easv.scanova.Model.SidebarItem;
import javafx.application.Platform;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanHistoryManager {

    private final CaseDAO       caseDAO       = new CaseDAO();
    private final DocumentDAO   documentDAO   = new DocumentDAO();
    private final PageDAO       pageDAO       = new PageDAO();
    private final ScannerClient scannerClient = new ScannerClient();

    // docNumber → real DB document id — used by ScanViewController.saveOrderToDB
    private final Map<Integer, Integer> loadedDocumentIdMap = new HashMap<>();

    public List<String[]> getCasesForUser(int userId) throws Exception {
        return caseDAO.getCasesForUser(userId);
    }

    public Map<Integer, Integer> getLoadedDocumentIdMap() {
        return loadedDocumentIdMap;
    }

    public boolean loadCaseIntoSidebar(int caseId,
                                       ObservableList<SidebarItem> sidebarItems,
                                       int[] fileCounter) throws Exception {
        Platform.runLater(sidebarItems::clear);
        loadedDocumentIdMap.clear();

        List<String[]> documents = documentDAO.getDocumentsByCaseId(caseId);
        if (documents.isEmpty()) return false;

        int inMemoryFileId = 1;

        for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
            String[] doc   = documents.get(docIndex);
            int documentId = Integer.parseInt(doc[0]);
            int docNumber  = docIndex + 1;

            // Store real DB document id for reorder saving after load
            loadedDocumentIdMap.put(docNumber, documentId);

            final int finalDocNumber = docNumber;
            Platform.runLater(() ->
                    sidebarItems.add(new SidebarItem(finalDocNumber)));

            // Load pages ordered by order_id
            List<int[]> pages = pageDAO.getPagesByDocumentId(documentId);
            System.out.println("Loading doc " + docNumber
                    + " — " + pages.size() + " pages");

            for (int[] page : pages) {
                int referenceId = page[0]; // API reference id
                int rotation    = page[1]; // saved rotation

                try {
                    List<byte[]> tiffs =
                            scannerClient.fetchTiffsById(referenceId);
                    if (!tiffs.isEmpty()) {
                        final int    fId   = inMemoryFileId;
                        final int    fDoc  = docNumber;
                        final int    fRot  = rotation;
                        final int    fRef  = referenceId;
                        final byte[] fData = tiffs.get(0);

                        Platform.runLater(() -> {
                            ScannedFile file = new ScannedFile(
                                    fId, fRef, fData, fDoc);
                            file.setRotation(fRot);
                            file.setDbFileId(-1);
                            sidebarItems.add(new SidebarItem(file));
                        });
                        inMemoryFileId++;
                        System.out.println("  Loaded ref: " + referenceId);
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