package dk.easv.scanova.Model;

public class Archive {

    private int     id;
    private int     clientId;
    private String  name;
    private String  clientName;
    private boolean isActive = true;

    public Archive(int id, int clientId, String name) {
        this.id       = id;
        this.clientId = clientId;
        this.name     = name;
    }

    public int     getId()         { return id; }
    public int     getClientId()   { return clientId; }
    public String  getName()       { return name; }
    public String  getClientName() {
        return clientName != null ? clientName : "Client #" + clientId;
    }
    public boolean isActive()      { return isActive; }

    public void setName(String name)       { this.name = name; }
    public void setClientName(String name) { this.clientName = name; }
    public void setActive(boolean active)  { this.isActive = active; }

    @Override
    public String toString() { return name; }
}