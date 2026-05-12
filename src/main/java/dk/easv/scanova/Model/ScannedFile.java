package dk.easv.scanova.Model;

public class ScannedFile {

    private int fileId;        // DB ID
    private int referenceId;   // API ID
    private byte[] imageData;
    private int rotation;
    private int documentId;

    public ScannedFile(int fileId,
                       int referenceId,
                       byte[] imageData,
                       int rotation,
                       int documentId) {
        this.fileId = fileId;
        this.referenceId = referenceId;
        this.imageData = imageData;
        this.rotation = rotation;
        this.documentId = documentId;
    }

    public int getFileId() { return fileId; }
    public int getReferenceId() { return referenceId; }
    public byte[] getImageData() { return imageData; }
    public int getRotation() { return rotation; }
    public int getDocumentId() { return documentId; }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "File #" + fileId + " (doc " + documentId + ")";
    }
}