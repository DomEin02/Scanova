package dk.easv.scanova.BE;

public class ScanSession {

    private Box box;

    private Document currentDocument;
    private int fileOrderCounter = 0;

    public Box getBox() {
        return box;
    }

    public void setBox(Box box) {
        this.box = box;
    }

    public Document getCurrentDocument() {
        return currentDocument;
    }

    public void setCurrentDocument(Document currentDocument) {
        this.currentDocument = currentDocument;
    }

    public int getFileOrderCounter() {
        return fileOrderCounter;
    }

    public void setFileOrderCounter(int fileOrderCounter) {
        this.fileOrderCounter = fileOrderCounter;
    }

    public int nextFileOrderId() {
        return ++fileOrderCounter;
    }
}
