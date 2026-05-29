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

    private static final String BASE_URL =
            "https://studentiffapi-production.up.railway.app";

    // Get total count
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

    // Fetch one file by id — used for scan one at a time
    public List<byte[]> fetchTiffsById(int id) throws Exception {
        URL url = new URL(BASE_URL + "/getById/" + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API error for id=" + id
                    + " status=" + responseCode);
        }

        byte[] zipBytes;
        try (InputStream is = conn.getInputStream()) {
            zipBytes = is.readAllBytes();
        }

        System.out.println("ID " + id + " → ZIP size: "
                + zipBytes.length + " bytes");
        return unzip(zipBytes);
    }

    // Fetch first N files
    public List<byte[]> fetchTiffs(int amount) throws Exception {
        URL url = new URL(BASE_URL + "/getFiles/" + amount);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200)
            throw new Exception("API error status=" + responseCode);

        byte[] zipBytes;
        try (InputStream is = conn.getInputStream()) {
            zipBytes = is.readAllBytes();
        }

        return unzip(zipBytes);
    }

    // Fetch files with offset and limit
    public List<byte[]> fetchTiffsWithOffset(int offset,
                                             int limit) throws Exception {
        URL url = new URL(BASE_URL + "/getFiles/" + offset + "/" + limit);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200)
            throw new Exception("API error status=" + responseCode);

        byte[] zipBytes;
        try (InputStream is = conn.getInputStream()) {
            zipBytes = is.readAllBytes();
        }

        return unzip(zipBytes);
    }

    // Fetch all files — used for export
    public List<byte[]> fetchAllTiffs() throws Exception {
        URL url = new URL(BASE_URL + "/getAllFiles");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200)
            throw new Exception("API error status=" + responseCode);

        byte[] zipBytes;
        try (InputStream is = conn.getInputStream()) {
            zipBytes = is.readAllBytes();
        }

        System.out.println("All files ZIP size: " + zipBytes.length + " bytes");
        return unzip(zipBytes);
    }

    // Shared unzip helper
    private List<byte[]> unzip(byte[] zipBytes) throws Exception {
        List<byte[]> tiffs = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(zipBytes))) {
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
}
