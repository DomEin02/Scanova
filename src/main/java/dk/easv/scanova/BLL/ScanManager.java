package dk.easv.scanova.BLL;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dk.easv.scanova.DAL.BoxDAO;
import dk.easv.scanova.DAL.CaseDAO;
import dk.easv.scanova.DAL.DocumentDAO;
import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.DAL.PageDAO;
import dk.easv.scanova.DAL.ProfileDAO;
import dk.easv.scanova.DAL.ScannerClient;
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
import java.util.stream.Collectors;

public class ScanManager {

    private final ScannerClient scannerClient = new ScannerClient();
    private final BoxDAO        boxDAO        = new BoxDAO();
    private final CaseDAO       caseDAO       = new CaseDAO();
    private final DocumentDAO   documentDAO   = new DocumentDAO();
    private final FileDAO       fileDAO       = new FileDAO();
    private final PageDAO       pageDAO       = new PageDAO();
    private final ProfileDAO    profileDAO    = new ProfileDAO();

    private final List<Document>        documents     = new ArrayList<>();
    private final Map<Integer, Integer> documentIdMap = new HashMap<>();

    private int fileIdCounter    = 0;
    private int referenceCounter = 0;
    private int documentCounter  = 0;
    private int totalAvailable   = 0;
    private int activeCaseId     = -1;

    private Box    activeBox;
    private String currentBoxId = "UNKNOWN";

    // Validate box and profile before session
    public Box validateAndPrepareSession(String boxLabel,
                                         String profileName) throws Exception {
        Box box = boxDAO.getBoxByLabel(boxLabel);

        int profileId = profileDAO.getProfileIdByName(profileName);
        if (profileId == -1)
            throw new Exception("Profile '" + profileName + "' not found.");

        if (boxDAO.isCombinationAlreadyUsed(box.getId(), profileId))
            throw new Exception(
                    "Box '" + boxLabel + "' has already been scanned "
                            + "with profile '" + profileName + "'. "
                            + "Please choose a different box or profile.");

        return box;
    }

    // Init session with box
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
                box.getId(), "Scan of " + box.getLabel());

        System.out.println("Session started — Box: " + box.getLabel()
                + " | Profile: " + box.getProfileName()
                + " | Auto-rotation: " + (int) box.getRotation() + "°"
                + " | Brightness: " + box.getBrightness()
                + " | Files: " + totalAvailable);
    }

    // Fetch next file from API
    public List<ScannedFile> fetchNext() throws Exception {
        if (!hasMore()) return null;

        referenceCounter++;
        List<byte[]> tiffs = scannerClient.fetchTiffsById(referenceCounter);
        List<ScannedFile> result = new ArrayList<>();

        int profileRotation = (activeBox != null)
                ? (int) activeBox.getRotation() : 0;

        for (byte[] data : tiffs) {
            if (isBarcode(data)) {
                System.out.println("  → Barcode! Starting document #"
                        + (documentCounter + 1));
                documents.add(new Document(++documentCounter, currentBoxId));

                if (activeCaseId != -1) {
                    try {
                        int realDocId = documentDAO.createDocument(
                                activeCaseId, "Document " + documentCounter);
                        documentIdMap.put(documentCounter, realDocId);
                        System.out.println("  → DB document created: " + realDocId);
                    } catch (Exception e) {
                        System.out.println("  → Could not create DB document: "
                                + e.getMessage());
                    }
                }

                fileIdCounter++;
                ScannedFile barcodeFile = new ScannedFile(
                        fileIdCounter, referenceCounter, data, documentCounter);
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
                file.setRotation(profileRotation);
                getCurrentDocument().addFile(file);
                saveFileToDB(file, false);
                result.add(file);
            }
        }
        return result;
    }

    // Save file to files + pages tables
    private void saveFileToDB(ScannedFile file, boolean barcodeDetected) {
        try {
            int realDocId = documentIdMap.getOrDefault(
                    file.getDocumentId(), -1);

            if (realDocId != -1) {
                try {
                    int dbFileId = fileDAO.insertFile(
                            file, realDocId, barcodeDetected);
                    file.setDbFileId(dbFileId);
                } catch (Exception e) {
                    System.out.println("  → Could not save to files: "
                            + e.getMessage());
                }

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

    // Get profiles assigned to current user
    public List<String> getProfilesForCurrentUser(int userId) throws Exception {
        return profileDAO.getProfilesForUser(userId);
    }

    // Get all available box labels (for session popup dropdown)
    public List<String> getAvailableBoxLabels() throws Exception {
        return boxDAO.getAllBoxes().stream()
                .map(Box::getLabel)
                .collect(Collectors.toList());
    }

    // Get real DB document id by in-memory document number
    public int getRealDocumentId(int docNumber) {
        return documentIdMap.getOrDefault(docNumber, -1);
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
            System.out.println("  → Could not check barcode: " + e.getMessage());
            return false;
        }
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
    public int getActiveCaseId()             { return activeCaseId; }
    public Box getActiveBox()                { return activeBox; }
}