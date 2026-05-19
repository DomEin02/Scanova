package dk.easv.scanova.Model;

public class User {

    private int     id;
    private String  username;
    private String  password;
    private String  role;
    private boolean isActive = true;

    public User(int id, String username, String password, String role) {
        this.id       = id;
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    public int     getId()        { return id; }
    public String  getUsername()  { return username; }
    public String  getPassword()  { return password; }
    public String  getRole()      { return role; }
    public boolean isActive()     { return isActive; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role)         { this.role = role; }
    public void setActive(boolean active)    { this.isActive = active; }

    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(this.role);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username
                + "', role='" + role + "', active=" + isActive + "}";
    }
}