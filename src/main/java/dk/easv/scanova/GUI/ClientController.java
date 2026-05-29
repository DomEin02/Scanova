package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ClientManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.Client;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class ClientController {

    @FXML private TableView<Client>           clientTable;
    @FXML private TableColumn<Client, String> colClientName;
    @FXML private TableColumn<Client, String> colClientStatus;
    @FXML private Label                       clientCountBadge;

    @FXML private Label     formTitle;
    @FXML private TextField usernameField;   // client name field (fx:id from FXML)
    @FXML private Label     feedbackLabel;
    @FXML private Button    saveButton;

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
                    new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
            colClientStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("status-active", "status-inactive");
                    if (empty || item == null) {
                        setText(null);
                    } else if (item.equals("Active")) {
                        setText("Active");
                        getStyleClass().add("status-active");
                    } else {
                        setText("Inactive");
                        getStyleClass().add("status-inactive");
                    }
                }
            });
        }

        // Inline action buttons per row
        TableColumn<Client, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(240);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn       = new Button("Edit");
            private final Button deactivateBtn = new Button("Deactivate");
            private final Button reactivateBtn = new Button("Reactivate");
            private final HBox   box           = new HBox(6, editBtn, deactivateBtn, reactivateBtn);
            {
                editBtn.getStyleClass().add("btn-row-edit");
                deactivateBtn.getStyleClass().add("btn-row-danger");
                reactivateBtn.getStyleClass().add("btn-row-reactivate");

                editBtn.setOnAction(e -> {
                    clientTable.getSelectionModel().select(getIndex());
                    handleEditClient();
                });
                deactivateBtn.setOnAction(e -> {
                    clientTable.getSelectionModel().select(getIndex());
                    handleDeleteClient();
                });
                reactivateBtn.setOnAction(e -> {
                    clientTable.getSelectionModel().select(getIndex());
                    handleReactivateClient();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        clientTable.getColumns().add(actionsCol);

        loadClients();
    }

    private void loadClients() {
        try {
            ObservableList<Client> clients =
                    FXCollections.observableArrayList(clientManager.getAllClientsIncludingInactive());
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
                clientManager.updateClient(clientBeingEdited.getId(), name, getCurrentUserId());
                showFeedback("Client '" + name + "' updated!", true);
            }
            loadClients();
            handleClearForm();
        } catch (Exception e) {
            showFeedback(e.getMessage(), false);
        }
    }

    private void handleEditClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a client to edit.", false); return; }
        clientBeingEdited = selected;
        usernameField.setText(selected.getName());
        formTitle.setText("Edit Client");
        saveButton.setText("Update Client");
        showFeedback("Editing: " + selected.getName(), true);
        usernameField.requestFocus();
    }

    private void handleDeleteClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a client to deactivate.", false); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Client");
        confirm.setHeaderText("Deactivate '" + selected.getName() + "'?");
        confirm.setContentText("The client will be deactivated. All associated archives must be deactivated first.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    clientManager.deleteClient(selected.getId(), getCurrentUserId(), selected.getName());
                    showFeedback("Client deactivated.", true);
                    loadClients();
                    handleClearForm();
                } catch (Exception e) {
                    showFeedback(e.getMessage(), false);
                }
            }
        });
    }

    private void handleReactivateClient() {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Please select a client to reactivate.", false); return; }
        if (selected.isActive()) { showFeedback("Client is already active.", false); return; }
        try {
            clientManager.reactivateClient(selected.getId(), getCurrentUserId(), selected.getName());
            showFeedback("Client '" + selected.getName() + "' reactivated!", true);
            loadClients();
        } catch (Exception e) {
            showFeedback("Could not reactivate: " + e.getMessage(), false);
        }
    }

    @FXML
    public void handleClearForm() {
        clientBeingEdited = null;
        usernameField.clear();
        formTitle.setText("Create new client");
        saveButton.setText("Create client");
        feedbackLabel.setText("");
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(success ? "feedback-success" : "feedback-error");
    }
}