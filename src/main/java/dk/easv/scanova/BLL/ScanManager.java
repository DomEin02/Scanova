package dk.easv.scanova.BLL;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dk.easv.scanova.BE.ScannedFile;
import dk.easv.scanova.DAL.ScannerClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ScanManager {

    private final ScannerClient scannerClient = new ScannerClient();

    private int referenceCounter = 0;
    private int totalAvailable = 0;

    public void initSession() throws Exception {
        totalAvailable = scannerClient.getTotalCount();
        referenceCounter = 0;
        System.out.println("Session started. Files available: " + totalAvailable);
    }

    public List<ScannedFile> fetchNext() throws Exception {

        if (!hasMore()) return null;

        referenceCounter++;

        List<byte[]> tiffs = scannerClient.fetchTiffsById(referenceCounter);
        List<ScannedFile> result = new ArrayList<>();

        int fileIdCounter = 0;

        for (byte[] data : tiffs) {

            fileIdCounter++;

            boolean barcode = isBarcode(data);

            ScannedFile file = new ScannedFile(
                    fileIdCounter,
                    referenceCounter,
                    data,
                    0,
                    0,
                    "scan_" + fileIdCounter,
                    barcode
            );

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

            System.out.println("→ Barcode detected in image!");
            return true;

        } catch (NotFoundException e) {
            return false;

        } catch (Exception e) {
            System.out.println("→ Could not read image: " + e.getMessage());
            return false;
        }
    }

    public boolean hasMore() {
        return referenceCounter < totalAvailable;
    }

    public int getTotalAvailable() {
        return totalAvailable;
    }
}