package dk.easv.scanova.Model;

public class Box {

    private int     id;
    private int     archiveId;
    private String  label;
    private int     profileId;
    private String  profileName;
    private String  archiveName;
    private double  rotation;
    private double  brightness;
    private boolean isActive = true;

    // Full constructor — used when loading from DB with joins
    public Box(int id, int archiveId, String label, int profileId,
               String profileName, double rotation, double brightness) {
        this.id          = id;
        this.archiveId   = archiveId;
        this.label       = label;
        this.profileId   = profileId;
        this.profileName = profileName;
        this.rotation    = rotation;
        this.brightness  = brightness;
    }

    // Simple constructor — used when creating in admin
    public Box(int id, int archiveId, String label, int profileId) {
        this.id        = id;
        this.archiveId = archiveId;
        this.label     = label;
        this.profileId = profileId;
    }

    public int     getId()          { return id; }
    public int     getArchiveId()   { return archiveId; }
    public String  getLabel()       { return label; }
    public int     getProfileId()   { return profileId; }
    public String  getProfileName() { return profileName; }
    public String  getArchiveName() {
        return archiveName != null ? archiveName : "Archive #" + archiveId;
    }
    public double  getRotation()    { return rotation; }
    public double  getBrightness()  { return brightness; }
    public boolean isActive()       { return isActive; }

    public void setLabel(String label)      { this.label = label; }
    public void setArchiveName(String name) { this.archiveName = name; }
    public void setActive(boolean active)   { this.isActive = active; }

    public String getExportFolderName() {
        return (profileName != null ? profileName : "Default") + "_" + label;
    }

    @Override
    public String toString() { return label; }
}