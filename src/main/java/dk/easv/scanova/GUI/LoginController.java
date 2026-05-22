package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.LogManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.BE.User;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final UserManager userManager = new UserManager();
    private final LogManager logManager = new LogManager();

    @FXML
    public void initialize() {
        try {
            new UserManager();
        } catch (Exception e) {
            errorLabel.setText("Could not connect to database.");
        }
    }

    @FXML
    private void handleLogin() {
        errorLabel.setText("");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        // Temp hardcoded login
        if (username.equals("admin") && password.equals("admin123")) {
            User tempUser = new User(1, "admin", "", "Admin");
            SessionManager.getInstance().setCurrentUser(tempUser);
            logManager.log("LOGIN_SUCCESS", 1, "Hardcoded login: admin");
            SceneManager.load("adminView.fxml");
            return;
        }

        if (username.equals("scanner") && password.equals("scan123")) {
            User tempUser = new User(2, "scanner", "", "User");
            SessionManager.getInstance().setCurrentUser(tempUser);
            logManager.log("LOGIN_SUCCESS", 2, "Hardcoded login: scanner");
            SceneManager.load("scanView.fxml");
            return;
        }

        // Real DB login
        try {
            User user = userManager.login(username, password);

            if (user == null) {
                errorLabel.setText("Incorrect username or password.");
                passwordField.clear();
                logManager.log("LOGIN_FAILED", -1,
                        "Failed login attempt: " + username);
                return;
            }

            SessionManager.getInstance().setCurrentUser(user);
            logManager.log("LOGIN_SUCCESS", user.getId(),
                    "Login: " + user.getUsername());

            if (user.isAdmin()) {
                SceneManager.load("adminView.fxml");
            } else {
                SceneManager.load("scanView.fxml");
            }

        } catch (Exception e) {
            errorLabel.setText("Something went wrong: " + e.getMessage());
        }
    }
}