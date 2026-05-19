package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ScanManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.DAL.PageDAO;
import dk.easv.scanova.Model.ScannedFile;
import dk.easv.scanova.Model.SidebarItem;
import dk.easv.scanova.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

public class ScanViewController {

    @FXML private Label scanCountLabel;
    @FXML private Label statusLabel;
    @FXML private Label documentCountLabel;
    @FXML private ListView<SidebarItem> fileListView;
    @FXML private ImagePreviewController imagePreviewComponentController;
    @FXML private Button startScanButton;
    @FXML private ComboBox<String> profileComboBox;
    @FXML private TextField boxIdField;

    private final ObservableList<SidebarItem> sidebarItems = FXCollections.observableArrayList();
    private final ScanManager scanManager = new ScanManager();
    private final PageDAO pageDAO = new PageDAO();
    private volatile boolean scanning = false;

    // Initialize
    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        statusLabel.setText("Ready · Logged in as: "
                + SessionManager.getInstance().getCurrentUser().getUsername());

        // Hardcoded profiles — Sprint 3 loads from DB
        profileComboBox.getItems().addAll("Default", "WebLager_Standard");

        // Disable start scan until both profile and box are selected
        startScanButton.setDisable(true);
        profileComboBox.valueProperty().addListener(
                (obs, old, val) -> checkCanStartScan());
        boxIdField.textProperty().addListener(
                (obs, old, val) -> checkCanStartScan());

        // Setup sidebar
        fileListView.setItems(sidebarItems);
        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SidebarItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item.isHeader()) {
                    setText("📁  Document " + item.getDocumentId());
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1A2E4A;");
                } else {
                    setText("    File #" + item.getFile().getFileId());
                    setStyle("");
                }
            }
        });

        // Click file in sidebar → show in ImageView
        fileListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldItem, newItem) -> {
                    if (newItem == null || newItem.isHeader()) return;
                    try {
                        BufferedImage buffered = ImageIO.read(
                                new ByteArrayInputStream(
                                        newItem.getFile().getImageData()));
                        if (buffered != null) {
                            imagePreviewComponentController.setImage(
                                    SwingFXUtils.toFXImage(buffered, null),
                                    newItem.getFile());
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Could not load image — "
                                + e.getMessage());
                    }
                });

        // ── Register keyboard shortcuts reliably via sceneProperty ────────────
        // sceneProperty fires exactly when the node is attached to a scene
        // — more reliable than Platform.runLater()
        fileListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(
                        KeyEvent.KEY_PRESSED, this::handleKeyPress);
            }
        });
    }

    // Keyboard shortcuts
    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case F1 -> {
                // Only start if profile and box are selected
                if (!startScanButton.isDisabled()) onStartScan();
                event.consume();
            }
            case F2 -> {
                onStopScan();
                event.consume();
            }
            case F3 -> {
                onOpenSlideshow();
                event.consume();
            }
            case F4 -> {
                statusLabel.setText("Status: Export — coming soon");
                event.consume();
            }
            case DELETE -> {
                onDeleteFile();
                event.consume();
            }
            case UP -> {
                int i = fileListView.getSelectionModel().getSelectedIndex();
                if (i > 0) {
                    fileListView.getSelectionModel().select(i - 1);
                    fileListView.scrollTo(i - 1);
                }
                event.consume();
            }
            case DOWN -> {
                int i = fileListView.getSelectionModel().getSelectedIndex();
                if (i < sidebarItems.size() - 1) {
                    fileListView.getSelectionModel().select(i + 1);
                    fileListView.scrollTo(i + 1);
                }
                event.consume();
            }
            default -> {}
        }
    }

    // Check if scan can start
    private void checkCanStartScan() {
        boolean hasProfile = profileComboBox.getValue() != null;
        boolean hasBox = boxIdField.getText() != null
                && !boxIdField.getText().isBlank();
        startScanButton.setDisable(!(hasProfile && hasBox));
    }

    // Check if document header already exists
    private boolean headerExists(int documentId) {
        return sidebarItems.stream()
                .anyMatch(i -> i.isHeader() && i.getDocumentId() == documentId);
    }

    // Move Up
    @FXML
    private void onMoveUp() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index <= 0) return;
        SidebarItem item = sidebarItems.remove(index);
        sidebarItems.add(index - 1, item);
        fileListView.getSelectionModel().select(index - 1);
    }

    // Move Down
    @FXML
    private void onMoveDown() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= sidebarItems.size() - 1) return;
        SidebarItem item = sidebarItems.remove(index);
        sidebarItems.add(index + 1, item);
        fileListView.getSelectionModel().select(index + 1);
    }

    // Delete File
    @FXML
    private void onDeleteFile() {
        SidebarItem selected =
                fileListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isHeader()) {
            statusLabel.setText("Status: Select a file to delete");
            return;
        }
        sidebarItems.remove(selected);
        statusLabel.setText("Status: File removed");
    }

    // Start Scan
    @FXML
    private void onStartScan() {
        scanning = true;
        statusLabel.setText("Status: Waiting for first barcode...");
        sidebarItems.clear();

        Task<Void> scanTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                scanManager.setCurrentBoxId(boxIdField.getText());

                try {
                    pageDAO.clearPages();
                } catch (Exception e) {
                    System.out.println("Could not clear pages: " + e.getMessage());
                }

                scanManager.initSession();

                Platform.runLater(() -> {
                    scanCountLabel.setText(
                            "Scans: 0 / " + scanManager.getTotalAvailable());
                    statusLabel.setText("Status: Waiting for first barcode...");
                });

                while (scanManager.hasMore() && scanning) {
                    List<ScannedFile> fetched = scanManager.fetchNext();
                    if (fetched == null) break;

                    for (ScannedFile file : fetched) {

                        new Thread(() -> {
                            try {
                                pageDAO.insertPage(file);
                            } catch (Exception e) {
                                System.out.println("DB save failed: "
                                        + e.getMessage());
                            }
                        }).start();

                        Platform.runLater(() -> {
                            if (!headerExists(file.getDocumentId())) {
                                sidebarItems.add(
                                        new SidebarItem(file.getDocumentId()));
                            }
                            sidebarItems.add(new SidebarItem(file));

                            statusLabel.setText("Status: Scanning...");

                            try {
                                BufferedImage buffered = ImageIO.read(
                                        new ByteArrayInputStream(
                                                file.getImageData()));
                                if (buffered != null) {
                                    Image image = SwingFXUtils.toFXImage(
                                            buffered, null);
                                    imagePreviewComponentController.setImage(
                                            image, file);
                                }
                            } catch (Exception e) {
                                statusLabel.setText(
                                        "Could not display image — "
                                                + e.getMessage());
                            }

                            int totalScans = scanManager.getAllDocuments()
                                    .stream()
                                    .mapToInt(d -> d.getFiles().size())
                                    .sum();
                            scanCountLabel.setText("Scans: " + totalScans
                                    + " / " + scanManager.getTotalAvailable());
                            documentCountLabel.setText("Documents: "
                                    + scanManager.getAllDocuments().size());
                        });
                    }
                }

                Platform.runLater(() ->
                        statusLabel.setText("Status: Done — "
                                + scanManager.getTotalFilesFetched()
                                + " files scanned"));
                return null;
            }
        };

        scanTask.setOnFailed(e -> Platform.runLater(() ->
                statusLabel.setText("Status: Error — "
                        + scanTask.getException().getMessage())));

        Thread thread = new Thread(scanTask);
        thread.setDaemon(true);
        thread.start();
    }

    // Stop Scan
    @FXML
    private void onStopScan() {
        scanning = false;
        statusLabel.setText("Status: Stopped");
    }

    // Open Slideshow
    @FXML
    private void onOpenSlideshow() {
        List<ScannedFile> allFiles = sidebarItems.stream()
                .filter(item -> !item.isHeader())
                .map(SidebarItem::getFile)
                .collect(Collectors.toList());

        if (allFiles.isEmpty()) {
            statusLabel.setText(
                    "Status: No files to review. Start a scan first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/dk/easv/scanova/slideshowView.fxml"));
            Parent root = loader.load();
            SlideshowController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Scanova — Review Mode");
            stage.setScene(new Scene(root));
            stage.show();
            stage.requestFocus();
            controller.setFiles(allFiles);
        } catch (Exception e) {
            statusLabel.setText("Could not open review mode: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    // Logout
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }
}