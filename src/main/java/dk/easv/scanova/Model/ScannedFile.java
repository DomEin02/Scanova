package dk.easv.scanova.Model;

public class ScannedFile {

    private int fileId;
    private int referenceId;
    private byte[] imageData;
    private int rotation;
    private int documentId;

    public ScannedFile(int fileId, int referenceId, byte[] imageData, int documentId) {
        this.fileId      = fileId;
        this.referenceId = referenceId;
        this.imageData   = imageData;
        this.rotation    = 0;
        this.documentId  = documentId;
    }

    public int getFileId()         { return fileId; }
    public int getReferenceId()    { return referenceId; }
    public byte[] getImageData()   { return imageData; }
    public int getRotation()       { return rotation; }
    public int getDocumentId()     { return documentId; }
    public void setRotation(int r) { this.rotation = r; }
    public void setFileId(int id)  { this.fileId = id; }

    @Override
    public String toString() {
        return "File #" + fileId + " (doc " + documentId + ")";
    }
}