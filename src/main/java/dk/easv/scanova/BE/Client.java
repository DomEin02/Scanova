package dk.easv.scanova.BE;

public class Client {

    private int     id;
    private String  name;
    private boolean isActive = true;

    public Client(int id, String name) {
        this.id   = id;
        this.name = name;
    }

    public int     getId()       { return id; }
    public String  getName()     { return name; }
    public boolean isActive()    { return isActive; }

    public void setName(String name)      { this.name = name; }
    public void setActive(boolean active) { this.isActive = active; }

    @Override
    public String toString() { return name; }
}