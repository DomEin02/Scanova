package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.LogManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField    usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private UserManager userManager;
    private LogManager  logManager;

    @FXML
    public void initialize() {
        try {
            userManager = new UserManager();
            logManager  = new LogManager();
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

        try {
            User user = userManager.login(username, password);

            if (user == null) {
                errorLabel.setText("Incorrect username or password.");
                passwordField.clear();
                try { logManager.log("LOGIN_FAILED", -1,
                        "Failed login: " + username); } catch (Exception ignored) {}
                return;
            }

            SessionManager.getInstance().setCurrentUser(user);
            try { logManager.log("LOGIN_SUCCESS", user.getId(),
                    "Login: " + user.getUsername()); } catch (Exception ignored) {}

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