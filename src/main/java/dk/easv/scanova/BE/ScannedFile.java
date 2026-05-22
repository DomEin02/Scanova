package dk.easv.scanova.BE;

public class ScannedFile {

    //Fields
    private int fileId;
    private int referenceId;
    private byte[] imageData;
    private int rotation;
    private int documentId;
    private int fileOrderId;
    private String filepath;
    private boolean barcodeDetected;


    //Constructor
    public ScannedFile(int fileId,
                       int referenceId,
                       byte[] imageData,
                       int documentId,
                       int fileOrderId,
                       String filepath,
                       boolean barcodeDetected) {

        this.fileId = fileId;
        this.referenceId = referenceId;
        this.imageData = imageData;
        this.documentId = documentId;
        this.fileOrderId = fileOrderId;
        this.filepath = filepath;
        this.barcodeDetected = barcodeDetected;
        this.rotation = 0;
    }

    public int getFileId() { return fileId; }

    public int getReferenceId() { return referenceId; }

    public byte[] getImageData() { return imageData; }

    public int getRotation() { return rotation; }

    public int getDocumentId() { return documentId; }

    public void setRotation(int r) { this.rotation = r; }

    public void setFileId(int id) { this.fileId = id; }

    public int getFileOrderId() { return fileOrderId; }

    public String getFilePath() { return filepath; }

    public boolean isBarcodeDetected() { return barcodeDetected; }

    public void setFileOrderId(int fileOrderId) { this.fileOrderId = fileOrderId; }

    public void setFilePath(String filepath) { this.filepath = filepath; }

    public void setBarcodeDetected(boolean barcodeDetected) {
        this.barcodeDetected = barcodeDetected;
    }

    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }

    public void setReferenceId(int referenceId) {
        this.referenceId = referenceId;
    }

    @Override
    public String toString() {
        return "File #" + fileId + " (doc " + documentId + ")";
    }
}