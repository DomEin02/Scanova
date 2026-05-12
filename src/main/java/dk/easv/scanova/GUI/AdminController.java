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

/**
 * Controller for adminView.fxml
 *
 * Two modes:
 *   CREATE mode — form is blank, Save button says "Create User"
 *   EDIT mode   — form filled with selected user, Save button says "Update User"
 *
 * Mode switches automatically when admin clicks "Edit Selected".
 */
public class AdminController {

    // ── Table (left side) ──────────────────────────────────
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;

    // ── Form (right side) ──────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label         feedbackLabel;
    @FXML private Button        saveButton;

    // ── Header ─────────────────────────────────────────────
    @FXML private Label loggedInLabel;

    // ── Internal state ─────────────────────────────────────
    private UserManager userManager;
    private User userBeingEdited = null; // null = CREATE mode

    // ───────────────────────────────────────────────────────
    // initialize() runs automatically when the FXML loads
    // ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Set up UserManager
        try {
            userManager = new UserManager();
        } catch (Exception e) {
            showFeedback("Could not connect to database: " + e.getMessage(), false);
            return;
        }

        // Show who is logged in
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            loggedInLabel.setText("Logged in as: " + current.getUsername());
        }

        // Populate role dropdown
        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "User"));
        roleComboBox.setValue("User"); // default selection

        // Wire table columns to User fields
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getId()).asObject());

        colUsername.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getUsername()));

        colRole.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getRole()));

        // Load users from database into the table
        loadUsers();
    }

    // ───────────────────────────────────────────────────────
    // Load all users from DB into the TableView
    // ───────────────────────────────────────────────────────
    private void loadUsers() {
        try {
            ObservableList<User> users =
                    FXCollections.observableArrayList(userManager.getAllUsers());
            userTable.setItems(users);
        } catch (Exception e) {
            showFeedback("Could not load users: " + e.getMessage(), false);
        }
    }

    // ───────────────────────────────────────────────────────
    // SAVE — handles both CREATE and EDIT depending on mode
    // ───────────────────────────────────────────────────────
    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role     = roleComboBox.getValue();

        try {
            if (userBeingEdited == null) {
                // ── CREATE mode ──
                userManager.createUser(username, password, role);
                showFeedback("User '" + username + "' created successfully!", true);
            } else {
                // ── EDIT mode ──
                userManager.updateUser(userBeingEdited.getId(), username, password, role);
                showFeedback("User '" + username + "' updated successfully!", true);
            }

            // Refresh table and reset form
            loadUsers();
            handleClearForm();

        } catch (Exception e) {
            // Validation errors or DB errors appear here
            showFeedback(e.getMessage(), false);
        }
    }

    // ───────────────────────────────────────────────────────
    // EDIT — fills the form with the selected user's data
    // ───────────────────────────────────────────────────────
    @FXML
    private void handleEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showFeedback("Please select a user to edit.", false);
            return;
        }

        // Switch to EDIT mode
        userBeingEdited = selected;

        // Fill the form with existing data
        usernameField.setText(selected.getUsername());
        passwordField.clear(); // leave blank — means "keep existing password"
        roleComboBox.setValue(selected.getRole());

        // Update UI labels to show we are editing
        formTitle.setText("Edit User");
        saveButton.setText("Update User");
        showFeedback("Editing user: " + selected.getUsername() +
                "\nLeave password blank to keep existing.", true);
    }

    // ───────────────────────────────────────────────────────
    // DELETE — removes selected user after confirmation
    // ───────────────────────────────────────────────────────
    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showFeedback("Please select a user to delete.", false);
            return;
        }

        // Confirmation dialog — prevents accidental deletes
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
                    showFeedback("Could not delete user: " + e.getMessage(), false);
                }
            }
        });
    }

    // ───────────────────────────────────────────────────────
    // CLEAR — resets form back to CREATE mode
    // ───────────────────────────────────────────────────────
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

    // ───────────────────────────────────────────────────────
    // LOGOUT — clears session and goes back to login screen
    // ───────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }

    // ───────────────────────────────────────────────────────
    // HELPER — shows green (success) or red (error) feedback
    // ───────────────────────────────────────────────────────
    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}