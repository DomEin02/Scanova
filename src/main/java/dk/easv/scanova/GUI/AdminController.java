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
import dk.easv.scanova.BLL.ProfileManager;
import dk.easv.scanova.Model.Profile;
import javafx.scene.layout.HBox;

import java.util.List;

public class AdminController {

    //Root for CSS Switch
    @FXML private BorderPane rootPane;

    //SideBar Nav
    @FXML private Button navUsers;
    @FXML private Button navClients;
    @FXML private Button navProfiles;
    @FXML private Button navBoxes;
    @FXML private Button navDocuments;
    @FXML private Button navLogs;
    @FXML private Button navSettings;

    //TopBar
    @FXML private Label loggedInLabel;
    @FXML private Label pageTitle;
    @FXML private Button themeToggle;

    //PageHeader
    @FXML private Label contentTitle;
    @FXML private Label contentSubtitle;

    //Status
    @FXML private Label statTotalUsers;
    @FXML private Label statAdmins;
    @FXML private Label statUsers;
    @FXML private Label userCountBadge;

    //UserTable
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colProfiles;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    //UserForm
    @FXML private Label formTitle;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label feedbackLabel;
    @FXML private Button saveButton;

    //ProfileAssign.
    @FXML private ComboBox<String> profileComboBox;
    @FXML private Label assignedProfilesLabel;

    //ProfileTable
    @FXML private TableView<Profile> profileTable;
    @FXML private TableColumn<Profile, Integer> colProfileId;
    @FXML private TableColumn<Profile, String> colProfileName;
    @FXML private TableColumn<Profile, Float> colProfileRotation;
    @FXML private TableColumn<Profile, Float> colProfileBrightness;
    @FXML private TableColumn<Profile, Integer> colProfileClientId;
    @FXML private Label profileCountBadge;
    @FXML private Button profileEditButton;
    @FXML private Button profileDeleteButton;

    //ProfileForm
    @FXML private Label profileFormTitle;
    @FXML private TextField profileNameField;
    @FXML private Slider profileRotationSlider;
    @FXML private Label profileRotationValueLabel;
    @FXML private Slider profileBrightnessSlider;
    @FXML private Label profileBrightnessValueLabel;
    @FXML private ComboBox<String> profileClientComboBox;
    @FXML private Label profileFeedbackLabel;
    @FXML private Button profileSaveButton;
    // Show/Hide sections
    @FXML private HBox usersSection;
    @FXML private HBox profilesSection;

    //InternalState
    private UserManager userManager;
    private ProfileManager profileManager;
    private final ProfileDAO profileDAO = new ProfileDAO();
    private Profile profileBeingEdited = null;
    private User userBeingEdited = null;
    private boolean isDarkMode = false;

    @FXML
    public void initialize() {
        // Try/catch - error if DB fails
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

        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null) {
            loggedInLabel.setText(current.getUsername());
        }

        roleComboBox.setItems(FXCollections.observableArrayList("Admin", "User"));
        roleComboBox.setValue("User");

        // WireTableColumn
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colUsername.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));

        // ProfilesColumn — shows all assigned profiles per user
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

        //WireProfileTableColumn
        if (profileTable != null) {
            colProfileId.setCellValueFactory(d ->
                    new SimpleIntegerProperty(d.getValue().getId()).asObject());

            colProfileName.setCellValueFactory(d ->
                    new SimpleStringProperty(d.getValue().getName()));

            colProfileRotation.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleFloatProperty(
                            d.getValue().getRotation()).asObject());

            colProfileBrightness.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleFloatProperty(
                            d.getValue().getBrightness()).asObject());

            colProfileClientId.setCellValueFactory(d ->
                    new SimpleIntegerProperty(d.getValue().getClientId()).asObject());

            // Actions column with Edit + Delete buttons per row
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

        //ProfileSliders
        if (profileRotationSlider != null) {
            profileRotationSlider.setMin(-180);
            profileRotationSlider.setMax(180);
            profileRotationSlider.setValue(0);
            profileRotationSlider.setMajorTickUnit(45);
            profileRotationSlider.setSnapToTicks(false);

            //Update the label as slider moves
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

            //Update label as slider moves
            profileBrightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double rounded = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                profileBrightnessValueLabel.setText(String.valueOf(rounded));
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

    //LoadUser
    private void loadUsers() {
        try {
            ObservableList<User> users =
                    FXCollections.observableArrayList(userManager.getAllUsers());
            userTable.setItems(users);

            long adminCount = users.stream().filter(User::isAdmin).count();
            long userCount = users.size() - adminCount;

            statTotalUsers.setText(String.valueOf(users.size()));
            statAdmins.setText(String.valueOf(adminCount));
            statUsers.setText(String.valueOf(userCount));
            userCountBadge.setText(String.valueOf(users.size()));

        } catch (Exception e) {
            showFeedback("Could not load users: " + e.getMessage(), false);
        }
    }

    //LoadProfileInCompbox
    private void loadProfiles() {
        try {
            if (profileComboBox == null) return;
            profileComboBox.getItems().clear();
            profileComboBox.getItems().addAll(profileDAO.getAllProfileNames());
        } catch (Exception e) {
            showFeedback("Could not load profiles: " + e.getMessage(), false);
        }
    }

    //Load all profiles into the profile table
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

    //ProfilesInLabel
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

    //Save(create/update)
    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role     = roleComboBox.getValue();

        try {
            if (userBeingEdited == null) {
                userManager.createUser(username, password, role);
                loadUsers();
                handleClearForm();
                showFeedback("User '" + username + "' created successfully!", true);
            } else {
                userManager.updateUser(
                        userBeingEdited.getId(), username, password, role);
                loadUsers();
                handleClearForm();
                showFeedback("User '" + username + "' updated successfully!", true);
            }
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    //EditSelectedUser
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

    //DeleteSelectedUser
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

    //AssingProfileToSelectedUser
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
    // Save profile — create or update depending on mode
    @FXML
    private void handleSaveProfile() {
        String name       = profileNameField.getText().trim();
        float  rotation   = (float) profileRotationSlider.getValue();
        float  brightness = (float) profileBrightnessSlider.getValue();

        // Get clientId from the dropdown
        // For now we use 1 as default until teammate finishes client dropdown
        int clientId = 1;

        try {
            if (profileBeingEdited == null) {
                // CREATE mode
                profileManager.createProfile(name, rotation, brightness, clientId);
                showProfileFeedback("Profile '" + name + "' created!", true);
            } else {
                // EDIT mode
                profileManager.updateProfile(
                        profileBeingEdited.getId(), name, rotation, brightness, clientId);
                showProfileFeedback("Profile '" + name + "' updated!", true);
            }

            loadProfileTable();
            handleClearProfileForm();

        } catch (Exception e) {
            showProfileFeedback(e.getMessage(), false);
        }
    }

    // Edit selected profile — fills form with existing data
    @FXML
    private void handleEditProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showProfileFeedback("Please select a profile to edit.", false);
            return;
        }

        // Switch to EDIT mode
        profileBeingEdited = selected;

        // Fill the form with existing data
        profileNameField.setText(selected.getName());
        profileRotationSlider.setValue(selected.getRotation());
        profileBrightnessSlider.setValue(selected.getBrightness());

        // Update form title and button
        profileFormTitle.setText("Edit profile");
        profileSaveButton.setText("Update profile");

        showProfileFeedback("Editing: " + selected.getName(), true);
    }

    @FXML
    private void handleDeleteProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showProfileFeedback("Please select a profile to delete.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Profile");
        confirm.setHeaderText("Delete '" + selected.getName() + "'?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    profileManager.deleteProfile(selected.getId());
                    showProfileFeedback("Profile deleted.", true);
                    loadProfileTable();
                    handleClearProfileForm();
                } catch (Exception e) {
                    showProfileFeedback("Could not delete: " + e.getMessage(), false);
                }
            }
        });
    }

    // Clear the profile form — reset to create mode
    @FXML
    public void handleClearProfileForm() {
        profileBeingEdited = null;

        if (profileNameField != null)
            profileNameField.clear();

        if (profileRotationSlider != null)
            profileRotationSlider.setValue(0);

        if (profileBrightnessSlider != null)
            profileBrightnessSlider.setValue(1.0);

        if (profileFormTitle != null)
            profileFormTitle.setText("Create new profile");

        if (profileSaveButton != null)
            profileSaveButton.setText("Create profile");

        if (profileFeedbackLabel != null)
            profileFeedbackLabel.setText("");
    }

    // ClearForm
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

    //Light/Dark ThemeToggle
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

    //LogOut
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }

    //Nav
    @FXML private void handleNavDashboard()  { setActivePage("Dashboard", "Overview of the system"); }
    @FXML private void handleNavUsers() {
        setActivePage("Users", "Manage user accounts");
        usersSection.setVisible(true);
        usersSection.setManaged(true);
        profilesSection.setVisible(false);
        profilesSection.setManaged(false);
        loadUsers();
    }
    @FXML private void handleNavProfiles() {
        setActivePage("Profiles", "Manage scanning profiles");
        usersSection.setVisible(false);
        usersSection.setManaged(false);
        profilesSection.setVisible(true);
        profilesSection.setManaged(true);
        loadProfileTable();
    }
    @FXML private void handleNavClients()    { setActivePage("Clients",   "Manage client organisations"); }
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

    //Feedback
    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }

    // Profile-specific feedback
    private void showProfileFeedback(String message, boolean success) {
        if (profileFeedbackLabel == null) return;
        profileFeedbackLabel.setText(message);
        profileFeedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}