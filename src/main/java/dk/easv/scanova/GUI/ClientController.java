package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.DAL.ClientDAO;
import dk.easv.scanova.Model.Client;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class ClientController {

    @FXML private TableView<Client> clientTable;
    @FXML private TableColumn<Client, String> colClientName;
    @FXML private TextField usernameField;
    @FXML private Label feedbackLabel;
    @FXML private Button saveButton;
    @FXML private Label formTitle;
    @FXML private Label clientCountBadge;

    private final ClientDAO clientDAO = new ClientDAO();
    private Client clientBeingEdited = null;

    // Get current logged in user id for logging
    private int getCurrentUserId() {
        if (SessionManager.getInstance().getCurrentUser() == null) return -1;
        return SessionManager.getInstance().getCurrentUser().getId();
    }

    @FXML
    public void initialize() {
        colClientName.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName()));
        loadClients();
    }

    private void loadClients() {
        try {
            ObservableList<Client> clients =
                    FXCollections.observableArrayList(clientDAO.getAllClients());
            clientTable.setItems(clients);
            // Update badge
            if (clientCountBadge != null)
                clientCountBadge.setText(String.valueOf(clients.size()));
        } catch (Exception e) {
            showFeedback("Could not load clients: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveClient() {
        String name = usernameField.getText().trim();

        if (name.isEmpty()) {
            showFeedback("Client name cannot be empty.", false);
            return;
        }

        try {
            if (clientBeingEdited == null) {
                clientDAO.createClient(name, getCurrentUserId());
                showFeedback("Client '" + name + "' created!", true);
            } else {
                clientDAO.updateClient(
                        clientBeingEdited.getId(), name, getCurrentUserId());
                showFeedback("Client '" + name + "' updated!", true);
            }
            loadClients();
            handleClearForm();
        } catch (Exception e) {
            showFeedback("Could not save: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleEditClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a client to edit.", false);
            return;
        }
        clientBeingEdited = selected;
        usernameField.setText(selected.getName());
        formTitle.setText("Edit Client");
        saveButton.setText("Update Client");
        showFeedback("Editing: " + selected.getName(), true);
    }

    @FXML
    private void handleDeleteClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a client to deactivate.", false);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Client");
        confirm.setHeaderText("Deactivate '" + selected.getName() + "'?");
        confirm.setContentText(
                "The client will be deactivated and hidden from the system.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    clientDAO.deleteClient(
                            selected.getId(),
                            getCurrentUserId(),
                            selected.getName());
                    showFeedback("Client deactivated.", true);
                    loadClients();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback(e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleClearForm() {
        clientBeingEdited = null;
        usernameField.clear();
        formTitle.setText("Create New Client");
        saveButton.setText("Create Client");
        feedbackLabel.setText("");
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setTextFill(
                success ? Color.web("#2ECC9A") : Color.web("#E53E3E"));
    }
}