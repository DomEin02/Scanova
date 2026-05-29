package dk.easv.scanova.Model;

public class SidebarItem {

    private final boolean     isHeader;
    private       int         documentId; // not final — updated on cross-doc moves
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

    public boolean     isHeader()      { return isHeader; }
    public int         getDocumentId() { return documentId; }
    public ScannedFile getFile()       { return file; }

    // Called when a file is dragged/moved to a different document
    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }
}