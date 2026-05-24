package dk.easv.scanova.GUI;

import dk.easv.scanova.BE.*;
import dk.easv.scanova.BLL.ScanSessionService;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.DAL.*;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import dk.easv.scanova.BE.ScannedFile;

public class ScanViewController {

    @FXML private Label statusLabel;
    @FXML private ComboBox<Box> profileComboBox;
    @FXML private TextField documentTitleField;
    @FXML private ListView<ScannedFile> fileListView;

    @FXML public Button moveUpButton;
    @FXML public Button moveDownButton;
    @FXML public Button deleteButton;

    @FXML public Label scanCountLabel;
    @FXML public Label documentCountLabel;

    public VBox imagePreviewComponent;

    private final ScanSessionService scanSessionService =
            new ScanSessionService(new DocumentDAO(), new FileDAO());

    private final ScanSession session = new ScanSession();

    private boolean scanning = false;

    @FXML
    public void initialize() {
        setupListView();
    }

    // SESSION START
    @FXML
    private void onStartScan() {

        session.setBox(profileComboBox.getValue());
        session.setCurrentDocument(null);
        statusLabel.setText("Status: Scanning started");
    }

    //  STOP
    @FXML
    private void onStopScan() {
        scanning = false;
        statusLabel.setText("Status: Stopped");
    }

    // LOGOUT
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }

    @FXML
    private void onOpenSlideshow() {
        System.out.println("Slideshow not implemented yet");
    }

    // SCAN EVENT
    @FXML
    private void onFileScanned(ScannedFile file) {

        scanSessionService.handleIncomingFile(session, file);

        refreshUI();
    }

    // UI
    private void setupListView() {

        fileListView.setCellFactory(list -> new ListCell<>() {

            @Override
            protected void updateItem(ScannedFile file, boolean empty) {
                super.updateItem(file, empty);

                if (empty || file == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText("File #" + file.getFileOrderId());
            }
        });
    }

    @FXML
    public void onMoveUp() {
        System.out.println("Move up clicked");
    }

    @FXML
    public void onMoveDown() {
        System.out.println("Move down clicked");
    }

    @FXML
    public void onDeleteFile() {
        System.out.println("Delete clicked");
    }

    private void refreshUI() {

        Document doc = session.getCurrentDocument();

        if (doc == null) return;

        // SHOW FILES
        fileListView.setItems(
                javafx.collections.FXCollections.observableArrayList(
                        doc.getFiles()
                )
        );

        // SHOW STATUS (DOCUMENT LEVEL)
        statusLabel.setText("Status: " + doc.getStatus());

        switch (doc.getStatus()) {

            case IN_PROGRESS ->
                    statusLabel.setStyle("-fx-background-color: #fff3a0;");

            case WAITING_FOR_QA ->
                    statusLabel.setStyle("-fx-background-color: #ff9a9a;");

            case QA_COMPLETED ->
                    statusLabel.setStyle("-fx-background-color: #9aff9a;");

            case EXPORTED ->
                    statusLabel.setStyle("-fx-background-color: #9ac7ff;");
        }
        fileListView.refresh();
    }
}