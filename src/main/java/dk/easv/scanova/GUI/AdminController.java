package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.DAL.ProfileDAO;
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

    // ── Root for CSS switch ───────────────────────────────────────────────────
    @FXML private BorderPane rootPane;

    // ── Sidebar navigation ────────────────────────────────────────────────────
    @FXML private Button navUsers;
    @FXML private Button navClients;
    @FXML private Button navProfiles;
    @FXML private Button navBoxes;
    @FXML private Button navDocuments;
    @FXML private Button navLogs;
    @FXML private Button navSettings;

    // ── TopBar ────────────────────────────────────────────────────────────────
    @FXML private Label  loggedInLabel;
    @FXML private Label  pageTitle;
    @FXML private Button themeToggle;

    // ── Page header ───────────────────────────────────────────────────────────
    @FXML private Label contentTitle;
    @FXML private Label contentSubtitle;

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML private Label statTotalUsers;
    @FXML private Label statAdmins;
    @FXML private Label statUsers;
    @FXML private Label userCountBadge;

    // ── User table ────────────────────────────────────────────────────────────
    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colProfiles;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    // ── User form ─────────────────────────────────────────────────────────────
    @FXML private Label            formTitle;
    @FXML private TextField        usernameField;
    @FXML private PasswordField    passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label            feedbackLabel;
    @FXML private Button           saveButton;

    // ── Profile assignment ────────────────────────────────────────────────────
    @FXML private ComboBox<String> profileComboBox;
    @FXML private Label            assignedProfilesLabel;

    // ── Internal state ────────────────────────────────────────────────────────
    private UserManager userManager;
    private final ProfileDAO profileDAO = new ProfileDAO();
    private User    userBeingEdited = null;
    private boolean isDarkMode      = false;

    @FXML
    public void initialize() {
        // Try/catch from version 2 — shows error if DB fails
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

        // Wire table columns
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUsername.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));

        // Profiles column — shows all assigned profiles per user
        if (colProfiles != null) {
            colProfiles.setCellValueFactory(data -> {
                try {
                    List<String> profiles =
                            profileDAO.getProfilesForUser(data.getValue().getId());
                    return new SimpleStringProperty(
                            profiles.isEmpty() ? "None" : String.join(", ", profiles));
                } catch (Exception e) {
                    return new SimpleStringProperty("Error");
                }
            });
        }

        // When user selected — show assigned profiles in label
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) showAssignedProfiles(newVal);
                    else if (assignedProfilesLabel != null)
                        assignedProfilesLabel.setText("");
                });

        loadUsers();
        loadProfiles();
    }

    // ── Load users ────────────────────────────────────────────────────────────
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

    // ── Load profiles into ComboBox ───────────────────────────────────────────
    private void loadProfiles() {
        try {
            if (profileComboBox == null) return;
            profileComboBox.getItems().clear();
            profileComboBox.getItems().addAll(profileDAO.getAllProfileNames());
        } catch (Exception e) {
            showFeedback("Could not load profiles: " + e.getMessage(), false);
        }
    }

    // ── Show assigned profiles in label ───────────────────────────────────────
    private void showAssignedProfiles(User user) {
        try {
            if (assignedProfilesLabel == null) return;
            List<String> profiles = profileDAO.getProfilesForUser(user.getId());
            assignedProfilesLabel.setText(profiles.isEmpty()
                    ? "No profiles assigned yet"
                    : "Assigned: " + String.join(", ", profiles));
        } catch (Exception e) {
            assignedProfilesLabel.setText("Could not load assigned profiles");
        }
    }

    // ── Save (create or update) ───────────────────────────────────────────────
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
                userManager.updateUser(
                        userBeingEdited.getId(), username, password, role);
                showFeedback("User '" + username + "' updated successfully!", true);
            }
            loadUsers();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    // ── Edit selected user ────────────────────────────────────────────────────
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

    // ── Delete selected user ──────────────────────────────────────────────────
    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to delete.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete '" + selected.getUsername() + "'?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userManager.deleteUser(selected.getId());
                    showFeedback("User deleted.", true);
                    loadUsers();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not delete: " + e.getMessage(), false);
                }
            }
        });
    }

    // ── Assign profile to selected user ───────────────────────────────────────
    @FXML
    private void handleAssignProfile() {
        User selected          = userTable.getSelectionModel().getSelectedItem();
        String selectedProfile = profileComboBox.getValue();

        if (selected == null) {
            showFeedback("Please select a user from the table first.", false);
            return;
        }
        if (selectedProfile == null) {
            showFeedback("Please select a profile from the dropdown.", false);
            return;
        }

        try {
            int profileId = profileDAO.getProfileIdByName(selectedProfile);
            if (profileId == -1) {
                showFeedback("Profile not found in database.", false);
                return;
            }
            profileDAO.assignProfileToUser(selected.getId(), profileId);
            showFeedback("Profile '" + selectedProfile + "' assigned to '"
                    + selected.getUsername() + "'!", true);
            loadUsers();
            showAssignedProfiles(selected);
        } catch (Exception e) {
            showFeedback("Could not assign profile: " + e.getMessage(), false);
        }
    }

    // ── Clear form ────────────────────────────────────────────────────────────
    @FXML
    public void handleClearForm() {
        userBeingEdited = null;
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue("User");
        formTitle.setText("Create New User");
        saveButton.setText("Create User");
        feedbackLabel.setText("");
        if (assignedProfilesLabel != null) assignedProfilesLabel.setText("");
    }

    // ── Theme toggle ──────────────────────────────────────────────────────────
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

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    @FXML private void handleNavDashboard()  { setActivePage("Dashboard", "Overview of the system"); }
    @FXML private void handleNavUsers()      { setActivePage("Users",     "Manage user accounts"); loadUsers(); }
    @FXML private void handleNavClients()    { setActivePage("Clients",   "Manage client organisations"); }
    @FXML private void handleNavProfiles()   { setActivePage("Profiles",  "Manage scanning profiles"); }
    @FXML private void handleNavBoxes()      { setActivePage("Boxes",     "View scanned boxes"); }
    @FXML private void handleNavDocuments()  { setActivePage("Documents", "View scanned documents"); }
    @FXML private void handleNavLogs()       { setActivePage("Logs",      "Audit trail of all actions"); }
    @FXML private void handleNavSettings()   { setActivePage("Settings",  "System configuration"); }

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

    // ── Feedback helper ───────────────────────────────────────────────────────
    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}