package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ClientManager;
import dk.easv.scanova.BLL.ProfileManager;
import dk.easv.scanova.BE.Client;
import dk.easv.scanova.Model.Profile;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.List;

public class ProfileController {

    @FXML private TableView<Profile>            profileTable;
    @FXML private TableColumn<Profile, String>  colProfileName;
    @FXML private TableColumn<Profile, String>   colProfileRotation;
    @FXML private TableColumn<Profile, String>   colProfileBrightness;
    @FXML private TableColumn<Profile, String>  colProfileClient;
    @FXML private TableColumn<Profile, String> colProfileStatus;
    @FXML private Label profileCountBadge;

    @FXML private Label            profileFormTitle;
    @FXML private TextField        profileNameField;
    @FXML private Slider           profileRotationSlider;
    @FXML private Label            profileRotationValueLabel;
    @FXML private Slider           profileBrightnessSlider;
    @FXML private Label            profileBrightnessValueLabel;
    @FXML private ComboBox<Client> profileClientComboBox;
    @FXML private Label            profileFeedbackLabel;
    @FXML private Button           profileSaveButton;

    private ProfileManager profileManager;
    private ClientManager  clientManager;
    private Profile        profileBeingEdited = null;
    private List<Client> allClients;

    @FXML
    public void initialize() {

        try {
            profileManager = new ProfileManager();
            clientManager = new ClientManager();
        } catch (Exception e) {
            showFeedback("Cannot connect to database: " + e.getMessage(), false);
            return;
        }

        colProfileName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getName()));

        colProfileRotation.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.valueOf((int) d.getValue().getRotation()) + "°"));

        colProfileBrightness.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.format("%.1f", d.getValue().getBrightness())));

        colProfileClient.setCellValueFactory(d -> {
            int clientId = d.getValue().getClientId();
            if (allClients != null) {
                return allClients.stream()
                        .filter(c -> c.getId() == clientId)
                        .findFirst()
                        .map(c -> new SimpleStringProperty(c.getName()))
                        .orElse(new SimpleStringProperty("Unknown"));

            }

            return new SimpleStringProperty(String.valueOf(clientId));
        });

        colProfileStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Inactive"));

        colProfileStatus.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else if (item.equals("Active")) {
                            setText("Active");
                            setStyle("-fx-text-fill: #2ECC9A; -fx-font-weight: bold;");
                        } else {
                            setText("Inactive");
                            setStyle("-fx-text-fill: #E53E3E; -fx-font-weight: bold;");
                        }
                    }
                });

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

        profileBrightnessSlider.setMin(0.1);
        profileBrightnessSlider.setMax(3.0);
        profileBrightnessSlider.setValue(1.0);
        profileBrightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double rounded = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            profileBrightnessValueLabel.setText(String.valueOf(rounded));
        });

        loadClients();
        loadProfileTable();
    }

    private void loadClients() {
        try {
            allClients = clientManager.getAllClients();
            profileClientComboBox.setItems(
                    FXCollections.observableArrayList(allClients));
        } catch (Exception e) {
            showFeedback("Could not load clients: " + e.getMessage(), false);
        }
    }

    private void loadProfileTable() {
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

        if (allClients != null) {
            allClients.stream()
                    .filter(c -> c.getId() == selected.getClientId())
                    .findFirst()
                    .ifPresent(c -> profileClientComboBox.setValue(c));
        }

        profileFormTitle.setText("Edit profile");
        profileSaveButton.setText("Update profile");
        showFeedback("Editing: " + selected.getName(), true);
    }

    @FXML
    private void handleDeactivateProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a profile to deactivate.", false);
            return;
        }

        if (!selected.isActive()) {
            showFeedback("Profile is already inactive.", false);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Profile");
        confirm.setHeaderText("Deactivate '" + selected.getName() + "'?");
        confirm.setContentText("The profile will be hidden from scanning.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    profileManager.deleteProfile(selected.getId());
                    showFeedback("Profile deactivated.", true);
                    loadProfileTable();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not deactivate: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleSaveProfile() {

        String name       = profileNameField.getText().trim();
        float  rotation   = (float) profileRotationSlider.getValue();
        float  brightness = (float) profileBrightnessSlider.getValue();

        Client selectedClient = profileClientComboBox.getValue();
        int    clientId       = selectedClient != null ? selectedClient.getId() : 0;

        try {
            if (profileBeingEdited == null) {
                profileManager.createProfile(name, rotation, brightness, clientId);
                loadProfileTable();
                handleClearForm();
                showFeedback("Profile '" + name + "' created!", true);
            } else {
                profileManager.updateProfile(
                        profileBeingEdited.getId(), name, rotation, brightness, clientId);
                loadProfileTable();
                handleClearForm();
                showFeedback("Profile '" + name + "' updated!", true);
            }
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    @FXML
    private void handleReactivateProfile() {
        Profile selected = profileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a profile to reactivate.", false);
            return;
        }
        if (selected.isActive()) {
            showFeedback("Profile is already active.", false);
            return;
        }
        try {
            profileManager.reactivateProfile(selected.getId());
            showFeedback("Profile '" + selected.getName() + "' reactivated!", true);
            loadProfileTable();
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
        if (profileFeedbackLabel == null) return;
        profileFeedbackLabel.setText(message);
        profileFeedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}