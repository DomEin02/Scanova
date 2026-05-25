package dk.easv.scanova.BE;

public class Archive {

    private int id;
    private int clientId;
    private String name;

    public Archive(int id, int clientId, String name) {
        this.id = id;
        this.clientId = clientId;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }
}
