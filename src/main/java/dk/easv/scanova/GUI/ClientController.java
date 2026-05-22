package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ClientManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.BE.Client;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class ClientController {

    @FXML private TableView<Client>           clientTable;
    @FXML private TableColumn<Client, String> colClientName;
    @FXML private TableColumn<Client, String> colClientStatus;
    @FXML private TextField                   usernameField;
    @FXML private Label                       feedbackLabel;
    @FXML private Button                      saveButton;
    @FXML private Label                       formTitle;
    @FXML private Label                       clientCountBadge;

    private final ClientManager clientManager = new ClientManager();
    private Client clientBeingEdited = null;

    private int getCurrentUserId() {
        if (SessionManager.getInstance().getCurrentUser() == null) return -1;
        return SessionManager.getInstance().getCurrentUser().getId();
    }

    @FXML
    public void initialize() {
        colClientName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        if (colClientStatus != null) {
            colClientStatus.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().isActive() ? "Active" : "Inactive"));
            colClientStatus.setCellFactory(col -> new TableCell<>() {
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
    }

    private void loadClients() {
        try {
            ObservableList<Client> clients =
                    FXCollections.observableArrayList(
                            clientManager.getAllClientsIncludingInactive());
            clientTable.setItems(clients);
            if (clientCountBadge != null)
                clientCountBadge.setText(String.valueOf(
                        clients.stream().filter(Client::isActive).count()));
        } catch (Exception e) {
            showFeedback("Could not load clients: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveClient() {
        String name = usernameField.getText().trim();
        try {
            if (clientBeingEdited == null) {
                clientManager.createClient(name, getCurrentUserId());
                showFeedback("Client '" + name + "' created!", true);
            } else {
                clientManager.updateClient(
                        clientBeingEdited.getId(), name, getCurrentUserId());
                showFeedback("Client '" + name + "' updated!", true);
            }
            loadClients();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
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
                "The client will be deactivated and hidden from scanning.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    clientManager.deleteClient(
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
    private void handleReactivateClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Please select a client to reactivate.", false);
            return;
        }
        if (selected.isActive()) {
            showFeedback("Client is already active.", false);
            return;
        }
        try {
            clientManager.reactivateClient(
                    selected.getId(),
                    getCurrentUserId(),
                    selected.getName());
            showFeedback("Client '" + selected.getName() + "' reactivated!", true);
            loadClients();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
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