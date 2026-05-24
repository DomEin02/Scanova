package dk.easv.scanova.BE;

import java.util.ArrayList;
import java.util.List;

public class Document {
    private int documentId;
    private int boxId;
    private DocumentStatus status;

    private List<dk.easv.scanova.BE.ScannedFile> files = new ArrayList<>();

    public Document(int documentId, int boxId) {

        this.documentId = documentId;
        this.boxId = boxId;
        this.status = DocumentStatus.IN_PROGRESS;
    }

    public int getDocumentId() {
        return documentId;
    }

    public void setStatus(Document.DocumentStatus status) {
        this.status = status;
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

    public enum DocumentStatus {
        IN_PROGRESS,
        WAITING_FOR_QA,
        QA_COMPLETED,
        EXPORTED
    }
    public DocumentStatus getStatus() {
        return status;
    }
}
