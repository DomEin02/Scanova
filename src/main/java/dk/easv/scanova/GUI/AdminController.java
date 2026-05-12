package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;

    @FXML private Label formTitle;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label feedbackLabel;
    @FXML private Button saveButton;
    @FXML private Label loggedInLabel;

    private UserManager userManager;
    private User userBeingEdited = null;

    @FXML
    public void initialize() {
        // No try/catch needed — UserManager constructor no longer throws
        userManager = new UserManager();

        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            loggedInLabel.setText("Logged in as: " + current.getUsername());
        }

        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "User"));
        roleComboBox.setValue("User");

        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getId()).asObject());
        colUsername.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getUsername()));
        colRole.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getRole()));

        loadUsers();
    }

    private void loadUsers() {
        try {
            ObservableList<User> users =
                    FXCollections.observableArrayList(userManager.getAllUsers());
            userTable.setItems(users);
        } catch (Exception e) {
            showFeedback("Could not load users: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role     = roleComboBox.getValue();

        try {
            if (userBeingEdited == null) {
                userManager.createUser(username, password, role);
                showFeedback("User '" + username + "' created successfully!", true);
            } else {
                userManager.updateUser(userBeingEdited.getId(), username, password, role);
                showFeedback("User '" + username + "' updated successfully!", true);
            }
            loadUsers();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    @FXML
    private void handleEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to edit.", false);
            return;
        }
        userBeingEdited = selected;
        usernameField.setText(selected.getUsername());
        passwordField.clear();
        roleComboBox.setValue(selected.getRole());
        formTitle.setText("Edit User");
        saveButton.setText("Update User");
        showFeedback("Editing: " + selected.getUsername()
                + " — leave password blank to keep existing.", true);
    }

    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to delete.", false);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete user '" + selected.getUsername() + "'?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userManager.deleteUser(selected.getId());
                    showFeedback("User deleted successfully.", true);
                    loadUsers();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not delete: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleClearForm() {
        userBeingEdited = null;
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue("User");
        formTitle.setText("Create New User");
        saveButton.setText("Create User");
        feedbackLabel.setText("");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}