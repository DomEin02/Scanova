package dk.easv.scanova.Model;

public class Profile {

    private int     id;
    private String  name;
    private float   rotation;
    private float   brightness;
    private int     clientId;
    private boolean isActive;

    public Profile(int id, String name, float rotation,
                   float brightness, int clientId, boolean isActive) {
        this.id         = id;
        this.name       = name;
        this.rotation   = rotation;
        this.brightness = brightness;
        this.clientId   = clientId;
        this.isActive   = isActive;
    }

    public Profile(String name, float rotation,
                   float brightness, int clientId) {
        this.name       = name;
        this.rotation   = rotation;
        this.brightness = brightness;
        this.clientId   = clientId;
    }

    public int     getId()         { return id; }
    public String  getName()       { return name; }
    public float   getRotation()   { return rotation; }
    public float   getBrightness() { return brightness; }
    public int     getClientId()   { return clientId; }
    public boolean isActive()      { return isActive; }

    public void setName(String name)            { this.name = name; }
    public void setRotation(float rotation)     { this.rotation = rotation; }
    public void setBrightness(float brightness) { this.brightness = brightness; }
    public void setClientId(int clientId)       { this.clientId = clientId; }

    @Override
    public String toString() { return name; }
}
