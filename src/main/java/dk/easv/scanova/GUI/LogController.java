package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.LogManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

public class LogController {

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<String[]>           logTable;
    @FXML private TableColumn<String[], String> colAction;
    @FXML private TableColumn<String[], String> colCategory;
    @FXML private TableColumn<String[], String> colUsername;
    @FXML private TableColumn<String[], String> colDetails;
    @FXML private TableColumn<String[], String> colTimestamp;

    // ── Filters ───────────────────────────────────────────────────────────────
    @FXML private CheckBox filterLogin;
    @FXML private CheckBox filterScanning;
    @FXML private CheckBox filterManagement;
    @FXML private CheckBox filterError;
    @FXML private CheckBox filterOther;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label logCountLabel;

    private final LogManager logManager = new LogManager();

    @FXML
    public void initialize() {
        // Wire columns
        colAction.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));

        // Category column — shows LOGIN, SCANNING, MANAGEMENT etc.
        colCategory.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        LogManager.getCategory(data.getValue()[0])));

        // Color code the category cell
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(switch (item) {
                        case "LOGIN"      -> "-fx-text-fill: #63B3ED; -fx-font-weight: bold;";
                        case "SCANNING"   -> "-fx-text-fill: #2ECC9A; -fx-font-weight: bold;";
                        case "MANAGEMENT" -> "-fx-text-fill: #F6AD55; -fx-font-weight: bold;";
                        case "ERROR"      -> "-fx-text-fill: #FC8181; -fx-font-weight: bold;";
                        default           -> "-fx-text-fill: #A0AEC0;";
                    });
                }
            }
        });

        colUsername.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue()[1] != null ? data.getValue()[1] : "System"));
        colDetails.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));
        colTimestamp.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue()[3]));

        // Load all logs on start
        loadLogs();
    }

    // ── Load all logs ─────────────────────────────────────────────────────────
    private void loadLogs() {
        try {
            List<String[]> logs = logManager.getAllLogs();
            logTable.setItems(FXCollections.observableArrayList(logs));
            logCountLabel.setText(logs.size() + " entries");
        } catch (Exception e) {
            logCountLabel.setText("Could not load logs: " + e.getMessage());
        }
    }

    // ── Apply filters ─────────────────────────────────────────────────────────
    @FXML
    private void handleApplyFilter() {
        List<String> types = new ArrayList<>();

        // Map checkboxes to actual action types in DB
        if (filterLogin.isSelected()) {
            types.add("LOGIN_SUCCESS");
            types.add("LOGIN_FAILED");
        }
        if (filterScanning.isSelected()) {
            types.add("SCAN_COMPLETE");
            types.add("FILE_DELETED");
        }
        if (filterManagement.isSelected()) {
            types.add("USER_CREATED");
            types.add("USER_DEACTIVATED");
            types.add("USER_REACTIVATED");
            types.add("CLIENT_CREATED");
            types.add("CLIENT_UPDATED");
            types.add("CLIENT_DEACTIVATED");
            types.add("CLIENT_REACTIVATED");
        }
        if (filterError.isSelected()) {
            types.add("ERROR");
        }
        if (filterOther.isSelected()) {
            types.add("OTHER");
        }

        String fromDate = fromDatePicker.getValue() != null
                ? fromDatePicker.getValue().toString() : null;
        String toDate = toDatePicker.getValue() != null
                ? toDatePicker.getValue().toString() : null;

        try {
            List<String[]> logs = types.isEmpty() && fromDate == null && toDate == null
                    ? logManager.getAllLogs()
                    : logManager.getFilteredLogs(types, fromDate, toDate);

            logTable.setItems(FXCollections.observableArrayList(logs));
            logCountLabel.setText(logs.size() + " entries");
        } catch (Exception e) {
            logCountLabel.setText("Error: " + e.getMessage());
        }
    }

    // ── Clear filters ─────────────────────────────────────────────────────────
    @FXML
    private void handleClearFilter() {
        filterLogin.setSelected(false);
        filterScanning.setSelected(false);
        filterManagement.setSelected(false);
        filterError.setSelected(false);
        filterOther.setSelected(false);
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        loadLogs();
    }
}