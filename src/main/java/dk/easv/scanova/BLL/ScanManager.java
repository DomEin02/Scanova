package dk.easv.scanova.BLL;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dk.easv.scanova.DAL.*;
import dk.easv.scanova.Model.Box;
import dk.easv.scanova.Model.Document;
import dk.easv.scanova.Model.ScannedFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScanManager {

    // DAL dependencies
    private final ScannerClient scannerClient = new ScannerClient();
    private final BoxDAO        boxDAO        = new BoxDAO();
    private final CaseDAO       caseDAO       = new CaseDAO();
    private final DocumentDAO   documentDAO   = new DocumentDAO();
    private final FileDAO       fileDAO       = new FileDAO();
    private final ProfileDAO    profileDAO    = new ProfileDAO();
    private final PageDAO pageDAO = new PageDAO();

    // In-memory state
    private final List<Document>        documents     = new ArrayList<>();
    private final Map<Integer, Integer> documentIdMap = new HashMap<>();

    private int fileIdCounter    = 0;
    private int referenceCounter = 0;
    private int documentCounter  = 0;
    private int totalAvailable   = 0;
    private int activeCaseId     = -1;

    private Box    activeBox;
    private String currentBoxId = "UNKNOWN";

    // Validate box and profile before scanning
    public Box validateAndPrepareSession(String boxLabel,
                                         String profileName) throws Exception {
        Box box = boxDAO.getBoxByLabel(boxLabel);

        int profileId = profileDAO.getProfileIdByName(profileName);
        if (profileId == -1)
            throw new Exception("Profile '" + profileName + "' not found.");

        if (boxDAO.isCombinationAlreadyUsed(box.getId(), profileId))
            throw new Exception(
                    "Box '" + boxLabel + "' has already been scanned " +
                            "with profile '" + profileName + "'. " +
                            "Please choose a different box or profile.");

        return box;
    }

    // Init session with real box
    public void initSession(Box box) throws Exception {
        this.activeBox    = box;
        this.currentBoxId = box.getLabel();
        totalAvailable    = scannerClient.getTotalCount();
        fileIdCounter     = 0;
        referenceCounter  = 0;
        documentCounter   = 0;
        documents.clear();
        documentIdMap.clear();

        activeCaseId = caseDAO.createCase(
                box.getId(),
                "Scan of " + box.getLabel());

        System.out.println("Session started — Box: " + box.getLabel()
                + " | Profile: " + box.getProfileName()
                + " | Auto-rotation: " + (int) box.getRotation() + "°"
                + " | Brightness: " + box.getBrightness()
                + " | Files: " + totalAvailable);
    }

    // Init session without box — fallback
    public void initSession() throws Exception {
        totalAvailable   = scannerClient.getTotalCount();
        fileIdCounter    = 0;
        referenceCounter = 0;
        documentCounter  = 0;
        documents.clear();
        documentIdMap.clear();
        activeCaseId = -1;
        activeBox    = null;
        currentBoxId = "UNKNOWN";
        System.out.println("Session started (no box). Files: " + totalAvailable);
    }

    // Fetch next file from API
    public List<ScannedFile> fetchNext() throws Exception {
        if (!hasMore()) return null;

        referenceCounter++;
        List<byte[]> tiffs = scannerClient.fetchTiffsById(referenceCounter);
        List<ScannedFile> result = new ArrayList<>();

        // Get profile rotation to auto-apply to every file
        int profileRotation = (activeBox != null)
                ? (int) activeBox.getRotation()
                : 0;

        for (byte[] data : tiffs) {
            if (isBarcode(data)) {
                System.out.println("  → Barcode! Starting document #"
                        + (documentCounter + 1));
                documents.add(new Document(++documentCounter, currentBoxId));

                if (activeCaseId != -1) {
                    try {
                        int realDocId = documentDAO.createDocument(
                                activeCaseId,
                                "Document " + documentCounter);
                        documentIdMap.put(documentCounter, realDocId);
                        System.out.println("  → DB document created: "
                                + realDocId);
                    } catch (Exception e) {
                        System.out.println("  → Could not create DB document: "
                                + e.getMessage());
                    }
                }

                fileIdCounter++;
                ScannedFile barcodeFile = new ScannedFile(
                        fileIdCounter, referenceCounter, data, documentCounter);
                // Apply profile rotation automatically
                barcodeFile.setRotation(profileRotation);
                getCurrentDocument().addFile(barcodeFile);
                saveFileToDB(barcodeFile, true);
                result.add(barcodeFile);

            } else {
                if (documentCounter == 0) {
                    System.out.println("  → Skipping — no barcode yet");
                    continue;
                }
                fileIdCounter++;
                ScannedFile file = new ScannedFile(
                        fileIdCounter, referenceCounter, data, documentCounter);
                // Apply profile rotation automatically
                file.setRotation(profileRotation);
                getCurrentDocument().addFile(file);
                saveFileToDB(file, false);
                result.add(file);
            }
        }
        return result;
    }

    // Update saveFileToDB() to also save to pages:
    private void saveFileToDB(ScannedFile file, boolean barcodeDetected) {
        try {
            int realDocId = documentIdMap.getOrDefault(
                    file.getDocumentId(), -1);

            if (realDocId != -1) {
                // Save to files table
                try {
                    int dbFileId = fileDAO.insertFile(
                            file, realDocId, barcodeDetected);
                    file.setDbFileId(dbFileId);
                } catch (Exception e) {
                    System.out.println("  → Could not save to files: "
                            + e.getMessage());
                }

                // Also save to pages table — this is what history uses
                try {
                    pageDAO.insertPageWithDocumentId(file, realDocId);
                } catch (Exception e) {
                    System.out.println("  → Could not save to pages: "
                            + e.getMessage());
                }
            } else {
                System.out.println("  → No real doc id — skipping DB save");
            }
        } catch (Exception e) {
            System.out.println("  → saveFileToDB error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Get profiles for current user
    public List<String> getProfilesForCurrentUser(int userId) throws Exception {
        return profileDAO.getProfilesForUser(userId);
    }

    // Barcode detection
    private boolean isBarcode(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return false;
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            new MultiFormatReader().decode(bitmap);
            System.out.println("  → Barcode detected!");
            return true;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception e) {
            System.out.println("  → Could not check barcode: "
                    + e.getMessage());
            return false;
        }
    }

    // Get real DB document id by in-memory document number
    public int getRealDocumentId(int docNumber) {
        return documentIdMap.getOrDefault(docNumber, -1);
    }

    // Getters
    public boolean hasMore() {
        return referenceCounter < totalAvailable;
    }

    public Document getCurrentDocument() {
        if (documents.isEmpty()) return null;
        return documents.get(documents.size() - 1);
    }

    public List<Document> getAllDocuments()  { return documents; }
    public int getTotalFilesFetched()        { return fileIdCounter; }
    public int getTotalAvailable()           { return totalAvailable; }
    public int getCurrentDocumentNumber()    { return documentCounter; }
    public int getActiveCaseId()             { return activeCaseId; }
    public Box getActiveBox()                { return activeBox; }
}