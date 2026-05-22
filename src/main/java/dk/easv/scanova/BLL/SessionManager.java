package dk.easv.scanova.BLL;

import dk.easv.scanova.BE.User;
import dk.easv.scanova.Model.User;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SessionManager {

    private static SessionManager instance;
    private User          currentUser;
    private LocalDateTime loginTime;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.loginTime   = LocalDateTime.now();
    }

    public User getCurrentUser() { return currentUser; }

    public String getLoginTime() {
        if (loginTime == null) return "—";
        return loginTime.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public void logout() {
        this.currentUser = null;
        this.loginTime   = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}