package dk.easv.scanova.Model;

public class SidebarItem {

    private final boolean    isHeader;
    private final int        documentId;
    private final ScannedFile file;

    // Header constructor
    public SidebarItem(int documentId) {
        this.isHeader   = true;
        this.documentId = documentId;
        this.file       = null;
    }

    // File constructor
    public SidebarItem(ScannedFile file) {
        this.isHeader   = false;
        this.documentId = file.getDocumentId();
        this.file       = file;
    }

    public boolean    isHeader()    { return isHeader; }
    public int        getDocumentId() { return documentId; }
    public ScannedFile getFile()    { return file; }
}