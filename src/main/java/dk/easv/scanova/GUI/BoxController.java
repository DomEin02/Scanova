package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ArchiveManager;
import dk.easv.scanova.BLL.BoxManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.DAL.ProfileDAO;
import dk.easv.scanova.Model.Archive;
import dk.easv.scanova.Model.Box;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.List;

public class BoxController {

    @FXML private TableView<Box>           boxTable;
    @FXML private TableColumn<Box, String> colBoxLabel;
    @FXML private TableColumn<Box, String> colBoxArchive;
    @FXML private TableColumn<Box, String> colBoxProfile;
    @FXML private TableColumn<Box, String> colBoxStatus;
    @FXML private Label                    boxCountBadge;

    @FXML private Label             formTitle;
    @FXML private TextField         labelField;
    @FXML private ComboBox<Archive> archiveComboBox;
    @FXML private ComboBox<String>  profileComboBox;
    @FXML private Label             feedbackLabel;
    @FXML private Button            saveButton;

    private final BoxManager     boxManager     = new BoxManager();
    private final ArchiveManager archiveManager = new ArchiveManager();
    private final ProfileDAO     profileDAO     = new ProfileDAO();
    private Box boxBeingEdited = null;

    private int getCurrentUserId() {
        if (SessionManager.getInstance().getCurrentUser() == null) return -1;
        return SessionManager.getInstance().getCurrentUser().getId();
    }

    @FXML
    public void initialize() {
        colBoxLabel.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLabel()));

        // Archive name from joined query
        colBoxArchive.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getArchiveName()));

        colBoxProfile.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getProfileName() != null
                                ? data.getValue().getProfileName()
                                : "None"));

        if (colBoxStatus != null) {
            colBoxStatus.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().isActive() ? "Active" : "Inactive"));
            colBoxStatus.setCellFactory(col -> new TableCell<>() {
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
        }

        loadArchives();
        loadProfiles();
        loadBoxes();
    }

    private void loadArchives() {
        try {
            ObservableList<Archive> archives =
                    FXCollections.observableArrayList(
                            archiveManager.getAllArchives());
            archiveComboBox.setItems(archives);
        } catch (Exception e) {
            showFeedback("Could not load archives: " + e.getMessage(), false);
        }
    }

    private void loadProfiles() {
        try {
            List<String> profiles = profileDAO.getAllProfileNames();
            profileComboBox.setItems(
                    FXCollections.observableArrayList(profiles));
        } catch (Exception e) {
            showFeedback("Could not load profiles: " + e.getMessage(), false);
        }
    }

    private void loadBoxes() {
        try {
            ObservableList<Box> boxes =
                    FXCollections.observableArrayList(
                            boxManager.getAllBoxesIncludingInactive());
            boxTable.setItems(boxes);
            if (boxCountBadge != null)
                boxCountBadge.setText(String.valueOf(
                        boxes.stream().filter(Box::isActive).count()));
        } catch (Exception e) {
            showFeedback("Could not load boxes: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveBox() {
        String  label           = labelField.getText().trim();
        Archive selectedArchive = archiveComboBox.getValue();
        String  selectedProfile = profileComboBox.getValue();

        try {
            int profileId = selectedProfile != null
                    ? profileDAO.getProfileIdByName(selectedProfile) : -1;

            if (boxBeingEdited == null) {
                if (selectedArchive == null)
                    throw new Exception("Please select an archive.");
                boxManager.createBox(
                        label,
                        selectedArchive.getId(),
                        profileId,
                        getCurrentUserId());
                showFeedback("Box '" + label + "' created!", true);
            } else {
                boxManager.updateBox(
                        boxBeingEdited.getId(),
                        label,
                        selectedArchive != null
                                ? selectedArchive.getId()
                                : boxBeingEdited.getArchiveId(),
                        profileId,
                        getCurrentUserId());
                showFeedback("Box '" + label + "' updated!", true);
            }
            loadBoxes();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    @FXML
    private void handleEditBox() {
        Box selected = boxTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a box to edit.", false);
            return;
        }
        boxBeingEdited = selected;
        labelField.setText(selected.getLabel());
        if (selected.getProfileName() != null)
            profileComboBox.setValue(selected.getProfileName());
        formTitle.setText("Edit Box");
        saveButton.setText("Update Box");
        showFeedback("Editing: " + selected.getLabel(), true);
    }

    @FXML
    private void handleDeleteBox() {
        Box selected = boxTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a box to deactivate.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Box");
        confirm.setHeaderText("Deactivate '" + selected.getLabel() + "'?");
        confirm.setContentText(
                "The box will be deactivated and cannot be used for scanning.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boxManager.deleteBox(
                            selected.getId(),
                            getCurrentUserId(),
                            selected.getLabel());
                    showFeedback("Box deactivated.", true);
                    loadBoxes();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback("Could not deactivate: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleReactivateBox() {
        Box selected = boxTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a box to reactivate.", false);
            return;
        }
        if (selected.isActive()) {
            showFeedback("Box is already active.", false);
            return;
        }
        try {
            boxManager.reactivateBox(
                    selected.getId(),
                    getCurrentUserId(),
                    selected.getLabel());
            showFeedback("Box '" + selected.getLabel() + "' reactivated!", true);
            loadBoxes();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleClearForm() {
        boxBeingEdited = null;
        labelField.clear();
        archiveComboBox.setValue(null);
        profileComboBox.setValue(null);
        formTitle.setText("Create New Box");
        saveButton.setText("Create Box");
        feedbackLabel.setText("");
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}