package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ClientManager;
import dk.easv.scanova.BLL.ProfileManager;
import dk.easv.scanova.Model.Client;
import dk.easv.scanova.Model.Profile;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import java.util.HashMap;
import java.util.Map;

public class ProfileController {

    @FXML private TableView<Profile>           profileTable;
    @FXML private TableColumn<Profile, String> colProfileName;
    @FXML private TableColumn<Profile, String> colProfileRotation;
    @FXML private TableColumn<Profile, String> colProfileBrightness;
    @FXML private TableColumn<Profile, String> colProfileClient;
    @FXML private TableColumn<Profile, String> colProfileStatus;
    @FXML private Label                        profileCountBadge;

    @FXML private Label            profileFormTitle;
    @FXML private TextField        profileNameField;
    @FXML private Slider           profileRotationSlider;
    @FXML private Slider           profileBrightnessSlider;
    @FXML private Label            profileRotationValueLabel;
    @FXML private Label            profileBrightnessValueLabel;
    @FXML private ComboBox<Client> profileClientComboBox;
    @FXML private Label            profileFeedbackLabel;
    @FXML private Button           profileSaveButton;

    private final ProfileManager profileManager = new ProfileManager();
    private final ClientManager  clientManager  = new ClientManager();
    private Profile profileBeingEdited = null;
    private final Map<Integer, String> clientNameMap = new HashMap<>();

    @FXML
    public void initialize() {
        colProfileName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        colProfileRotation.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRotation() + "°"));
        colProfileBrightness.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getBrightness())));
        colProfileClient.setCellValueFactory(data ->
                new SimpleStringProperty(clientNameMap.getOrDefault(data.getValue().getClientId(), "—")));

        if (colProfileStatus != null) {
            colProfileStatus.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
            colProfileStatus.setCellFactory(col -> new TableCell<>() {
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
        TableColumn<Profile, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(240);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn       = new Button("Edit");
            private final Button deactivateBtn = new Button("Deactivate");
            private final Button reactivateBtn = new Button("Reactivate");
            private final HBox   box           = new HBox(6, editBtn, deactivateBtn, reactivateBtn);
            {
                editBtn.setOnAction(e -> {
                    profileTable.getSelectionModel().select(getIndex());
                    handleEditProfile();
                });
                deactivateBtn.setOnAction(e -> {
                    profileTable.getSelectionModel().select(getIndex());
                    handleDeleteProfile();
                });
                reactivateBtn.setOnAction(e -> {
                    profileTable.getSelectionModel().select(getIndex());
                    handleReactivateProfile();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        profileTable.getColumns().add(actionsCol);

        // Slider live labels
        profileRotationSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                profileRotationValueLabel.setText(String.format("%.0f°", newVal.doubleValue())));
        profileBrightnessSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                profileBrightnessValueLabel.setText(String.format("%.1f", newVal.doubleValue())));

        loadClients();
        loadProfiles();
    }

    private void loadClients() {
        try {
            ObservableList<Client> clients =
                    FXCollections.observableArrayList(clientManager.getAllClients());
            profileClientComboBox.setItems(clients);
            clientNameMap.clear();
            for (Client c : clients) clientNameMap.put(c.getId(), c.getName());
        } catch (Exception e) {
            showFeedback("Could not load clients: " + e.getMessage(), false);
        }
    }

    private void loadProfiles() {
        try {
            ObservableList<Profile> profiles =
                    FXCollections.observableArrayList(profileManager.getAllProfilesIncludingInactive());
            profileTable.setItems(profiles);
            if (profileCountBadge != null)
                profileCountBadge.setText(String.valueOf(
                        profiles.stream().filter(Profile::isActive).count()));
        } catch (Exception e) {
            showFeedback("Could not load profiles: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveProfile() {
        String name       = profileNameField.getText().trim();
        float  rotation   = (float) profileRotationSlider.getValue();
        float  brightness = (float) profileBrightnessSlider.getValue();
        Client client     = profileClientComboBox.getValue();

        try {
            if (profileBeingEdited == null) {
                if (client == null) throw new Exception("Please select a client.");
                profileManager.createProfile(name, rotation, brightness, client.getId());
                showFeedback("Profile '" + name + "' created!", true);
            } else {
                int clientId = client != null ? client.getId() : profileBeingEdited.getClientId();
                profileManager.updateProfile(profileBeingEdited.getId(), name, rotation, brightness, clientId);
                showFeedback("Profile '" + name + "' updated!", true);
            }
            loadProfiles();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    private void handleEditProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a profile to edit.", false); return; }
        profileBeingEdited = selected;
        profileNameField.setText(selected.getName());
        profileRotationSlider.setValue(selected.getRotation());
        profileBrightnessSlider.setValue(selected.getBrightness());
        Client matchingClient = profileClientComboBox.getItems().stream()
                .filter(c -> c.getId() == selected.getClientId())
                .findFirst().orElse(null);
        profileClientComboBox.setValue(matchingClient);
        profileFormTitle.setText("Edit Profile");
        profileSaveButton.setText("Update Profile");
        showFeedback("Editing: " + selected.getName(), true);
        profileNameField.requestFocus();
    }

    private void handleDeleteProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a profile to deactivate.", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Profile");
        confirm.setHeaderText("Deactivate '" + selected.getName() + "'?");
        confirm.setContentText("The profile will be deactivated and cannot be used for scanning.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    profileManager.deleteProfile(selected.getId());
                    showFeedback("Profile deactivated.", true);
                    loadProfiles();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback(e.getMessage(), false);
                }
            }
        });
    }

    private void handleReactivateProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a profile to reactivate.", false); return; }
        if (selected.isActive()) { showFeedback("Profile is already active.", false); return; }
        try {
            profileManager.reactivateProfile(selected.getId());
            showFeedback("Profile '" + selected.getName() + "' reactivated!", true);
            loadProfiles();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
    }

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

    private void showFeedback(String message, boolean success) {
        profileFeedbackLabel.setText(message);
        profileFeedbackLabel.setStyle(success
                ? "-fx-text-fill: #2ECC9A;"
                : "-fx-text-fill: #E53E3E;");
    }
}