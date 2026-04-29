package dk.easv.scanova.BLL;

public class UserManager {

    public String login(String username, String password) throws Exception {
        if (username == null || username.isBlank()) {
            throw new Exception("Please enter a username.");
        }
        if (password == null || password.isBlank()) {
            throw new Exception("Please enter a password.");
        }
        if (username.equals("admin") && password.equals("admin123")) {
            return "admin";
        }
        if (username.equals("scanner") && password.equals("scan123")) {
            return "scanner";
        }
        throw new Exception("Incorrect username or password.");
    }
}
