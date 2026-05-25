package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ClientManager;
import dk.easv.scanova.BLL.ProfileManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.Client;
import dk.easv.scanova.Model.Profile;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

public class ProfileController {

    // ── Profile table ─────────────────────────────────────────────────────────
    @FXML private TableView<Profile>            profileTable;
    @FXML private TableColumn<Profile, Integer> colProfileId;
    @FXML private TableColumn<Profile, String>  colProfileName;
    @FXML private TableColumn<Profile, Float>   colProfileRotation;
    @FXML private TableColumn<Profile, Float>   colProfileBrightness;
    @FXML private TableColumn<Profile, Integer> colProfileClientId;
    @FXML private Label                         profileCountBadge;

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

    // ── BLL only — never DAL directly ────────────────────────────────────────
    private final ProfileManager profileManager = new ProfileManager();
    private final ClientManager  clientManager  = new ClientManager();

    private Profile profileBeingEdited = null;

    private int getCurrentUserId() {
        if (SessionManager.getInstance().getCurrentUser() == null) return -1;
        return SessionManager.getInstance().getCurrentUser().getId();
    }

    @FXML
    public void initialize() {
        // Wire table columns
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
                    showFeedback("Editing: " + p.getName(), true);
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
                                showFeedback("Profile deleted.", true);
                                loadProfiles();
                                handleClearForm();
                            } catch (Exception ex) {
                                showFeedback("Could not delete: "
                                        + ex.getMessage(), false);
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

        // Rotation slider
        profileRotationSlider.setMin(-180);
        profileRotationSlider.setMax(180);
        profileRotationSlider.setValue(0);
        profileRotationSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {
                    int snapped = ((int) Math.round(
                            newVal.doubleValue() / 5)) * 5;
                    profileRotationSlider.setValue(snapped);
                    profileRotationValueLabel.setText(snapped + "°");
                });

        // Brightness slider
        profileBrightnessSlider.setMin(0.1);
        profileBrightnessSlider.setMax(3.0);
        profileBrightnessSlider.setValue(1.0);
        profileBrightnessSlider.valueProperty().addListener(
                (obs, oldVal, newVal) -> {
                    double rounded =
                            Math.round(newVal.doubleValue() * 10.0) / 10.0;
                    profileBrightnessValueLabel.setText(
                            String.valueOf(rounded));
                });

        loadClients();
        loadProfiles();
    }

    // ── Load clients into dropdown ────────────────────────────────────────────
    private void loadClients() {
        try {
            List<Client> clients = clientManager.getAllClients();
            profileClientComboBox.setItems(
                    FXCollections.observableArrayList(
                            clients.stream()
                                    .map(Client::getName)
                                    .collect(Collectors.toList())));
        } catch (Exception e) {
            showFeedback("Could not load clients: " + e.getMessage(), false);
        }
    }

    // ── Load profiles ─────────────────────────────────────────────────────────
    private void loadProfiles() {
        try {
            ObservableList<Profile> profiles =
                    FXCollections.observableArrayList(
                            profileManager.getAllProfiles());
            profileTable.setItems(profiles);
            if (profileCountBadge != null)
                profileCountBadge.setText(String.valueOf(profiles.size()));
        } catch (Exception e) {
            showFeedback("Could not load profiles: " + e.getMessage(), false);
        }
    }

    // ── Save (create or update) ───────────────────────────────────────────────
    @FXML
    private void handleSaveProfile() {
        String name        = profileNameField.getText().trim();
        float  rotation    = (float) profileRotationSlider.getValue();
        float  brightness  = (float) profileBrightnessSlider.getValue();
        String clientName  = profileClientComboBox.getValue();

        try {
            if (clientName == null)
                throw new Exception("Please select a client.");

            // Get client id by name
            List<Client> clients = clientManager.getAllClients();
            int clientId = clients.stream()
                    .filter(c -> c.getName().equals(clientName))
                    .findFirst()
                    .orElseThrow(() -> new Exception("Client not found."))
                    .getId();

            if (profileBeingEdited == null) {
                profileManager.createProfile(
                        name, rotation, brightness, clientId);
                showFeedback("Profile '" + name + "' created!", true);
            } else {
                profileManager.updateProfile(
                        profileBeingEdited.getId(),
                        name, rotation, brightness, clientId);
                showFeedback("Profile '" + name + "' updated!", true);
            }
            loadProfiles();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    // ── Edit selected ─────────────────────────────────────────────────────────
    @FXML
    private void handleEditProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a profile to edit.", false);
            return;
        }
        profileBeingEdited = selected;
        profileNameField.setText(selected.getName());
        profileRotationSlider.setValue(selected.getRotation());
        profileBrightnessSlider.setValue(selected.getBrightness());
        profileFormTitle.setText("Edit profile");
        profileSaveButton.setText("Update profile");
        showFeedback("Editing: " + selected.getName(), true);
    }

    // ── Delete selected ───────────────────────────────────────────────────────
    @FXML
    private void handleDeleteProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a profile to delete.", false);
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
                    showFeedback("Profile deleted.", true);
                    loadProfiles();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not delete: " + e.getMessage(), false);
                }
            }
        });
    }

    // ── Clear form ────────────────────────────────────────────────────────────
    @FXML
    public void handleClearForm() {
        profileBeingEdited = null;
        profileNameField.clear();
        profileRotationSlider.setValue(0);
        profileBrightnessSlider.setValue(1.0);
        profileClientComboBox.setValue(null);
        profileFormTitle.setText("Create new profile");
        profileSaveButton.setText("Create profile");
        profileFeedbackLabel.setText("");
    }

    // ── Feedback ──────────────────────────────────────────────────────────────
    private void showFeedback(String message, boolean success) {
        profileFeedbackLabel.setText(message);
        profileFeedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}