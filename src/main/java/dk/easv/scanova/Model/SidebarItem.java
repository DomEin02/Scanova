package dk.easv.scanova.model;

public class SidebarItem {

    private final boolean isHeader;
    private final int documentId;
    private final dk.easv.scanova.Model.ScannedFile file;

    // Header (Document)
    public SidebarItem(int documentId) {
        this.isHeader = true;
        this.documentId = documentId;
        this.file = null;
    }

    // File
    public SidebarItem(dk.easv.scanova.Model.ScannedFile file) {
        this.isHeader = false;
        this.documentId = file.getDocumentId();
        this.file = file;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public int getDocumentId() {
        return documentId;
    }

    public dk.easv.scanova.Model.ScannedFile getFile() {
        return file;
    }
}