package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.LogManager;
import dk.easv.scanova.BLL.ProfileManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.DAL.ProfileDAO;
import dk.easv.scanova.Model.Profile;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleFloatProperty;
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

    // ── Show/Hide sections ────────────────────────────────────────────────────
    @FXML private HBox usersSection;
    @FXML private HBox profilesSection;

    // ── Dynamic content area (teammate's architecture) ────────────────────────
    @FXML private ScrollPane contentArea;

    // ── Profile table ─────────────────────────────────────────────────────────
    @FXML private TableView<Profile>            profileTable;
    @FXML private TableColumn<Profile, Integer> colProfileId;
    @FXML private TableColumn<Profile, String>  colProfileName;
    @FXML private TableColumn<Profile, Float>   colProfileRotation;
    @FXML private TableColumn<Profile, Float>   colProfileBrightness;
    @FXML private TableColumn<Profile, Integer> colProfileClientId;
    @FXML private Label                         profileCountBadge;
    @FXML private Button                        profileEditButton;
    @FXML private Button                        profileDeleteButton;

    // ── Profile form ──────────────────────────────────────────────────────────
    @FXML private Label            profileFormTitle;
    @FXML private TextField        profileNameField;
    @FXML private Slider           profileRotationSlider;
    @FXML private Label            profileRotationValueLabel;
    @FXML private Slider           profileBrightnessSlider;
    @FXML private Label            profileBrightnessValueLabel;
    @FXML private ComboBox<String> profileClientComboBox;
    @FXML private Label            profileFeedbackLabel;
    @FXML private Button           profileSaveButton;

    // ── Internal state ────────────────────────────────────────────────────────
    private UserManager    userManager;
    private ProfileManager profileManager;
    private final ProfileDAO profileDAO  = new ProfileDAO();
    private final LogManager logManager  = new LogManager();
    private Profile profileBeingEdited   = null;
    private User    userBeingEdited      = null;
    private boolean isDarkMode           = false;
    private ObservableList<User> allUsers = FXCollections.observableArrayList();
    private Node defaultContent;

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            userManager = new UserManager();
        } catch (Exception e) {
            showFeedback("Cannot connect to database: " + e.getMessage(), false);
            return;
        }
        try {
            profileManager = new ProfileManager();
        } catch (Exception e) {
            showFeedback("Cannot connect: " + e.getMessage(), false);
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

        // Profiles column
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

        // Status column
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

        // Wire profile table columns
        if (profileTable != null) {
            colProfileId.setCellValueFactory(d ->
                    new SimpleIntegerProperty(d.getValue().getId()).asObject());
            colProfileName.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().getName()));
            colProfileRotation.setCellValueFactory(d ->
                    new SimpleFloatProperty(d.getValue().getRotation()).asObject());
            colProfileBrightness.setCellValueFactory(d ->
                    new SimpleFloatProperty(d.getValue().getBrightness()).asObject());
            colProfileClientId.setCellValueFactory(d ->
                    new SimpleIntegerProperty(d.getValue().getClientId()).asObject());

            // Inline Edit + Delete buttons per row
            TableColumn<Profile, Void> actionsCol = new TableColumn<>("Actions");
            actionsCol.setPrefWidth(160);
            actionsCol.setCellFactory(col -> new TableCell<>() {
                private final Button editBtn   = new Button("Edit");
                private final Button deleteBtn = new Button("Delete");
                private final HBox   box       = new HBox(6, editBtn, deleteBtn);
                {
                    editBtn.getStyleClass().add("btn-secondary");
                    editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10;");
                    deleteBtn.getStyleClass().add("btn-danger");
                    deleteBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10;");
                    editBtn.setOnAction(e -> {
                        Profile p = getTableView().getItems().get(getIndex());
                        profileBeingEdited = p;
                        profileNameField.setText(p.getName());
                        profileRotationSlider.setValue(p.getRotation());
                        profileBrightnessSlider.setValue(p.getBrightness());
                        profileFormTitle.setText("Edit profile");
                        profileSaveButton.setText("Update profile");
                        showProfileFeedback("Editing: " + p.getName(), true);
                    });
                    deleteBtn.setOnAction(e -> {
                        Profile p = getTableView().getItems().get(getIndex());
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Delete Profile");
                        confirm.setHeaderText("Delete '" + p.getName() + "'?");
                        confirm.setContentText("This cannot be undone.");
                        confirm.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                try {
                                    profileManager.deleteProfile(p.getId());
                                    showProfileFeedback("Profile deleted.", true);
                                    loadProfileTable();
                                    handleClearProfileForm();
                                } catch (Exception ex) {
                                    showProfileFeedback(
                                            "Could not delete: " + ex.getMessage(), false);
                                }
                            }
                        });
                    });
                }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
            profileTable.getColumns().add(actionsCol);
            loadProfileTable();
        }

        // Profile sliders
        if (profileRotationSlider != null) {
            profileRotationSlider.setMin(-180);
            profileRotationSlider.setMax(180);
            profileRotationSlider.setValue(0);
            profileRotationSlider.setMajorTickUnit(45);
            profileRotationSlider.setSnapToTicks(false);
            profileRotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int snapped = ((int) Math.round(newVal.doubleValue() / 5)) * 5;
                profileRotationSlider.setValue(snapped);
                profileRotationValueLabel.setText(snapped + "°");
            });
        }
        if (profileBrightnessSlider != null) {
            profileBrightnessSlider.setMin(0.1);
            profileBrightnessSlider.setMax(3.0);
            profileBrightnessSlider.setValue(1.0);
            profileBrightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double rounded = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                profileBrightnessValueLabel.setText(String.valueOf(rounded));
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

    @FXML
    private void handleClearSearch() {
        if (searchField != null) searchField.clear();
        if (roleFilterComboBox != null) roleFilterComboBox.setValue("All");
        userTable.setItems(allUsers);
        userCountBadge.setText(String.valueOf(allUsers.size()));
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

    // ── Load profile table ────────────────────────────────────────────────────
    private void loadProfileTable() {
        try {
            if (profileTable == null) return;
            ObservableList<Profile> profiles =
                    FXCollections.observableArrayList(profileManager.getAllProfiles());
            profileTable.setItems(profiles);
            if (profileCountBadge != null)
                profileCountBadge.setText(String.valueOf(profiles.size()));
        } catch (Exception e) {
            if (profileFeedbackLabel != null) {
                profileFeedbackLabel.setText("Could not load profiles: " + e.getMessage());
                profileFeedbackLabel.setTextFill(Color.web("#E53E3E"));
            }
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

    // ── Save user ─────────────────────────────────────────────────────────────
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
                loadUsers(); handleClearForm();
                showFeedback("User '" + username + "' created successfully!", true);
            } else {
                userManager.updateUser(userBeingEdited.getId(), username, password, role);
                loadUsers(); handleClearForm();
                showFeedback("User '" + username + "' updated successfully!", true);
            }
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    @FXML
    private void handleEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a user to edit.", false); return; }
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
        if (selected == null) { showFeedback("Please select a user to deactivate.", false); return; }
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
                    loadUsers(); handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not deactivate: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleReactivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a user to reactivate.", false); return; }
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

    @FXML
    private void handleAssignProfile() {
        User selected          = userTable.getSelectionModel().getSelectedItem();
        String selectedProfile = profileComboBox.getValue();
        if (selected == null) { showFeedback("Please select a user from the table first.", false); return; }
        if (selectedProfile == null) { showFeedback("Please select a profile from the dropdown.", false); return; }
        try {
            int profileId = profileDAO.getProfileIdByName(selectedProfile);
            if (profileId == -1) { showFeedback("Profile not found in database.", false); return; }
            profileDAO.assignProfileToUser(selected.getId(), profileId);
            showFeedback("Profile '" + selectedProfile + "' assigned to '"
                    + selected.getUsername() + "'!", true);
            loadUsers();
            showAssignedProfiles(selected);
        } catch (Exception e) {
            showFeedback("Could not assign profile: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveProfile() {
        String name       = profileNameField.getText().trim();
        float  rotation   = (float) profileRotationSlider.getValue();
        float  brightness = (float) profileBrightnessSlider.getValue();
        int    clientId   = 1; // TODO: wire profileClientComboBox
        try {
            if (profileBeingEdited == null) {
                profileManager.createProfile(name, rotation, brightness, clientId);
                loadProfileTable(); handleClearProfileForm();
                showProfileFeedback("Profile '" + name + "' created!", true);
            } else {
                profileManager.updateProfile(
                        profileBeingEdited.getId(), name, rotation, brightness, clientId);
                loadProfileTable(); handleClearProfileForm();
                showProfileFeedback("Profile '" + name + "' updated!", true);
            }
        } catch (Exception e) {
            showProfileFeedback(e.getMessage(), false);
        }
    }

    @FXML
    private void handleEditProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showProfileFeedback("Please select a profile to edit.", false); return; }
        profileBeingEdited = selected;
        profileNameField.setText(selected.getName());
        profileRotationSlider.setValue(selected.getRotation());
        profileBrightnessSlider.setValue(selected.getBrightness());
        profileFormTitle.setText("Edit profile");
        profileSaveButton.setText("Update profile");
        showProfileFeedback("Editing: " + selected.getName(), true);
    }

    @FXML
    private void handleDeleteProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showProfileFeedback("Please select a profile to delete.", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Profile");
        confirm.setHeaderText("Delete '" + selected.getName() + "'?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    profileManager.deleteProfile(selected.getId());
                    showProfileFeedback("Profile deleted.", true);
                    loadProfileTable(); handleClearProfileForm();
                } catch (Exception e) {
                    showProfileFeedback("Could not delete: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    public void handleClearProfileForm() {
        profileBeingEdited = null;
        if (profileNameField != null)        profileNameField.clear();
        if (profileRotationSlider != null)   profileRotationSlider.setValue(0);
        if (profileBrightnessSlider != null) profileBrightnessSlider.setValue(1.0);
        if (profileFormTitle != null)        profileFormTitle.setText("Create new profile");
        if (profileSaveButton != null)       profileSaveButton.setText("Create profile");
        if (profileFeedbackLabel != null)    profileFeedbackLabel.setText("");
    }

    @FXML
    public void handleClearForm() {
        userBeingEdited = null;
        usernameField.clear(); passwordField.clear();
        roleComboBox.setValue("User");
        formTitle.setText("Create New User");
        saveButton.setText("Create User");
        feedbackLabel.setText("");
        if (assignedProfilesLabel != null) assignedProfilesLabel.setText("");
    }

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
        if (usersSection != null)    { usersSection.setVisible(true);     usersSection.setManaged(true); }
        if (profilesSection != null) { profilesSection.setVisible(false); profilesSection.setManaged(false); }
        loadUsers();
    }
    @FXML private void handleNavProfiles() {
        setActivePage("Profiles", "Manage scanning profiles");
        showDefaultContent();
        if (usersSection != null)    { usersSection.setVisible(false);    usersSection.setManaged(false); }
        if (profilesSection != null) { profilesSection.setVisible(true);  profilesSection.setManaged(true); }
        loadProfileTable();
    }
    @FXML private void handleNavClients()   { setActivePage("Clients",   "Manage client organisations"); loadView("/dk/easv/scanova/clientView.fxml"); }
    @FXML private void handleNavLogs()      { setActivePage("Logs",      "Audit trail of all actions");  loadView("/dk/easv/scanova/logView.fxml"); }
    @FXML private void handleNavBoxes()     { setActivePage("Boxes",     "View scanned boxes"); }
    @FXML private void handleNavDocuments() { setActivePage("Documents", "View scanned documents"); }
    @FXML private void handleNavSettings()  { setActivePage("Settings",  "System configuration"); }

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

    private void showDefaultContent() {
        if (contentArea != null && defaultContent != null)
            contentArea.setContent(defaultContent);
    }

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

    private void setActive(Button btn) { btn.getStyleClass().add("nav-item-active"); }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }

    private void showProfileFeedback(String message, boolean success) {
        if (profileFeedbackLabel == null) return;
        profileFeedbackLabel.setText(message);
        profileFeedbackLabel.setTextFill(success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}