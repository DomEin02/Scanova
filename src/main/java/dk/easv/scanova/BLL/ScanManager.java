package dk.easv.scanova.BLL;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dk.easv.scanova.DAL.FileDAO;
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
    private final FileDAO fileDAO = new FileDAO();

    private final List<Document> documents = new ArrayList<>();

    private int referenceCounter = 0;
    private int documentCounter = 0;
    private int totalAvailable = 0;

    public void initSession() throws Exception {
        totalAvailable = scannerClient.getTotalCount();
        referenceCounter = 0;
        documentCounter = 0;
        documents.clear();
        documents.add(new Document(++documentCounter));
    }

    public List<ScannedFile> fetchNext() throws Exception {

        if (!hasMore()) return null;

        referenceCounter++;

        List<byte[]> tiffs = scannerClient.fetchTiffsById(referenceCounter);

        List<ScannedFile> result = new ArrayList<>();

        for (byte[] data : tiffs) {

            if (isBarcode(data)) {
                documents.add(new Document(++documentCounter));
                continue;
            }

            // ✔ DB CREATE → REAL ID
            int fileId = fileDAO.createFileAndGetId(
                    documentCounter,
                    "api_" + referenceCounter
            );

            int rotation = fileDAO.getRotation(fileId);

            ScannedFile file = new ScannedFile(
                    fileId,
                    referenceCounter,
                    data,
                    rotation,
                    documentCounter
            );

            getCurrentDocument().addFile(file);
            result.add(file);
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

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasMore() {
        return referenceCounter < totalAvailable;
    }

    public Document getCurrentDocument() {
        return documents.get(documents.size() - 1);
    }

    public List<Document> getAllDocuments() {
        return documents;
    }

    public int getTotalAvailable() {
        return totalAvailable;
    }
}