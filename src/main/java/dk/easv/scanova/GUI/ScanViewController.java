package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.*;
import dk.easv.scanova.Model.Box;
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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScanViewController {

    @FXML private Label    scanCountLabel;
    @FXML private Label    statusLabel;
    @FXML private Label    documentCountLabel;
    @FXML private ListView<SidebarItem> fileListView;
    @FXML private ImagePreviewController imagePreviewComponentController;
    @FXML private Button   startScanButton;
    @FXML private Button   scanNextButton;
    @FXML private Button   loadPreviousButton;
    @FXML private ComboBox<String> profileComboBox;
    @FXML private TextField boxIdField;

    // ── BLL only — never DAL directly ────────────────────────────────────────
    private final ScanManager         scanManager    = new ScanManager();
    private final LogManager          logManager     = new LogManager();
    private final FileManager         fileManager    = new FileManager();
    private final ScanHistoryManager  historyManager = new ScanHistoryManager();
    private final ExportManager exportManager = new ExportManager();

    private final ObservableList<SidebarItem> sidebarItems =
            FXCollections.observableArrayList();

    private Box selectedBox = null;

    // Stores real DB document ids when history is loaded
    // so reorder can update the pages table correctly
    private Map<Integer, Integer> loadedDocumentIdMap = new HashMap<>();

    // ── Initialize ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        statusLabel.setText("Ready · Logged in as: "
                + SessionManager.getInstance().getCurrentUser().getUsername());

        // Load profiles from DB — only profiles assigned to this user
        try {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            List<String> profiles = scanManager.getProfilesForCurrentUser(userId);
            profileComboBox.getItems().addAll(profiles);
            if (!profiles.isEmpty()) profileComboBox.setValue(profiles.get(0));
            else {
                statusLabel.setText(
                        "Warning: No profiles assigned. Contact an administrator.");
                startScanButton.setDisable(true);
            }
        } catch (Exception e) {
            statusLabel.setText("Could not load profiles: " + e.getMessage());
        }

        // Disable buttons until ready
        startScanButton.setDisable(true);
        scanNextButton.setDisable(true);

        profileComboBox.valueProperty().addListener(
                (obs, old, val) -> checkCanStartScan());
        boxIdField.textProperty().addListener(
                (obs, old, val) -> checkCanStartScan());

        // Setup sidebar with drag and drop
        fileListView.setItems(sidebarItems);
        fileListView.setCellFactory(lv -> {

            ListCell<SidebarItem> cell = new ListCell<>() {
                @Override
                protected void updateItem(SidebarItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else if (item.isHeader()) {
                        setText("📁  Document " + item.getDocumentId());
                        setStyle("-fx-font-weight: bold;" +
                                "-fx-text-fill: #1A2E4A;");
                    } else {
                        setText("    File #" + item.getFile().getFileId()
                                + " (ref: " + item.getFile().getReferenceId()
                                + ", rot: " + item.getFile().getRotation() + "°)");
                        setStyle("");
                    }
                }
            };

            // ── Drag detected ─────────────────────────────────────────────────
            cell.setOnDragDetected(event -> {
                if (cell.isEmpty() || cell.getItem() == null) return;
                if (cell.getItem().isHeader()) return;

                int index = sidebarItems.indexOf(cell.getItem());
                if (index > 0 && sidebarItems.get(index - 1).isHeader()) {
                    statusLabel.setText(
                            "Status: Cannot move the first file of a document.");
                    return;
                }

                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(index));
                db.setContent(content);
                event.consume();
            });

            // ── Drag over ─────────────────────────────────────────────────────
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() == cell) {
                    event.consume();
                    return;
                }
                if (!event.getDragboard().hasString()) {
                    event.consume();
                    return;
                }
                if (cell.isEmpty() || cell.getItem() == null) {
                    event.consume();
                    return;
                }
                if (!cell.getItem().isHeader()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    cell.setStyle("-fx-border-color: #2ECC9A;" +
                            "-fx-border-width: 0 0 2 0;");
                }
                event.consume();
            });

            // ── Drag exited ───────────────────────────────────────────────────
            cell.setOnDragExited(event -> {
                if (cell.getItem() != null && cell.getItem().isHeader()) {
                    cell.setStyle(
                            "-fx-font-weight: bold; -fx-text-fill: #1A2E4A;");
                } else {
                    cell.setStyle("");
                }
                event.consume();
            });

            // ── Drag dropped ──────────────────────────────────────────────────
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (!db.hasString()) {
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }

                int fromIndex = Integer.parseInt(db.getString());
                int toIndex   = cell.isEmpty()
                        ? sidebarItems.size() - 1
                        : sidebarItems.indexOf(cell.getItem());

                if (fromIndex == toIndex) {
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }

                if (cell.getItem() != null && cell.getItem().isHeader()) {
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }

                if (toIndex > 0
                        && sidebarItems.get(toIndex - 1).isHeader()
                        && fromIndex > toIndex) {
                    statusLabel.setText(
                            "Status: Cannot move a file before the barcode.");
                    event.setDropCompleted(false);
                    event.consume();
                    return;
                }

                SidebarItem item = sidebarItems.remove(fromIndex);
                int adjustedTo = fromIndex < toIndex ? toIndex - 1 : toIndex;
                sidebarItems.add(adjustedTo, item);
                fileListView.getSelectionModel().select(adjustedTo);
                fileListView.scrollTo(adjustedTo);
                statusLabel.setText("Status: File moved — saving order...");
                saveOrderToDB();

                event.setDropCompleted(true);
                event.consume();
            });

            // ── Drag done ─────────────────────────────────────────────────────
            cell.setOnDragDone(event -> {
                if (cell.getItem() != null && cell.getItem().isHeader()) {
                    cell.setStyle(
                            "-fx-font-weight: bold; -fx-text-fill: #1A2E4A;");
                } else {
                    cell.setStyle("");
                }
                event.consume();
            });

            return cell;
        });

        // Click file in sidebar → show in ImageView with profile brightness
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
                                    newItem.getFile(),
                                    getProfileBrightness());
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Could not load image — "
                                + e.getMessage());
                    }
                });

        // Register keyboard shortcuts
        fileListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(
                        KeyEvent.KEY_PRESSED, this::handleKeyPress);
            }
        });
    }

    // ── Load Previous Scan ────────────────────────────────────────────────────
    @FXML
    private void onLoadPreviousScan() {
        int userId = SessionManager.getInstance().getCurrentUser().getId();

        List<String[]> cases;
        try {
            cases = historyManager.getCasesForUser(userId);
        } catch (Exception e) {
            statusLabel.setText("Could not load history: " + e.getMessage());
            return;
        }

        if (cases.isEmpty()) {
            statusLabel.setText("Status: No previous scan sessions found.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                formatCase(cases.get(0)),
                cases.stream()
                        .map(this::formatCase)
                        .collect(Collectors.toList()));
        dialog.setTitle("Load Previous Scan");
        dialog.setHeaderText("Select a previous scan session to load:");
        dialog.setContentText("Session:");

        dialog.showAndWait().ifPresent(selected -> {
            String[] selectedCase = cases.stream()
                    .filter(c -> formatCase(c).equals(selected))
                    .findFirst()
                    .orElse(null);

            if (selectedCase == null) return;

            int caseId = Integer.parseInt(selectedCase[0]);
            statusLabel.setText("Status: Loading scan session...");

            int[] fileCounter = {0};

            Task<Boolean> loadTask = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return historyManager.loadCaseIntoSidebar(
                            caseId, sidebarItems, fileCounter);
                }
            };

            loadTask.setOnSucceeded(e -> {
                if (loadTask.getValue()) {
                    // Store document id map for reorder saving
                    loadedDocumentIdMap =
                            historyManager.getLoadedDocumentIdMap();
                    statusLabel.setText("Status: Loaded "
                            + fileCounter[0] + " files from "
                            + selectedCase[2]);
                    scanNextButton.setDisable(true);
                    startScanButton.setDisable(false);
                } else {
                    statusLabel.setText(
                            "Status: No files found in this session.");
                }
            });

            loadTask.setOnFailed(e ->
                    statusLabel.setText("Could not load session: "
                            + loadTask.getException().getMessage()));

            Thread thread = new Thread(loadTask);
            thread.setDaemon(true);
            thread.start();
        });
    }

    // ── Format case for dialog ────────────────────────────────────────────────
    private String formatCase(String[] c) {
        return "Box: " + c[2]  // box label
                + " | " + c[1] // title
                + " | " + c[3]; // status
    }

    // ── Save current sidebar order to DB ─────────────────────────────────────
    private void saveOrderToDB() {
        List<SidebarItem> snapshot = List.copyOf(sidebarItems);

        new Thread(() -> {
            int order = 1;
            for (SidebarItem item : snapshot) {
                if (!item.isHeader()) {
                    ScannedFile file = item.getFile();

                    // Update files table if we have a real DB file id
                    if (file.getDbFileId() != -1) {
                        fileManager.updateFileOrder(
                                file.getDbFileId(), order);
                    }

                    // Update pages table
                    int docId = getRealDocumentId(file.getDocumentId());
                    if (docId != -1) {
                        fileManager.updatePageOrder(
                                file.getReferenceId(), docId, order);
                    }

                    order++;
                }
            }
            Platform.runLater(() ->
                    statusLabel.setText("Status: Order saved."));
        }).start();
    }

    // ── Get real DB document id ───────────────────────────────────────────────
    // Works for both live scan and loaded history
    private int getRealDocumentId(int docNumber) {
        // Try scanManager first (live scan)
        int fromScan = scanManager.getRealDocumentId(docNumber);
        if (fromScan != -1) return fromScan;

        // Fall back to loaded history map
        return loadedDocumentIdMap.getOrDefault(docNumber, -1);
    }

    // ── Get profile brightness ────────────────────────────────────────────────
    private double getProfileBrightness() {
        return (scanManager.getActiveBox() != null)
                ? scanManager.getActiveBox().getBrightness()
                : 1.0;
    }

    // ── Keyboard shortcuts ────────────────────────────────────────────────────
    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case F1 -> {
                if (!scanNextButton.isDisabled()) onScanNext();
                else if (!startScanButton.isDisabled()) onStartScan();
                event.consume();
            }
            case F2 -> { onStopScan(); event.consume(); }
            case F3 -> { onOpenSlideshow(); event.consume(); }
            case F4 -> { onExport(); event.consume(); }
            case DELETE -> { onDeleteFile(); event.consume(); }
            case UP -> { onMoveUp(); event.consume(); }
            case DOWN -> { onMoveDown(); event.consume(); }
            default -> {}
        }
    }

    // ── Check if scan can start ───────────────────────────────────────────────
    private void checkCanStartScan() {
        boolean hasProfile = profileComboBox.getValue() != null;
        boolean hasBox     = boxIdField.getText() != null
                && !boxIdField.getText().isBlank();
        startScanButton.setDisable(!(hasProfile && hasBox));
    }

    // ── Check if document header already exists ───────────────────────────────
    private boolean headerExists(int documentId) {
        return sidebarItems.stream()
                .anyMatch(i -> i.isHeader()
                        && i.getDocumentId() == documentId);
    }

    // ── Start Session ─────────────────────────────────────────────────────────
    @FXML
    private void onStartScan() {
        String boxLabel    = boxIdField.getText().trim();
        String profileName = profileComboBox.getValue();

        try {
            selectedBox = scanManager.validateAndPrepareSession(
                    boxLabel, profileName);
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
            return;
        }

        try {
            scanManager.initSession(selectedBox);
        } catch (Exception e) {
            statusLabel.setText("Error starting session: " + e.getMessage());
            return;
        }

        sidebarItems.clear();
        loadedDocumentIdMap.clear();
        startScanButton.setDisable(true);
        scanNextButton.setDisable(false);
        scanCountLabel.setText("Scans: 0 / " + scanManager.getTotalAvailable());
        statusLabel.setText("Status: Session started"
                + " | Profile: " + selectedBox.getProfileName()
                + " | Auto-rotation: " + (int) selectedBox.getRotation() + "°"
                + " | Brightness: " + selectedBox.getBrightness()
                + " — press Scan Next or F1");
    }

    // ── Scan Next ─────────────────────────────────────────────────────────────
    @FXML
    private void onScanNext() {
        if (!scanManager.hasMore()) {
            scanNextButton.setDisable(true);
            startScanButton.setDisable(false);
            statusLabel.setText("Status: All files scanned!");

            String summary = "Box: " + selectedBox.getLabel()
                    + " | Profile: " + selectedBox.getProfileName()
                    + " | Documents: " + scanManager.getAllDocuments().size()
                    + " | Pages: " + scanManager.getTotalFilesFetched();
            logManager.log("SCAN_COMPLETE",
                    SessionManager.getInstance().getCurrentUser().getId(),
                    summary);
            return;
        }

        Task<Void> fetchTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<ScannedFile> fetched = scanManager.fetchNext();
                if (fetched == null || fetched.isEmpty()) return null;

                double brightness = getProfileBrightness();

                for (ScannedFile file : fetched) {
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
                                imagePreviewComponentController.setImage(
                                        SwingFXUtils.toFXImage(buffered, null),
                                        file,
                                        brightness);
                            }
                        } catch (Exception e) {
                            statusLabel.setText("Could not display image — "
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

                        if (!scanManager.hasMore()) {
                            scanNextButton.setDisable(true);
                            startScanButton.setDisable(false);
                            statusLabel.setText("Status: Done — "
                                    + scanManager.getTotalFilesFetched()
                                    + " files scanned");
                        }
                    });
                }
                return null;
            }
        };

        fetchTask.setOnFailed(e -> Platform.runLater(() ->
                statusLabel.setText("Error: "
                        + fetchTask.getException().getMessage())));

        Thread thread = new Thread(fetchTask);
        thread.setDaemon(true);
        thread.start();
    }

    // Add this method:
// ── Export TIFF ───────────────────────────────────────────────────────────
    @FXML
    private void onExport() {
        if (sidebarItems.isEmpty()) {
            statusLabel.setText("Status: Nothing to export — scan first.");
            return;
        }

        // Need a box to know the export folder name
        if (selectedBox == null) {
            statusLabel.setText("Status: No active session — scan first.");
            return;
        }

        // Ask user: single-page or multi-page
        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(
                        "Single-page TIFF",
                        "Single-page TIFF", "Multi-page TIFF");
        dialog.setTitle("Export TIFF");
        dialog.setHeaderText("Choose export format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(choice -> {
            boolean multiPage = choice.equals("Multi-page TIFF");
            String folderName = selectedBox.getExportFolderName();

            statusLabel.setText("Status: Exporting...");

            // Run export on background thread — API calls are slow
            javafx.concurrent.Task<Integer> exportTask =
                    new javafx.concurrent.Task<>() {
                        @Override
                        protected Integer call() throws Exception {
                            if (multiPage) {
                                return exportManager.exportMultiPage(
                                        sidebarItems, folderName);
                            } else {
                                return exportManager.exportSinglePage(
                                        sidebarItems, folderName);
                            }
                        }
                    };

            exportTask.setOnSucceeded(e -> {
                int count = exportTask.getValue();
                String type = multiPage ? "documents" : "files";
                statusLabel.setText("Status: Exported " + count
                        + " " + type + " to "
                        + System.getProperty("user.home")
                        + "/Scanova_Exports/"
                        + folderName);

                // Log the export
                logManager.log(
                        "SCAN_COMPLETE",
                        SessionManager.getInstance().getCurrentUser().getId(),
                        "Exported " + count + " " + type
                                + " | Box: " + selectedBox.getLabel()
                                + " | Format: " + choice);
            });

            exportTask.setOnFailed(e ->
                    statusLabel.setText("Export failed: "
                            + exportTask.getException().getMessage()));

            Thread thread = new Thread(exportTask);
            thread.setDaemon(true);
            thread.start();
        });
    }

    // ── Stop Scan ─────────────────────────────────────────────────────────────
    @FXML
    private void onStopScan() {
        scanNextButton.setDisable(true);
        startScanButton.setDisable(false);
        statusLabel.setText("Status: Stopped");
    }

    // ── Move Up ───────────────────────────────────────────────────────────────
    @FXML
    private void onMoveUp() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index <= 0) return;

        SidebarItem selected = sidebarItems.get(index);
        if (selected.isHeader()) return;

        SidebarItem above = sidebarItems.get(index - 1);
        if (above.isHeader()) {
            statusLabel.setText(
                    "Status: Cannot move the first file above its document.");
            return;
        }

        sidebarItems.remove(index);
        sidebarItems.add(index - 1, selected);
        fileListView.getSelectionModel().select(index - 1);
        fileListView.scrollTo(index - 1);
        statusLabel.setText("Status: File moved up — saving...");
        saveOrderToDB();
    }

    // ── Move Down ─────────────────────────────────────────────────────────────
    @FXML
    private void onMoveDown() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= sidebarItems.size() - 1) return;

        SidebarItem selected = sidebarItems.get(index);
        if (selected.isHeader()) return;

        SidebarItem below = sidebarItems.get(index + 1);

        if (below.isHeader()) {
            if (index + 2 >= sidebarItems.size()) {
                statusLabel.setText(
                        "Status: Cannot move below last document header.");
                return;
            }
            sidebarItems.remove(index);
            sidebarItems.add(index + 2, selected);
            fileListView.getSelectionModel().select(index + 2);
            fileListView.scrollTo(index + 2);
        } else {
            sidebarItems.remove(index);
            sidebarItems.add(index + 1, selected);
            fileListView.getSelectionModel().select(index + 1);
            fileListView.scrollTo(index + 1);
        }

        statusLabel.setText("Status: File moved down — saving...");
        saveOrderToDB();
    }

    // ── Delete File ───────────────────────────────────────────────────────────
    @FXML
    private void onDeleteFile() {
        SidebarItem selected =
                fileListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isHeader()) {
            statusLabel.setText("Status: Select a file to delete");
            return;
        }
        sidebarItems.remove(selected);
        logManager.log("FILE_DELETED",
                SessionManager.getInstance().getCurrentUser().getId(),
                "File #" + selected.getFile().getFileId() + " removed");
        statusLabel.setText("Status: File removed");
    }

    // ── Open Slideshow ────────────────────────────────────────────────────────
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

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }
}