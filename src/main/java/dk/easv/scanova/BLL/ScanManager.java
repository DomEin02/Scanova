package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.ScannerClient;
import dk.easv.scanova.Model.Document;
import dk.easv.scanova.Model.ScannedFile;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScanManager {
    private final ScannerClient scannerClient = new ScannerClient();
    private final List<Document> documents    = new ArrayList<>();

    private int fileIdCounter    = 0;
    private int referenceCounter = 0;
    private int documentCounter  = 0;
    private int totalAvailable   = 0;

     // Call this when the user starts a new scan session.
     // Fetches the total count from the API and resets all counters.
    public void initSession() throws Exception {
        totalAvailable   = scannerClient.getTotalCount();
        fileIdCounter    = 0;
        referenceCounter = 0;
        documentCounter  = 0;
        documents.clear();
        documents.add(new Document(++documentCounter));
        System.out.println("Session started. Files available: " + totalAvailable);
    }

     // Fetches the next entry from the API.
     // Unpacks the ZIP and processes each TIFF inside.
     // Returns a list of ScannedFiles added in this fetch.
     // Returns an empty list if a barcode was detected (new doc started).
     // Returns null if there are no more files.
    public List<ScannedFile> fetchNext() throws Exception {
        if (!hasMore()) return null;

        referenceCounter++;
        List<byte[]> tiffs = scannerClient.fetchTiffsById(referenceCounter);

        List<ScannedFile> result = new ArrayList<>();
        for (byte[] data : tiffs) {
            if (isBarcode(data)) {
                System.out.println("  → Barcode! Starting document #" + (documentCounter + 1));
                documents.add(new Document(++documentCounter));
            } else {
                fileIdCounter++;
                ScannedFile file = new ScannedFile(
                        fileIdCounter, referenceCounter, data, documentCounter
                );
                getCurrentDocument().addFile(file);
                result.add(file);
            }
        }
        return result;
    }

     // Barcode detection — tune this threshold after checking
     // the console output for real file sizes from the API.
    private boolean isBarcode(byte[] data) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return false;

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            new MultiFormatReader().decode(bitmap);

            // If we get here, a barcode was successfully decoded
            System.out.println("  → Barcode detected in image!");
            return true;
        } catch (NotFoundException e) {
            // No barcode found in the image — it's a normal TIFF page
            return false;
        } catch (Exception e) {
            System.out.println("  → Could not read image for barcode check: " + e.getMessage());
            return false;
        }
    }

    public boolean hasMore() {
        return referenceCounter < totalAvailable;
    }

    public Document getCurrentDocument() {
        return documents.get(documents.size() - 1);
    }

    public List<Document> getAllDocuments()  { return documents; }
    public int getTotalFilesFetched()        { return fileIdCounter; }
    public int getTotalAvailable()           { return totalAvailable; }
    public int getCurrentDocumentNumber()    { return documentCounter; }

    public static void main(String[] args) throws Exception {
        ScanManager manager = new ScanManager();
        manager.initSession();
        System.out.println("Total available: " + manager.getTotalAvailable());

        // Fetch ALL files
        while (manager.hasMore()) {
            List<ScannedFile> files = manager.fetchNext();
            if (files == null) break;
            if (files.isEmpty()) {
                System.out.println(">>> BARCODE at referenceId="
                        + manager.getTotalFilesFetched()
                        + " → Document #" + manager.getCurrentDocumentNumber() + " started");
            } else {
                for (ScannedFile f : files) {
                    System.out.println("Got: " + f + " (" + f.getImageData().length + " bytes)");
                }
            }
        }

        System.out.println("\n--- SUMMARY ---");
        System.out.println("Total files scanned: " + manager.getTotalFilesFetched());
        System.out.println("Total documents: " + manager.getAllDocuments().size());
        for (Document doc : manager.getAllDocuments()) {
            System.out.println("  " + doc + " → " + doc.getFiles().size() + " files");
        }
    }
}
