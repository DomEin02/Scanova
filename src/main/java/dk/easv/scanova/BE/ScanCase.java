package dk.easv.scanova.BE;

public class ScanCase {

    private int id;
    private int boxId;
    private String title;

    public ScanCase(int id, int boxId, String title) {
        this.id = id;
        this.boxId = boxId;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public int getBoxId() {
        return boxId;
    }

    public String getTitle() {
        return title;
    }
}
