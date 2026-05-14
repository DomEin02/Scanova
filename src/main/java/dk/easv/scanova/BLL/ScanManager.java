package dk.easv.scanova.BLL;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dk.easv.scanova.DAL.ScannerClient;
import dk.easv.scanova.Model.Document;
import dk.easv.scanova.Model.ScannedFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScanManager {

    private final ScannerClient scannerClient = new ScannerClient();
    private final List<Document> documents = new ArrayList<>();

    private int fileIdCounter    = 0;
    private int referenceCounter = 0;
    private int documentCounter  = 0;
    private int totalAvailable   = 0;

    private String currentBoxId;

    public void setCurrentBoxId(String boxId) {
        this.currentBoxId = boxId;
    }

    public void initSession() throws Exception {
        totalAvailable   = scannerClient.getTotalCount();
        fileIdCounter    = 0;
        referenceCounter = 0;
        documentCounter  = 0;
        documents.clear();
        // No document created here — first barcode creates Document #1
        System.out.println("Session started. Files available: " + totalAvailable);
    }

    public List<ScannedFile> fetchNext() throws Exception {
        if (!hasMore()) return null;
        referenceCounter++;
        List<byte[]> tiffs = scannerClient.fetchTiffsById(referenceCounter);
        List<ScannedFile> result = new ArrayList<>();
        for (byte[] data : tiffs) {
            if (isBarcode(data)) {
                System.out.println("  → Barcode! Starting document #" + (documentCounter + 1));
                documents.add(new Document(++documentCounter, currentBoxId));
                // Barcode page is first file in new document
                fileIdCounter++;
                ScannedFile barcodeFile = new ScannedFile(fileIdCounter, referenceCounter, data, documentCounter);
                getCurrentDocument().addFile(barcodeFile);
                result.add(barcodeFile);

            } else {
                // Skip files before first barcode
                if (documentCounter == 0) {
                    System.out.println("  → Skipping — no barcode detected yet");
                    continue;
                }
                fileIdCounter++;
                ScannedFile file = new ScannedFile(fileIdCounter, referenceCounter, data, documentCounter);
                getCurrentDocument().addFile(file);
                result.add(file);
            }
        }
        return result;
    }

    private boolean isBarcode(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return false;
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            new MultiFormatReader().decode(bitmap);
            System.out.println("  → Barcode detected in image!");
            return true;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception e) {
            System.out.println("  → Could not read image: " + e.getMessage());
            return false;
        }
    }

    public boolean hasMore() {
        return referenceCounter < totalAvailable; }

    public Document getCurrentDocument() {
        if (documents.isEmpty()) return null;
        return documents.get(documents.size() - 1);
    }

    public List<Document> getAllDocuments()  { return documents; }
    public int getTotalFilesFetched()        { return fileIdCounter; }
    public int getTotalAvailable()           { return totalAvailable; }
    public int getCurrentDocumentNumber()    { return documentCounter; }
}