package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.util.List;

public class AdminController {

    // ── Root for CSS-Switch ────────────────────────────────
    @FXML private BorderPane rootPane;

    // ── Sidebar Navigation Buttons ─────────────────────────
    @FXML private Button navUsers;
    @FXML private Button navClients;
    @FXML private Button navProfiles;
    @FXML private Button navBoxes;
    @FXML private Button navDocuments;
    @FXML private Button navLogs;
    @FXML private Button navSettings;

    // ── TopBar ─────────────────────────────────────────────
    @FXML private Label  loggedInLabel;
    @FXML private Label  pageTitle;
    @FXML private Button themeToggle;

    // ── Page Header ────────────────────────────────────────
    @FXML private Label contentTitle;
    @FXML private Label contentSubtitle;

    // ── Status Cards ───────────────────────────────────────
    @FXML private Label statTotalUsers;
    @FXML private Label statAdmins;
    @FXML private Label statUsers;

    // ── Table ──────────────────────────────────────────────
    @FXML private TableView<User>          userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private Label  userCountBadge;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // ── Form ───────────────────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label  feedbackLabel;
    @FXML private Button saveButton;

    // ── Internal State ─────────────────────────────────────
    private UserManager userManager;
    private User    userBeingEdited = null;
    private boolean isDarkMode      = false;

    // ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            userManager = new UserManager();
        } catch (Exception e) {
            showFeedback("Cannot connect to database: " + e.getMessage(), false);
            return;
        }

        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            loggedInLabel.setText(current.getUsername());
        }

        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "User"));
        roleComboBox.setValue("User");

        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUsername.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));

        loadUsers();
    }

    // ── Load users ─────────────────────────────────────────
    private void loadUsers() {
        try {
            ObservableList<User> users =
                    FXCollections.observableArrayList(userManager.getAllUsers());
            userTable.setItems(users);

            long adminCount = users.stream().filter(User::isAdmin).count();
            long userCount  = users.size() - adminCount;

            statTotalUsers.setText(String.valueOf(users.size()));
            statAdmins.setText(String.valueOf(adminCount));
            statUsers.setText(String.valueOf(userCount));
            userCountBadge.setText(String.valueOf(users.size()));

        } catch (Exception e) {
            showFeedback("Could not load users: " + e.getMessage(), false);
        }
    }

    // ── Save (create or update) ────────────────────────────
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

    // ── Edit ───────────────────────────────────────────────
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

    // ── Delete ─────────────────────────────────────────────
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

    // ── Clear form ─────────────────────────────────────────
    @FXML
    public void handleClearForm() {
        userBeingEdited = null;
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue("User");
        formTitle.setText("Create New User");
        saveButton.setText("Create User");
        feedbackLabel.setText("");
    }

    // ── Theme toggle ───────────────────────────────────────
    @FXML
    private void handleThemeToggle() {
        isDarkMode = !isDarkMode;
        String css = getClass().getResource(
                isDarkMode ? "/dk/easv/scanova/styles/dark.css"
                        : "/dk/easv/scanova/styles/light.css"
        ).toExternalForm();
        rootPane.getStylesheets().clear();
        rootPane.getStylesheets().add(css);
        themeToggle.setText(isDarkMode ? "☀  Light mode" : "☾  Dark mode");
    }

    // ── Logout ─────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }

    // ── Navigation ─────────────────────────────────────────
    @FXML private void handleNavDashboard()  { setActivePage("Dashboard", "Overview of the system");         }
    @FXML private void handleNavUsers()      { setActivePage("Users",     "Manage user accounts"); loadUsers(); }
    @FXML private void handleNavClients()    { setActivePage("Clients",   "Manage client organisations");    }
    @FXML private void handleNavProfiles()   { setActivePage("Profiles",  "Manage scanning profiles");       }
    @FXML private void handleNavBoxes()      { setActivePage("Boxes",     "View scanned boxes");              }
    @FXML private void handleNavDocuments()  { setActivePage("Documents", "View scanned documents");         }
    @FXML private void handleNavLogs()       { setActivePage("Logs",      "Audit trail of all actions");     }
    @FXML private void handleNavSettings()   { setActivePage("Settings",  "System configuration");           }

    private void setActivePage(String title, String subtitle) {
        pageTitle.setText(title);
        contentTitle.setText(title);
        contentSubtitle.setText(subtitle);

        List<Button> navItems = List.of(
                navUsers, navClients, navProfiles,
                navBoxes, navDocuments, navLogs, navSettings);
        navItems.forEach(b -> {
            b.getStyleClass().remove("nav-item-active");
            if (!b.getStyleClass().contains("nav-item"))
                b.getStyleClass().add("nav-item");
        });

        switch (title) {
            case "Users"     -> setActive(navUsers);
            case "Clients"   -> setActive(navClients);
            case "Profiles"  -> setActive(navProfiles);
            case "Boxes"     -> setActive(navBoxes);
            case "Documents" -> setActive(navDocuments);
            case "Logs"      -> setActive(navLogs);
            case "Settings"  -> setActive(navSettings);
        }
    }

    private void setActive(Button btn) {
        btn.getStyleClass().add("nav-item-active");
    }

    // ── Feedback helper ────────────────────────────────────
    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}