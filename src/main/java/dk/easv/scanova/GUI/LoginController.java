package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserManager userManager;

    @FXML
    public void initialize() {
        try {
            userManager = new UserManager();
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

        // Real DB login
        try {
            User user = userManager.login(username, password);

            if (user == null) {
                errorLabel.setText("Incorrect username or password.");
                passwordField.clear();
                return;
            }

            SessionManager.getInstance().setCurrentUser(user);

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