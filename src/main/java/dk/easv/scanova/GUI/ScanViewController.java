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

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        // Use getUsername() since Elena's SessionManager stores a User object
        statusLabel.setText("Ready · Logged in as: "
                + SessionManager.getInstance().getCurrentUser().getUsername());

        // Load hardcoded profiles — Sprint 3 loads from DB
        profileComboBox.getItems().addAll("Default", "WebLager_Standard");

        // Disable start scan until both profile and box are selected
        startScanButton.setDisable(true);
        profileComboBox.valueProperty().addListener((obs, old, val) -> checkCanStartScan());
        boxIdField.textProperty().addListener((obs, old, val) -> checkCanStartScan());

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

        fileListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldItem, newItem) -> {
                    if (newItem == null || newItem.isHeader()) return;
                    try {
                        BufferedImage buffered = ImageIO.read(
                                new ByteArrayInputStream(newItem.getFile().getImageData()));
                        if (buffered != null) {
                            Image image = SwingFXUtils.toFXImage(buffered, null);
                            imagePreviewComponentController.setImage(image, newItem.getFile());
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Could not load image — " + e.getMessage());
                    }
                });
    }

    // ── Check if scan can start ───────────────────────────────────────────────
    private void checkCanStartScan() {
        boolean hasProfile = profileComboBox.getValue() != null;
        boolean hasBox = boxIdField.getText() != null
                && !boxIdField.getText().isBlank();
        startScanButton.setDisable(!(hasProfile && hasBox));
    }

    // ── Check if document header already exists in sidebar ───────────────────
    private boolean headerExists(int documentId) {
        return sidebarItems.stream()
                .anyMatch(i -> i.isHeader() && i.getDocumentId() == documentId);
    }

    // ── Start Scan ────────────────────────────────────────────────────────────
    @FXML
    private void onStartScan() {
        scanning = true;
        statusLabel.setText("Status: Waiting for first barcode...");
        sidebarItems.clear();

        Task<Void> scanTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                // Clear old pages from DB before new scan
                try {
                    pageDAO.clearPages();
                } catch (Exception e) {
                    System.out.println("Could not clear pages: " + e.getMessage());
                }

                scanManager.initSession();

                Platform.runLater(() -> {
                    scanCountLabel.setText("Scans: 0 / " + scanManager.getTotalAvailable());
                    statusLabel.setText("Status: Waiting for first barcode...");
                });

                while (scanManager.hasMore() && scanning) {
                    List<ScannedFile> fetched = scanManager.fetchNext();
                    if (fetched == null) break;

                    for (ScannedFile file : fetched) {

                        // Save to DB on separate thread — never blocks scanning
                        new Thread(() -> {
                            try {
                                pageDAO.insertPage(file);
                            } catch (Exception e) {
                                System.out.println("DB save failed: " + e.getMessage());
                            }
                        }).start();

                        Platform.runLater(() -> {
                            // Add document header only once
                            if (!headerExists(file.getDocumentId())) {
                                sidebarItems.add(new SidebarItem(file.getDocumentId()));
                            }

                            // Add file to sidebar
                            sidebarItems.add(new SidebarItem(file));

                            // Update status once files start appearing
                            statusLabel.setText("Status: Scanning...");

                            // Display TIFF in ImageView
                            try {
                                BufferedImage buffered = ImageIO.read(
                                        new ByteArrayInputStream(file.getImageData()));
                                if (buffered != null) {
                                    Image image = SwingFXUtils.toFXImage(buffered, null);
                                    imagePreviewComponentController.setImage(image, file);
                                }
                            } catch (Exception e) {
                                statusLabel.setText("Could not display image — " + e.getMessage());
                            }

                            // Update counters
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
                                + scanManager.getTotalFilesFetched() + " files scanned"));
                return null;
            }
        };

        scanTask.setOnFailed(e -> Platform.runLater(() ->
                statusLabel.setText("Status: Error — " + scanTask.getException().getMessage())));

        Thread thread = new Thread(scanTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ── Stop Scan ─────────────────────────────────────────────────────────────
    @FXML
    private void onStopScan() {
        scanning = false;
        statusLabel.setText("Status: Stopped");
    }

    // ── Open Slideshow Review Mode ────────────────────────────────────────────
    @FXML
    private void onOpenSlideshow() {
        List<ScannedFile> allFiles = sidebarItems.stream()
                .filter(item -> !item.isHeader())
                .map(SidebarItem::getFile)
                .collect(Collectors.toList());

        if (allFiles.isEmpty()) {
            statusLabel.setText("Status: No files to review. Start a scan first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dk/easv/scanova/slideshowView.fxml"));
            Parent root = loader.load();

            SlideshowController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Scanova — Review Mode");
            stage.setScene(new Scene(root));
            stage.show();
            stage.requestFocus();

            // Pass files AFTER show() so scene is available for keyboard shortcuts
            controller.setFiles(allFiles);

        } catch (Exception e) {
            statusLabel.setText("Could not open review mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }
}