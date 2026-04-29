package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;


    private final UserManager userManager = new UserManager();

    @FXML
    private void handleLogin() {

        errorLabel.setText("");

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        try {
            String validUser = userManager.login(username, password);
            SessionManager.getInstance().login(validUser);
            SceneManager.load("scanView.fxml");

        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
}