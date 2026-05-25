package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ArchiveManager;
import dk.easv.scanova.BLL.ClientManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.Archive;
import dk.easv.scanova.Model.Client;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class ArchiveController {

    @FXML private TableView<Archive>           archiveTable;
    @FXML private TableColumn<Archive, String> colArchiveName;
    @FXML private TableColumn<Archive, String> colArchiveClient;
    @FXML private TableColumn<Archive, String> colArchiveStatus;
    @FXML private Label                        archiveCountBadge;

    @FXML private Label            formTitle;
    @FXML private TextField        nameField;
    @FXML private ComboBox<Client> clientComboBox;
    @FXML private Label            feedbackLabel;
    @FXML private Button           saveButton;

    private final ArchiveManager archiveManager = new ArchiveManager();
    private final ClientManager  clientManager  = new ClientManager();
    private Archive archiveBeingEdited = null;

    private int getCurrentUserId() {
        if (SessionManager.getInstance().getCurrentUser() == null) return -1;
        return SessionManager.getInstance().getCurrentUser().getId();
    }

    @FXML
    public void initialize() {
        // Client name now shown from joined query
        colArchiveName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        colArchiveClient.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getClientName()));

        if (colArchiveStatus != null) {
            colArchiveStatus.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().isActive() ? "Active" : "Inactive"));
            colArchiveStatus.setCellFactory(col -> new TableCell<>() {
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

        loadClients();
        loadArchives();
    }

    private void loadClients() {
        try {
            ObservableList<Client> clients =
                    FXCollections.observableArrayList(
                            clientManager.getAllClients());
            clientComboBox.setItems(clients);
        } catch (Exception e) {
            showFeedback("Could not load clients: " + e.getMessage(), false);
        }
    }

    private void loadArchives() {
        try {
            ObservableList<Archive> archives =
                    FXCollections.observableArrayList(
                            archiveManager.getAllArchivesIncludingInactive());
            archiveTable.setItems(archives);
            if (archiveCountBadge != null)
                archiveCountBadge.setText(String.valueOf(
                        archives.stream().filter(Archive::isActive).count()));
        } catch (Exception e) {
            showFeedback("Could not load archives: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveArchive() {
        String name = nameField.getText().trim();
        Client selectedClient = clientComboBox.getValue();

        try {
            if (archiveBeingEdited == null) {
                if (selectedClient == null)
                    throw new Exception("Please select a client.");
                archiveManager.createArchive(
                        name, selectedClient.getId(), getCurrentUserId());
                showFeedback("Archive '" + name + "' created!", true);
            } else {
                archiveManager.updateArchive(
                        archiveBeingEdited.getId(), name, getCurrentUserId());
                showFeedback("Archive '" + name + "' updated!", true);
            }
            loadArchives();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    @FXML
    private void handleEditArchive() {
        Archive selected = archiveTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select an archive to edit.", false);
            return;
        }
        archiveBeingEdited = selected;
        nameField.setText(selected.getName());
        formTitle.setText("Edit Archive");
        saveButton.setText("Update Archive");
        showFeedback("Editing: " + selected.getName(), true);
    }

    @FXML
    private void handleDeleteArchive() {
        Archive selected = archiveTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select an archive to deactivate.", false);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Archive");
        confirm.setHeaderText("Deactivate '" + selected.getName() + "'?");
        confirm.setContentText("All boxes must be deactivated first.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    archiveManager.deleteArchive(
                            selected.getId(),
                            getCurrentUserId(),
                            selected.getName());
                    showFeedback("Archive deactivated.", true);
                    loadArchives();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback(e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleReactivateArchive() {
        Archive selected = archiveTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select an archive to reactivate.", false);
            return;
        }
        if (selected.isActive()) {
            showFeedback("Archive is already active.", false);
            return;
        }
        try {
            archiveManager.reactivateArchive(
                    selected.getId(),
                    getCurrentUserId(),
                    selected.getName());
            showFeedback("Archive '" + selected.getName()
                    + "' reactivated!", true);
            loadArchives();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleClearForm() {
        archiveBeingEdited = null;
        nameField.clear();
        clientComboBox.setValue(null);
        formTitle.setText("Create New Archive");
        saveButton.setText("Create Archive");
        feedbackLabel.setText("");
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}