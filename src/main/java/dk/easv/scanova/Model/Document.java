package dk.easv.scanova.Model;

import java.util.ArrayList;
import java.util.List;

public class Document {
    private int documentId;
    private List<ScannedFile> files = new ArrayList<>();

    public Document(int documentId) {
        this.documentId = documentId;
    }

    public int getDocumentId() { return documentId; }
    public List<ScannedFile> getFiles() { return files; }

    public void addFile(ScannedFile file) {
        files.add(file);
    }

    @Override
    public String toString() {
        return "Document #" + documentId + " (" + files.size() + " files)";
    }
}
