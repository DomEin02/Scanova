package dk.easv.scanova.GUI;

import dk.easv.scanova.BE.*;
import dk.easv.scanova.BLL.ScanSessionService;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.DAL.*;
import dk.easv.scanova.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ScanViewController {

    @FXML private Label statusLabel;
    @FXML private ComboBox<Box> boxComboBox;
    @FXML private TextField documentTitleField;
    @FXML private ListView<ScannedFile> fileListView;

    @FXML private Button moveUpButton;
    @FXML private Button moveDownButton;
    @FXML private Button deleteButton;

    @FXML private Label scanCountLabel;
    @FXML private Label documentCountLabel;

    @FXML private void onMoveUp() {}
    @FXML private void onMoveDown() {}
    @FXML private void onDeleteFile() {}

    private final ScanSessionService scanSessionService =
            new ScanSessionService(new DocumentDAO(), new FileDAO());

    private final ScanSession session = new ScanSession();

    private boolean scanning = false;

    // SESSION START
    @FXML
    private void onStartScan() {

        session.setBox(boxComboBox.getValue());
        session.setCurrentDocument(null);
        statusLabel.setText("Status: Scanning started");
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

    public VBox imagePreviewComponent;

    private void refreshUI() {
        // opdater counters, table osv.
    }
}