package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.LogManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BLL.UserManager;
import dk.easv.scanova.DAL.ProfileDAO;
import dk.easv.scanova.Model.User;
import dk.easv.scanova.SceneManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

public class AdminController {

    @FXML private BorderPane rootPane;

    @FXML private Button navUsers;
    @FXML private Button navClients;
    @FXML private Button navArchives;
    @FXML private Button navProfiles;
    @FXML private Button navBoxes;
    @FXML private Button navDocuments;
    @FXML private Button navLogs;
    @FXML private Button navSettings;

    @FXML private Label  loggedInLabel;
    @FXML private Label  loggedInRole;
    @FXML private Label  loggedInTime;
    @FXML private Label  pageTitle;
    @FXML private Button themeToggle;

    @FXML private Label contentTitle;
    @FXML private Label contentSubtitle;

    @FXML private Label statTotalUsers;
    @FXML private Label statAdmins;
    @FXML private Label statUsers;
    @FXML private Label userCountBadge;

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> roleFilterComboBox;

    @FXML private TableView<User>           userTable;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colProfiles;
    @FXML private TableColumn<User, String> colStatus;

    @FXML private Label            formTitle;
    @FXML private TextField        usernameField;
    @FXML private PasswordField    passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label            feedbackLabel;
    @FXML private Button           saveButton;

    @FXML private ComboBox<String> profileComboBox;
    @FXML private Label            assignedProfilesLabel;
    private final ProfileDAO profileDAO = new ProfileDAO();

    @FXML private ScrollPane contentArea;
    private Node defaultContent;

    private UserManager userManager;
    private final LogManager logManager = new LogManager();
    private User    userBeingEdited = null;
    private boolean isDarkMode      = false;
    private ObservableList<User> allUsers = FXCollections.observableArrayList();

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
            if (loggedInRole != null)
                loggedInRole.setText(
                        current.isAdmin() ? "Administrator" : "Scanner User");
            if (loggedInTime != null)
                loggedInTime.setText("Since: "
                        + SessionManager.getInstance().getLoginTime());
        }

        roleComboBox.setItems(
                FXCollections.observableArrayList("Admin", "User"));
        roleComboBox.setValue("User");

        if (roleFilterComboBox != null) {
            roleFilterComboBox.setItems(
                    FXCollections.observableArrayList("All", "Admin", "User"));
            roleFilterComboBox.setValue("All");
        }

        // Wire table columns
        colUsername.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        colRole.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));

        if (colProfiles != null) {
            colProfiles.setCellValueFactory(data -> {
                try {
                    List<String> profiles =
                            profileDAO.getProfilesForUser(data.getValue().getId());
                    return new SimpleStringProperty(
                            profiles.isEmpty()
                                    ? "None"
                                    : String.join(", ", profiles));
                } catch (Exception e) {
                    return new SimpleStringProperty("Error");
                }
            });
        }

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

        // Inline action buttons per row
        TableColumn<User, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(240);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn       = new Button("Edit");
            private final Button deactivateBtn = new Button("Deactivate");
            private final Button reactivateBtn = new Button("Reactivate");
            private final HBox   box           = new HBox(6, editBtn,
                    deactivateBtn, reactivateBtn);
            {
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10;");
                deactivateBtn.getStyleClass().add("btn-danger");
                deactivateBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10;");
                reactivateBtn.getStyleClass().add("btn-secondary");
                reactivateBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 10;");

                editBtn.setOnAction(e -> {
                    userTable.getSelectionModel().select(getIndex());
                    handleEditUser();
                });
                deactivateBtn.setOnAction(e -> {
                    userTable.getSelectionModel().select(getIndex());
                    handleDeleteUser();
                });
                reactivateBtn.setOnAction(e -> {
                    userTable.getSelectionModel().select(getIndex());
                    handleReactivateUser();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        userTable.getColumns().add(actionsCol);

        userTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) showAssignedProfiles(newVal);
                    else if (assignedProfilesLabel != null)
                        assignedProfilesLabel.setText("");
                });

        if (searchField != null)
            searchField.textProperty().addListener(
                    (obs, old, val) -> applyFilters());
        if (roleFilterComboBox != null)
            roleFilterComboBox.valueProperty().addListener(
                    (obs, old, val) -> applyFilters());

        loadUsers();
        loadProfilesIntoComboBox();

        Platform.runLater(() -> {
            if (contentArea != null)
                defaultContent = contentArea.getContent();

            // Keyboard shortcuts
            if (rootPane.getScene() != null)
                registerKeyboardShortcuts(rootPane.getScene());
            rootPane.sceneProperty().addListener(
                    (obs, oldScene, newScene) -> {
                        if (newScene != null)
                            registerKeyboardShortcuts(newScene);
                    });
        });
    }

    // Register global keyboard shortcuts
    private void registerKeyboardShortcuts(javafx.scene.Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            boolean textFieldFocused =
                    scene.getFocusOwner() instanceof TextField
                            || scene.getFocusOwner() instanceof PasswordField;

            switch (event.getCode()) {
                case DIGIT1 -> {
                    if (!textFieldFocused) {
                        handleNavUsers(); event.consume();
                    }
                }
                case DIGIT2 -> {
                    if (!textFieldFocused) {
                        handleNavClients(); event.consume();
                    }
                }
                case DIGIT3 -> {
                    if (!textFieldFocused) {
                        handleNavArchives(); event.consume();
                    }
                }
                case DIGIT4 -> {
                    if (!textFieldFocused) {
                        handleNavProfiles(); event.consume();
                    }
                }
                case DIGIT5 -> {
                    if (!textFieldFocused) {
                        handleNavBoxes(); event.consume();
                    }
                }
                case DIGIT6 -> {
                    if (!textFieldFocused) {
                        handleNavLogs(); event.consume();
                    }
                }
                case E -> {
                    if (!textFieldFocused) {
                        handleEditUser(); event.consume();
                    }
                }
                case DELETE -> {
                    if (!textFieldFocused) {
                        handleDeleteUser(); event.consume();
                    }
                }
                case R -> {
                    if (!textFieldFocused) {
                        handleReactivateUser(); event.consume();
                    }
                }
                case N -> {
                    if (!textFieldFocused) {
                        handleClearForm(); event.consume();
                    }
                }
                case ESCAPE -> { handleClearForm(); event.consume(); }
                case F5     -> { loadUsers(); event.consume(); }
                case ENTER  -> {
                    if (textFieldFocused) {
                        handleSaveUser(); event.consume();
                    }
                }
                default -> {}
            }
        });
    }

    private void loadUsers() {
        try {
            allUsers = FXCollections.observableArrayList(
                    userManager.getAllUsersIncludingInactive());
            userTable.setItems(allUsers);

            long adminCount = allUsers.stream()
                    .filter(u -> u.isAdmin() && u.isActive()).count();
            long userCount  = allUsers.stream()
                    .filter(u -> !u.isAdmin() && u.isActive()).count();
            long total      = allUsers.stream().filter(User::isActive).count();

            statTotalUsers.setText(String.valueOf(total));
            statAdmins.setText(String.valueOf(adminCount));
            statUsers.setText(String.valueOf(userCount));
            userCountBadge.setText(String.valueOf(allUsers.size()));
        } catch (Exception e) {
            showFeedback("Could not load users: " + e.getMessage(), false);
        }
    }

    private void applyFilters() {
        if (searchField == null || roleFilterComboBox == null) return;
        String search = searchField.getText() == null
                ? "" : searchField.getText().toLowerCase().trim();
        String role = roleFilterComboBox.getValue();

        ObservableList<User> filtered = allUsers.stream()
                .filter(u -> {
                    boolean matchesSearch = search.isEmpty()
                            || u.getUsername().toLowerCase().contains(search);
                    boolean matchesRole = role == null
                            || role.equals("All")
                            || u.getRole().equalsIgnoreCase(role);
                    return matchesSearch && matchesRole;
                })
                .collect(Collectors.toCollection(
                        FXCollections::observableArrayList));

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

    private void loadProfilesIntoComboBox() {
        try {
            if (profileComboBox == null) return;
            profileComboBox.getItems().clear();
            profileComboBox.getItems().addAll(profileDAO.getAllProfileNames());
        } catch (Exception e) {
            showFeedback("Could not load profiles: " + e.getMessage(), false);
        }
    }

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

    @FXML
    private void handleSaveUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role     = roleComboBox.getValue();

        try {
            if (userBeingEdited == null) {
                userManager.createUser(username, password, role);
                logManager.log("USER_CREATED",
                        SessionManager.getInstance().getCurrentUser().getId(),
                        "User created: " + username);
                showFeedback("User '" + username + "' created!", true);
            } else {
                userManager.updateUser(
                        userBeingEdited.getId(), username, password, role);
                showFeedback("User '" + username + "' updated!", true);
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
            showFeedback("Please select a user to edit.", false); return;
        }
        userBeingEdited = selected;
        usernameField.setText(selected.getUsername());
        passwordField.clear();
        roleComboBox.setValue(selected.getRole());
        formTitle.setText("Edit User");
        saveButton.setText("Update User");
        showFeedback("Editing: " + selected.getUsername()
                + " — leave password blank to keep existing.", true);
        usernameField.requestFocus();
    }

    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to deactivate.", false); return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate User");
        confirm.setHeaderText("Deactivate '" + selected.getUsername() + "'?");
        confirm.setContentText(
                "The user will be deactivated and cannot log in.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userManager.deleteUser(selected.getId());
                    logManager.log("USER_DEACTIVATED",
                            SessionManager.getInstance().getCurrentUser().getId(),
                            "User deactivated: " + selected.getUsername());
                    showFeedback("User deactivated.", true);
                    loadUsers();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not deactivate: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleReactivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a user to reactivate.", false); return;
        }
        try {
            userManager.reactivateUser(selected.getId());
            logManager.log("USER_REACTIVATED",
                    SessionManager.getInstance().getCurrentUser().getId(),
                    "User reactivated: " + selected.getUsername());
            showFeedback("User '" + selected.getUsername()
                    + "' reactivated!", true);
            loadUsers();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleAssignProfile() {
        User   selected        = userTable.getSelectionModel().getSelectedItem();
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
                showFeedback("Profile not found in database.", false); return;
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

    // Navigation
    @FXML private void handleNavDashboard() {
        setActivePage("Dashboard", "Overview of the system");
        showDefaultContent();
    }
    @FXML private void handleNavUsers() {
        setActivePage("Users", "Manage user accounts");
        showDefaultContent(); loadUsers();
    }
    @FXML private void handleNavClients() {
        setActivePage("Clients", "Manage client organisations");
        loadView("/dk/easv/scanova/clientView.fxml");
    }
    @FXML private void handleNavArchives() {
        setActivePage("Archives", "Manage archives");
        loadView("/dk/easv/scanova/archiveView.fxml");
    }
    @FXML private void handleNavProfiles() {
        setActivePage("Profiles", "Manage scanning profiles");
        loadView("/dk/easv/scanova/profileView.fxml");
    }
    @FXML private void handleNavBoxes() {
        setActivePage("Boxes", "Manage scanning boxes");
        loadView("/dk/easv/scanova/boxView.fxml");
    }
    @FXML private void handleNavLogs() {
        setActivePage("Logs", "Audit trail of all actions");
        loadView("/dk/easv/scanova/logView.fxml");
    }
    @FXML private void handleNavDocuments() {
        setActivePage("Documents", "View scanned documents");
    }
    @FXML private void handleNavSettings() {
        setActivePage("Settings", "System configuration");
    }

    private void loadView(String fxmlPath) {
        if (contentArea == null) return;
        try {
            javafx.fxml.FXMLLoader loader =
                    new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.setContent(view);
        } catch (Exception e) {
            System.err.println("Could not load view: " + fxmlPath);
            e.printStackTrace();
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
                navUsers, navClients, navArchives, navProfiles,
                navBoxes, navDocuments, navLogs, navSettings);
        navItems.forEach(b -> {
            b.getStyleClass().remove("nav-item-active");
            if (!b.getStyleClass().contains("nav-item"))
                b.getStyleClass().add("nav-item");
        });

        switch (title) {
            case "Users"     -> setActive(navUsers);
            case "Clients"   -> setActive(navClients);
            case "Archives"  -> setActive(navArchives);
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

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}