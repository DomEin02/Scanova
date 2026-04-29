package dk.easv.scanova.BLL;

public class SessionManager {

    private static SessionManager instance;

    private boolean loggedIn = false;
    private String  currentUser = "";

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(String username) {
        this.loggedIn    = true;
        this.currentUser = username;
    }

    public void logout() {
        this.loggedIn    = false;
        this.currentUser = "";
    }

    public boolean isLoggedIn() { return loggedIn; }
    public String  getCurrentUser() { return currentUser; }
}
