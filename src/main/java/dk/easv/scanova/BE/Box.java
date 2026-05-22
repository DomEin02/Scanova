package dk.easv.scanova.BE;

public class Box {

    private int id;
    private int archiveId;
    private String label;

    public Box(int id, int archiveId, String label) {
        this.id = id;
        this.archiveId = archiveId;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public int getArchiveId() {
        return archiveId;
    }

    public String getLabel() {
        return label;
    }
}