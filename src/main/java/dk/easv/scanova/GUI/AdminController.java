package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.LogManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.DAL.ProfileDAO;
import dk.easv.scanova.BE.User;
import dk.easv.scanova.SceneManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

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
    @FXML private Label  loggedInRole;
    @FXML private Label  loggedInTime;
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

    // ── Search and filter ─────────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilterComboBox;

    // ── User table ────────────────────────────────────────────────────────────
    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colUsername;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colProfiles;
    @FXML private TableColumn<User, String>  colStatus;
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

    // ── Dynamic content area ──────────────────────────────────────────────────
    @FXML private ScrollPane contentArea;

    // ── Internal state ────────────────────────────────────────────────────────
    private UserManager              userManager;
    private final ProfileDAO         profileDAO  = new ProfileDAO();
    private final LogManager         logManager  = new LogManager();
    private User                     userBeingEdited = null;
    private boolean                  isDarkMode      = false;
    private ObservableList<User>     allUsers        = FXCollections.observableArrayList();
    private Node                     defaultContent;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            userManager = new UserManager();
        } catch (Exception e) {
            showFeedback("Cannot connect to database: " + e.getMessage(), false);
            return;
        }

        // Show logged-in user info
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            loggedInLabel.setText(current.getUsername());
            if (loggedInRole != null)
                loggedInRole.setText(current.isAdmin() ? "Administrator" : "Scanner User");
            if (loggedInTime != null)
                loggedInTime.setText("Since: " + SessionManager.getInstance().getLoginTime());
        }

        // Role dropdown for form
        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "User"));
        roleComboBox.setValue("User");

        // Role filter dropdown
        if (roleFilterComboBox != null) {
            roleFilterComboBox.setItems(
                    FXCollections.observableArrayList("All", "Admin", "User"));
            roleFilterComboBox.setValue("All");
        }

        // Wire user table columns
        if (colId != null)
            colId.setCellValueFactory(d ->
                    new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUsername.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));

        // Profiles column — shows assigned profiles per user
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

        // Status column — shows Active/Inactive
        if (colStatus != null) {
            colStatus.setCellValueFactory(d ->
                    new SimpleStringProperty(
                            d.getValue().isActive() ? "Active" : "Inactive"));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setStyle("");
                    } else if (item.equals("Active")) {
                        setText("Active");
                        setStyle("-fx-text-fill: #2ECC9A; -fx-font-weight: bold;");
                    } else {
                        setText("Inactive");
                        setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
                    }
                }
            });
        }

        // When user selected — show assigned profiles
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) showAssignedProfiles(newVal);
                    else if (assignedProfilesLabel != null)
                        assignedProfilesLabel.setText("");
                });

        // Search and filter listeners
        if (searchField != null)
            searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        if (roleFilterComboBox != null)
            roleFilterComboBox.valueProperty().addListener((obs, old, val) -> applyFilters());

        loadUsers();
        loadProfiles();

        // Save default content for nav restore
        Platform.runLater(() -> {
            if (contentArea != null) defaultContent = contentArea.getContent();
        });
    }

    // ── Load users ────────────────────────────────────────────────────────────
    private void loadUsers() {
        try {
            try {
                allUsers = FXCollections.observableArrayList(
                        userManager.getAllUsersIncludingInactive());
            } catch (Exception ex) {
                allUsers = FXCollections.observableArrayList(userManager.getAllUsers());
            }
            userTable.setItems(allUsers);

            long adminCount = allUsers.stream().filter(User::isAdmin).count();
            long userCount  = allUsers.stream().filter(u -> !u.isAdmin()).count();

            statTotalUsers.setText(String.valueOf(allUsers.size()));
            statAdmins.setText(String.valueOf(adminCount));
            statUsers.setText(String.valueOf(userCount));
            userCountBadge.setText(String.valueOf(allUsers.size()));

        } catch (Exception e) {
            showFeedback("Could not load users: " + e.getMessage(), false);
        }
    }

    // ── Apply search + role filter ────────────────────────────────────────────
    private void applyFilters() {
        if (searchField == null || roleFilterComboBox == null) return;
        String search = searchField.getText() == null
                ? "" : searchField.getText().toLowerCase().trim();
        String role = roleFilterComboBox.getValue();

        ObservableList<User> filtered = allUsers.stream()
                .filter(u -> {
                    boolean matchesSearch = search.isEmpty()
                            || u.getUsername().toLowerCase().contains(search);
                    boolean matchesRole = role == null || role.equals("All")
                            || u.getRole().equalsIgnoreCase(role);
                    return matchesSearch && matchesRole;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        userTable.setItems(filtered);
        userCountBadge.setText(String.valueOf(filtered.size()));
    }

    // ── Clear search ──────────────────────────────────────────────────────────
    @FXML
    private void handleClearSearch() {
        if (searchField != null) searchField.clear();
        if (roleFilterComboBox != null) roleFilterComboBox.setValue("All");
        userTable.setItems(allUsers);
        userCountBadge.setText(String.valueOf(allUsers.size()));
    }

    // ── Load profiles into assignment ComboBox ────────────────────────────────
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

    // ── Save user (create or update) ──────────────────────────────────────────
    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role     = roleComboBox.getValue();
        try {
            if (userBeingEdited == null) {
                userManager.createUser(username, password, role);
                try { logManager.log("USER_CREATED",
                        SessionManager.getInstance().getCurrentUser().getId(),
                        "User created: " + username);
                } catch (Exception ignored) {}
                loadUsers();
                handleClearForm();
                showFeedback("User '" + username + "' created successfully!", true);
            } else {
                userManager.updateUser(userBeingEdited.getId(), username, password, role);
                loadUsers();
                handleClearForm();
                showFeedback("User '" + username + "' updated successfully!", true);
            }
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

    // ── Deactivate selected user ──────────────────────────────────────────────
    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to deactivate.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate User");
        confirm.setHeaderText("Deactivate '" + selected.getUsername() + "'?");
        confirm.setContentText("The user will be deactivated and cannot log in.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userManager.deleteUser(selected.getId());
                    try { logManager.log("USER_DEACTIVATED",
                            SessionManager.getInstance().getCurrentUser().getId(),
                            "User deactivated: " + selected.getUsername());
                    } catch (Exception ignored) {}
                    showFeedback("User deactivated.", true);
                    loadUsers();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not deactivate: " + e.getMessage(), false);
                }
            }
        });
    }

    // ── Reactivate selected user ──────────────────────────────────────────────
    @FXML
    private void handleReactivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to reactivate.", false);
            return;
        }
        try {
            userManager.reactivateUser(selected.getId());
            try { logManager.log("USER_REACTIVATED",
                    SessionManager.getInstance().getCurrentUser().getId(),
                    "User reactivated: " + selected.getUsername());
            } catch (Exception ignored) {}
            showFeedback("User '" + selected.getUsername() + "' reactivated!", true);
            loadUsers();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
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

    // ── Clear user form ───────────────────────────────────────────────────────
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
    @FXML private void handleNavDashboard() {
        setActivePage("Dashboard", "Overview of the system");
        showDefaultContent();
    }

    @FXML private void handleNavUsers() {
        setActivePage("Users", "Manage user accounts");
        showDefaultContent();
        loadUsers();
    }

    @FXML private void handleNavProfiles() {
        setActivePage("Profiles", "Manage scanning profiles");
        loadView("/dk/easv/scanova/profileView.fxml");
    }

    @FXML private void handleNavClients() {
        setActivePage("Clients", "Manage client organisations");
        loadView("/dk/easv/scanova/clientView.fxml");
    }

    @FXML private void handleNavLogs() {
        setActivePage("Logs", "Audit trail of all actions");
        loadView("/dk/easv/scanova/logView.fxml");
    }

    @FXML private void handleNavBoxes() {
        setActivePage("Boxes", "View scanned boxes");
    }

    @FXML private void handleNavDocuments() {
        setActivePage("Documents", "View scanned documents");
    }

    @FXML private void handleNavSettings() {
        setActivePage("Settings", "System configuration");
    }

    // ── Load external FXML into content area ──────────────────────────────────
    private void loadView(String fxmlPath) {
        if (contentArea == null) return;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.setContent(view);
        } catch (Exception e) {
            showFeedback("Could not load view: " + e.getMessage(), false);
        }
    }

    // ── Restore default content ───────────────────────────────────────────────
    private void showDefaultContent() {
        if (contentArea != null && defaultContent != null)
            contentArea.setContent(defaultContent);
    }

    // ── Set active page ───────────────────────────────────────────────────────
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