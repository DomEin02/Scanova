package dk.easv.scanova.DAL;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScannerClient {
    private static final String BASE_URL = "https://studentiffapi-production.up.railway.app";

     // Fetches /getById/{id}, unzips the response in memory,
     // and returns all TIFF byte arrays found inside the ZIP.
    public List<byte[]> fetchTiffsById(int id) throws Exception {
        URL url = new URL(BASE_URL + "/getById/" + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API error for id=" + id + " status=" + responseCode);
        }

        // Read full ZIP into memory
        byte[] zipBytes;
        try (InputStream is = conn.getInputStream()) {
            zipBytes = is.readAllBytes();
        }

        System.out.println("ID " + id + " → ZIP size: " + zipBytes.length + " bytes");

        // Unzip and collect TIFFs
        List<byte[]> tiffs = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] fileBytes = zis.readAllBytes();
                    System.out.println("  └─ " + entry.getName()
                            + " (" + fileBytes.length + " bytes)");
                    tiffs.add(fileBytes);
                }
                zis.closeEntry();
            }
        }
        return tiffs;
    }

      //Returns the total number of entries available in the API.
    public int getTotalCount() throws Exception {
        URL url = new URL(BASE_URL + "/getCount");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (InputStream is = conn.getInputStream()) {
            return Integer.parseInt(new String(is.readAllBytes()).trim());
        }
    }
}
