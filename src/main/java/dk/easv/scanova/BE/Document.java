package dk.easv.scanova.BE;

import java.util.ArrayList;
import java.util.List;

public class Document {
    private int documentId;
    private int boxId;

    private List<dk.easv.scanova.BE.ScannedFile> files = new ArrayList<>();

    public Document(int documentId, int boxId) {

        this.documentId = documentId;
        this.boxId = boxId;
    }

    public int getDocumentId() {
        return documentId;
    }

    public int getBoxId() {
        return boxId;
    }

    public List<dk.easv.scanova.BE.ScannedFile> getFiles() {
        return files;
    }

    public void addFile(dk.easv.scanova.BE.ScannedFile file) {

        files.add(file);
    }

    @Override
    public String toString() {
        return "Document #" + documentId +
                " [Box: " + boxId + "] (" +
                files.size() + " files)";
    }
}
