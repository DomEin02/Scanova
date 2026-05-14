package dk.easv.scanova.Model;

import java.util.ArrayList;
import java.util.List;

public class Document {
    private int documentId;
    private String boxId;

    private List<dk.easv.scanova.Model.ScannedFile> files = new ArrayList<>();

    public Document(int documentId, String boxId) {

        this.documentId = documentId;
        this.boxId = boxId;
    }

    public int getDocumentId() {
        return documentId;
    }

    public String getBoxId() {
        return boxId;
    }

    public List<dk.easv.scanova.Model.ScannedFile> getFiles() {
        return files;
    }

    public void addFile(dk.easv.scanova.Model.ScannedFile file) {

        files.add(file);
    }

    @Override
    public String toString() {
        return "Document #" + documentId +
                " [Box: " + boxId + "] (" +
                files.size() + " files)";
    }
}
