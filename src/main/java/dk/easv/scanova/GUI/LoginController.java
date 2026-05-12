package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;


public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;

    private UserManager userManager;

    @FXML
    public void initialize() {
        try {
            userManager = new UserManager();
        } catch (Exception e) {
            showError("System Error", "Could not connect to the database.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();


        if (username.isEmpty() || password.isEmpty()) {
            showError("Missing Input", "Please enter both username and password.");
            return;
        }

        //Temp Login
        if (username.equals("admin") && password.equals("admin123")) {
            User tempUser = new User(1, "admin", "", "Admin");
            SessionManager.getInstance().setCurrentUser(tempUser);
            SceneManager.load("adminView.fxml");
            return;
        }

        if (username.equals("scanner") && password.equals("scan123")) {
            User tempUser = new User(2, "scanner", "", "User");
            SessionManager.getInstance().setCurrentUser(tempUser);
            SceneManager.load("scanView.fxml");
            return;
        }

        try {
            User user = userManager.login(username, password);

            if (user == null) {
                // Wrong username or password
                showError("Login Failed", "Incorrect username or password.");
                passwordField.clear();
                return;
            }


            SessionManager.getInstance().setCurrentUser(user);


            if (user.isAdmin()) {
                SceneManager.load("adminView.fxml");   // Step 8
            } else {
                SceneManager.load("scanView.fxml");
            }

        } catch (Exception e) {
            showError("System Error", "Something went wrong:\n" + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}