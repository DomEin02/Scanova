package dk.easv.scanova.BLL;

import dk.easv.scanova.DAL.ScannerClient;
import dk.easv.scanova.Model.ScannedFile;
import dk.easv.scanova.Model.SidebarItem;
import javafx.collections.ObservableList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExportManager {

    private final ScannerClient scannerClient = new ScannerClient();
    private final FileManager fileManager = new FileManager();

    public int exportSinglePage(ObservableList<SidebarItem> sidebarItems,
                                String exportFolderName) throws Exception {

        File folder = createExportFolder(exportFolderName);

        int docNumber = 0;
        int pageNumber = 0;
        int exported = 0;

        for (SidebarItem item : sidebarItems) {

            if (item.isHeader()) {
                docNumber++;
                pageNumber = 0;
                continue;
            }

            pageNumber++;

            ScannedFile file = item.getFile();

            byte[] data = getFileBytes(file);

            if (data != null && data.length > 0) {

                String fileName = String.format(
                        "document_%d_page_%d.tiff",
                        docNumber,
                        pageNumber
                );

                File outputFile = new File(folder, fileName);

                writeSinglePageTiff(data, outputFile);

                if (file.getDbFileId() != -1) {
                    fileManager.updateFilePath(
                            file.getDbFileId(),
                            outputFile.getAbsolutePath()
                    );
                }

                exported++;

                System.out.println("Exported: " + fileName);
            }
        }

        return exported;
    }

    public int exportMultiPage(ObservableList<SidebarItem> sidebarItems,
                               String exportFolderName) throws Exception {

        File folder = createExportFolder(exportFolderName);

        List<List<SidebarItem>> documents =
                groupByDocument(sidebarItems);

        if (documents.isEmpty()) {
            throw new Exception("No documents to export.");
        }

        int exported = 0;

        for (int i = 0; i < documents.size(); i++) {

            List<SidebarItem> pages = documents.get(i);

            if (pages.isEmpty()) {
                continue;
            }

            List<byte[]> pageBytes = new ArrayList<>();

            for (SidebarItem item : pages) {

                byte[] data = getFileBytes(item.getFile());

                if (data != null && data.length > 0) {
                    pageBytes.add(data);
                }
            }

            if (!pageBytes.isEmpty()) {

                String fileName =
                        String.format("document_%d.tiff", i + 1);

                File outputFile = new File(folder, fileName);

                writeMultiPageTiff(pageBytes, outputFile);

                ScannedFile first = pages.get(0).getFile();

                if (first.getDbFileId() != -1) {
                    fileManager.updateFilePath(
                            first.getDbFileId(),
                            outputFile.getAbsolutePath()
                    );
                }

                exported++;

                System.out.println(
                        "Exported multipage TIFF: "
                                + fileName
                );
            }
        }

        return exported;
    }

    private byte[] getFileBytes(ScannedFile file) {
        // Use image data already in memory — this is what's shown in the sidebar
        if (file.getImageData() != null && file.getImageData().length > 0)
            return file.getImageData();

        // Fallback for history-loaded sessions only
        try {
            List<byte[]> tiffs = scannerClient.fetchTiffsById(
                    file.getReferenceId());
            if (!tiffs.isEmpty()) return tiffs.get(0);
        } catch (Exception e) {
            System.out.println("Could not fetch file "
                    + file.getReferenceId() + ": " + e.getMessage());
        }
        return null;
    }

    private void writeSinglePageTiff(byte[] bytes,
                                     File outputFile) throws Exception {

        BufferedImage image = ImageIO.read(
                new ByteArrayInputStream(bytes)
        );

        if (image == null) {
            throw new Exception("Invalid TIFF image");
        }

        BufferedImage rgb = convertToRGB(image);

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("TIFF");

        if (!writers.hasNext()) {
            throw new Exception("No TIFF writer found");
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream ios =
                     ImageIO.createImageOutputStream(outputFile)) {

            writer.setOutput(ios);

            ImageWriteParam param =
                    writer.getDefaultWriteParam();

            writer.write(null, new IIOImage(rgb, null, null), param);
        }

        writer.dispose();
    }

    private void writeMultiPageTiff(List<byte[]> pageBytes,
                                    File outputFile) throws Exception {

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByFormatName("TIFF");

        if (!writers.hasNext()) {
            throw new Exception("No TIFF writer found");
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream ios =
                     ImageIO.createImageOutputStream(outputFile)) {

            writer.setOutput(ios);

            writer.prepareWriteSequence(null);

            for (byte[] bytes : pageBytes) {

                BufferedImage image = ImageIO.read(
                        new ByteArrayInputStream(bytes)
                );

                if (image == null) {
                    throw new Exception("Could not read TIFF page");
                }

                BufferedImage rgb = convertToRGB(image);

                IIOImage iioImage =
                        new IIOImage(rgb, null, null);

                ImageWriteParam param =
                        writer.getDefaultWriteParam();

                writer.writeToSequence(iioImage, param);
            }

            writer.endWriteSequence();
        }

        writer.dispose();
    }

    private BufferedImage convertToRGB(BufferedImage original) {

        BufferedImage rgb = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = rgb.createGraphics();

        g.drawImage(original, 0, 0, null);

        g.dispose();

        return rgb;
    }

    private List<List<SidebarItem>> groupByDocument(
            ObservableList<SidebarItem> sidebarItems) {

        List<List<SidebarItem>> groups = new ArrayList<>();

        List<SidebarItem> current = null;

        for (SidebarItem item : sidebarItems) {

            if (item.isHeader()) {

                current = new ArrayList<>();

                groups.add(current);

            } else if (current != null) {

                current.add(item);
            }
        }

        return groups;
    }

    private File createExportFolder(String folderName)
            throws Exception {

        File base = new File(
                System.getProperty("user.home"),
                "Scanova_Exports"
        );

        File folder = new File(base, folderName);

        if (!folder.mkdirs() && !folder.exists()) {

            throw new Exception(
                    "Could not create export folder: "
                            + folder.getAbsolutePath()
            );
        }

        System.out.println(
                "Exporting to: "
                        + folder.getAbsolutePath()
        );

        return folder;
    }
}