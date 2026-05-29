package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ExportManager;
import dk.easv.scanova.BLL.FileManager;
import dk.easv.scanova.BLL.LogManager;
import dk.easv.scanova.BLL.ScanHistoryManager;
import dk.easv.scanova.BLL.ScanManager;
import dk.easv.scanova.BLL.SessionManager;
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

    private final ScanManager        scanManager    = new ScanManager();
    private final LogManager         logManager     = new LogManager();
    private final FileManager        fileManager    = new FileManager();
    private final ScanHistoryManager historyManager = new ScanHistoryManager();
    private final ExportManager      exportManager  = new ExportManager();

    private final ObservableList<SidebarItem> sidebarItems =
            FXCollections.observableArrayList();

    private Box selectedBox = null;

    // Stores real DB document ids when history is loaded
    private Map<Integer, Integer> loadedDocumentIdMap = new HashMap<>();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        statusLabel.setText("Ready · Press F5 to start a new session — Logged in as: "
                + SessionManager.getInstance().getCurrentUser().getUsername());

        startScanButton.setDisable(false);
        scanNextButton.setDisable(true);

        // Sidebar list
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
                        setStyle("-fx-font-weight: bold;"
                                + "-fx-text-fill: #1A2E4A;");
                    } else {
                        setText("    File #" + item.getFile().getFileId()
                                + " (ref: " + item.getFile().getReferenceId()
                                + ", rot: " + item.getFile().getRotation() + "°)");
                        setStyle("");
                    }
                }
            };

            // Drag detected — only files, not headers
            cell.setOnDragDetected(event -> {
                if (cell.isEmpty() || cell.getItem() == null) return;
                if (cell.getItem().isHeader()) return;
                int index = sidebarItems.indexOf(cell.getItem());
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(index));
                db.setContent(content);
                event.consume();
            });

            // Drag over — accept on files AND headers
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() == cell) {
                    event.consume(); return;
                }
                if (!event.getDragboard().hasString()
                        || cell.isEmpty() || cell.getItem() == null) {
                    event.consume(); return;
                }
                event.acceptTransferModes(TransferMode.MOVE);
                cell.setStyle(cell.getItem().isHeader()
                        ? "-fx-font-weight: bold; -fx-text-fill: #1A2E4A;"
                        + "-fx-border-color: #2ECC9A; -fx-border-width: 0 0 2 0;"
                        : "-fx-border-color: #2ECC9A; -fx-border-width: 0 0 2 0;");
                event.consume();
            });

            // Drag exited
            cell.setOnDragExited(event -> {
                cell.setStyle(cell.getItem() != null && cell.getItem().isHeader()
                        ? "-fx-font-weight: bold; -fx-text-fill: #1A2E4A;" : "");
                event.consume();
            });

            // Drag dropped — allow cross-document moves
            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (!db.hasString()) {
                    event.setDropCompleted(false); event.consume(); return;
                }

                int fromIndex = Integer.parseInt(db.getString());
                int toIndex   = cell.isEmpty()
                        ? sidebarItems.size() - 1
                        : sidebarItems.indexOf(cell.getItem());

                if (fromIndex == toIndex) {
                    event.setDropCompleted(false); event.consume(); return;
                }

                SidebarItem dragged = sidebarItems.get(fromIndex);

                // Dropping onto a header → place right after it
                int insertAt = (cell.getItem() != null && cell.getItem().isHeader())
                        ? toIndex + 1 : toIndex;

                sidebarItems.remove(fromIndex);
                if (fromIndex < insertAt) insertAt--;
                insertAt = Math.max(0, Math.min(insertAt, sidebarItems.size()));
                sidebarItems.add(insertAt, dragged);

                // Update file's document id
                int newDocId = findDocumentIdForPosition(insertAt);
                if (newDocId != -1 && !dragged.isHeader())
                    dragged.getFile().setDocumentId(newDocId);

                fileListView.getSelectionModel().select(insertAt);
                fileListView.scrollTo(insertAt);
                statusLabel.setText("Status: File moved — saving order...");
                saveOrderToDB();

                event.setDropCompleted(true);
                event.consume();
            });

            // Drag done
            cell.setOnDragDone(event -> {
                cell.setStyle(cell.getItem() != null && cell.getItem().isHeader()
                        ? "-fx-font-weight: bold; -fx-text-fill: #1A2E4A;" : "");
                event.consume();
            });

            return cell;
        });

        // Click file → show in preview
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
            if (newScene != null)
                newScene.addEventFilter(KeyEvent.KEY_PRESSED,
                        this::handleKeyPress);
        });
    }

    // Keyboard shortcuts
    private void handleKeyPress(KeyEvent event) {
        boolean textFocused =
                fileListView.getScene() != null
                        && (fileListView.getScene().getFocusOwner()
                        instanceof TextField
                        || fileListView.getScene().getFocusOwner()
                        instanceof PasswordField);

        switch (event.getCode()) {
            case F1 -> {
                if (!scanNextButton.isDisabled()) onScanNext();
                event.consume();
            }
            case F2 -> { onStopScan();       event.consume(); }
            case F3 -> { onOpenSlideshow();  event.consume(); }
            case F4 -> { onExport();         event.consume(); }
            case F5 -> { onStartScan();      event.consume(); }
            case M  -> {
                if (!textFocused) { onManualSplit();   event.consume(); }
            }
            case G  -> {
                if (!textFocused) { onMergeDocument(); event.consume(); }
            }
            case DELETE -> {
                if (!textFocused) { onDeleteFile();    event.consume(); }
            }
            case UP -> {
                if (!textFocused) { onMoveUp();        event.consume(); }
            }
            case DOWN -> {
                if (!textFocused) { onMoveDown();      event.consume(); }
            }
            default -> {}
        }
    }

    // Start Session (F5) — popup dialog
    @FXML
    private void onStartScan() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Start Scan Session");
        dialog.setHeaderText("Select box and profile to begin scanning");

        ComboBox<String> boxCombo     = new ComboBox<>();
        ComboBox<String> profileCombo = new ComboBox<>();
        boxCombo.setPromptText("Select a box");
        boxCombo.setPrefWidth(260);
        profileCombo.setPromptText("Select a profile");
        profileCombo.setPrefWidth(260);

        try {
            List<String> boxes = scanManager.getAvailableBoxLabels();
            boxCombo.getItems().addAll(boxes);
            if (!boxes.isEmpty()) boxCombo.setValue(boxes.get(0));
        } catch (Exception e) {
            statusLabel.setText("Could not load boxes: " + e.getMessage());
            return;
        }

        try {
            int userId = SessionManager.getInstance().getCurrentUser().getId();
            List<String> profiles =
                    scanManager.getProfilesForCurrentUser(userId);
            profileCombo.getItems().addAll(profiles);
            if (!profiles.isEmpty())
                profileCombo.setValue(profiles.get(0));
            else {
                statusLabel.setText(
                        "Warning: No profiles assigned. Contact an administrator.");
                return;
            }
        } catch (Exception e) {
            statusLabel.setText("Could not load profiles: " + e.getMessage());
            return;
        }

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
                new Label("Box:"),    boxCombo,
                new Label("Profile:"), profileCombo);
        content.setStyle("-fx-padding: 16;");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL);

        javafx.scene.Node okBtn =
                dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(boxCombo.getValue() == null
                || profileCombo.getValue() == null);
        boxCombo.valueProperty().addListener((o, ov, nv) ->
                okBtn.setDisable(nv == null || profileCombo.getValue() == null));
        profileCombo.valueProperty().addListener((o, ov, nv) ->
                okBtn.setDisable(nv == null || boxCombo.getValue() == null));

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            try {
                selectedBox = scanManager.validateAndPrepareSession(
                        boxCombo.getValue(), profileCombo.getValue());
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
                    + " | Box: " + selectedBox.getLabel()
                    + " | Profile: " + selectedBox.getProfileName()
                    + " — press F1 or Scan Next");
        });
    }

    // Scan Next (F1)
    @FXML
    private void onScanNext() {
        if (!scanManager.hasMore()) {
            scanNextButton.setDisable(true);
            startScanButton.setDisable(false);
            statusLabel.setText("Status: All files scanned!");
            if (selectedBox != null) {
                String summary = "Box: " + selectedBox.getLabel()
                        + " | Profile: " + selectedBox.getProfileName()
                        + " | Documents: " + scanManager.getAllDocuments().size()
                        + " | Pages: " + scanManager.getTotalFilesFetched();
                logManager.log("SCAN_COMPLETE",
                        SessionManager.getInstance().getCurrentUser().getId(),
                        summary);
            }
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
                        if (!headerExists(file.getDocumentId()))
                            sidebarItems.add(new SidebarItem(file.getDocumentId()));
                        sidebarItems.add(new SidebarItem(file));
                        statusLabel.setText("Status: Scanning...");

                        try {
                            BufferedImage buffered = ImageIO.read(
                                    new ByteArrayInputStream(file.getImageData()));
                            if (buffered != null) {
                                imagePreviewComponentController.setImage(
                                        SwingFXUtils.toFXImage(buffered, null),
                                        file, brightness);
                            }
                        } catch (Exception e) {
                            statusLabel.setText("Could not display image — "
                                    + e.getMessage());
                        }

                        int totalScans = scanManager.getAllDocuments()
                                .stream().mapToInt(d -> d.getFiles().size()).sum();
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

        Thread t = new Thread(fetchTask);
        t.setDaemon(true);
        t.start();
    }

    // Stop Scan (F2)
    @FXML
    private void onStopScan() {
        scanNextButton.setDisable(true);
        startScanButton.setDisable(false);
        statusLabel.setText("Status: Stopped");
    }

    // Manual Split (M) — insert new document header at selected file
    @FXML
    private void onManualSplit() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            statusLabel.setText("Status: Select a file to split from.");
            return;
        }
        SidebarItem selected = sidebarItems.get(index);
        if (selected.isHeader()) {
            statusLabel.setText("Status: Select a file, not a document header.");
            return;
        }

        // New doc id = highest existing + 1
        int newDocId = sidebarItems.stream()
                .filter(SidebarItem::isHeader)
                .mapToInt(SidebarItem::getDocumentId)
                .max().orElse(0) + 1;

        sidebarItems.add(index, new SidebarItem(newDocId));

        // Reassign following files until next header
        for (int i = index + 1; i < sidebarItems.size(); i++) {
            SidebarItem item = sidebarItems.get(i);
            if (item.isHeader()) break;
            item.getFile().setDocumentId(newDocId);
        }

        fileListView.getSelectionModel().select(index);
        statusLabel.setText("Status: Document split — saving...");
        saveOrderToDB();
    }

    // Merge Document (G) — merge selected header into previous document
    @FXML
    private void onMergeDocument() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index <= 0) {
            statusLabel.setText(
                    "Status: Select a document header (📁) to merge up.");
            return;
        }
        SidebarItem selected = sidebarItems.get(index);
        if (!selected.isHeader()) {
            statusLabel.setText(
                    "Status: Select a document header (📁) to merge.");
            return;
        }

        int prevDocId = -1;
        for (int i = index - 1; i >= 0; i--) {
            if (sidebarItems.get(i).isHeader()) {
                prevDocId = sidebarItems.get(i).getDocumentId();
                break;
            }
        }
        if (prevDocId == -1) {
            statusLabel.setText("Status: Cannot merge — no previous document.");
            return;
        }

        sidebarItems.remove(index);

        final int targetDocId = prevDocId;
        for (int i = index; i < sidebarItems.size(); i++) {
            SidebarItem item = sidebarItems.get(i);
            if (item.isHeader()) break;
            item.getFile().setDocumentId(targetDocId);
        }

        statusLabel.setText("Status: Documents merged — saving...");
        saveOrderToDB();
    }

    // Move Up
    @FXML
    private void onMoveUp() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index <= 0) return;
        SidebarItem selected = sidebarItems.get(index);
        if (selected.isHeader()) return;

        SidebarItem above = sidebarItems.get(index - 1);
        if (above.isHeader()) {
            // Move across document boundary
            if (index - 2 < 0) {
                statusLabel.setText("Status: Cannot move above first document.");
                return;
            }
            sidebarItems.remove(index);
            sidebarItems.add(index - 1, selected);
            int newDocId = findDocumentIdForPosition(index - 1);
            if (newDocId != -1) selected.getFile().setDocumentId(newDocId);
        } else {
            sidebarItems.remove(index);
            sidebarItems.add(index - 1, selected);
        }

        fileListView.getSelectionModel().select(index - 1);
        fileListView.scrollTo(index - 1);
        statusLabel.setText("Status: File moved up — saving...");
        saveOrderToDB();
    }

    // Move Down
    @FXML
    private void onMoveDown() {
        int index = fileListView.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= sidebarItems.size() - 1) return;
        SidebarItem selected = sidebarItems.get(index);
        if (selected.isHeader()) return;

        SidebarItem below = sidebarItems.get(index + 1);
        if (below.isHeader()) {
            if (index + 2 >= sidebarItems.size()) {
                statusLabel.setText("Status: Cannot move below last document.");
                return;
            }
            sidebarItems.remove(index);
            sidebarItems.add(index + 2, selected);
            int newDocId = findDocumentIdForPosition(index + 2);
            if (newDocId != -1) selected.getFile().setDocumentId(newDocId);
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

    // Delete File
    @FXML
    private void onDeleteFile() {
        SidebarItem selected =
                fileListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isHeader()) {
            statusLabel.setText("Status: Select a file to delete.");
            return;
        }
        sidebarItems.remove(selected);
        logManager.log("FILE_DELETED",
                SessionManager.getInstance().getCurrentUser().getId(),
                "File #" + selected.getFile().getFileId() + " removed");
        statusLabel.setText("Status: File removed.");
    }

    // Export TIFF (F4)
    @FXML
    private void onExport() {
        if (sidebarItems.isEmpty()) {
            statusLabel.setText("Status: Nothing to export — scan first.");
            return;
        }
        if (selectedBox == null) {
            statusLabel.setText(
                    "Status: No active session — start a session first.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                "Single-page TIFF", "Single-page TIFF", "Multi-page TIFF");
        dialog.setTitle("Export TIFF");
        dialog.setHeaderText("Choose export format");
        dialog.setContentText("Format:");

        dialog.showAndWait().ifPresent(choice -> {
            boolean multiPage  = choice.equals("Multi-page TIFF");
            String  folderName = selectedBox.getExportFolderName();

            statusLabel.setText("Status: Exporting...");

            Task<Integer> exportTask = new Task<>() {
                @Override
                protected Integer call() throws Exception {
                    return multiPage
                            ? exportManager.exportMultiPage(sidebarItems,
                            folderName)
                            : exportManager.exportSinglePage(sidebarItems,
                            folderName);
                }
            };

            exportTask.setOnSucceeded(e -> {
                int count = exportTask.getValue();
                String type = multiPage ? "documents" : "files";
                statusLabel.setText("Status: Exported " + count + " " + type
                        + " to ~/Scanova_Exports/" + folderName);
                logManager.log("SCAN_COMPLETE",
                        SessionManager.getInstance().getCurrentUser().getId(),
                        "Exported " + count + " " + type
                                + " | Box: " + selectedBox.getLabel()
                                + " | Format: " + choice);
            });

            exportTask.setOnFailed(e ->
                    statusLabel.setText("Export failed: "
                            + exportTask.getException().getMessage()));

            Thread t = new Thread(exportTask);
            t.setDaemon(true);
            t.start();
        });
    }

    // Load Previous Scan
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
                cases.stream().map(this::formatCase)
                        .collect(Collectors.toList()));
        dialog.setTitle("Load Previous Scan");
        dialog.setHeaderText("Select a previous scan session to load:");
        dialog.setContentText("Session:");

        dialog.showAndWait().ifPresent(selected -> {
            String[] selectedCase = cases.stream()
                    .filter(c -> formatCase(c).equals(selected))
                    .findFirst().orElse(null);
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

            Thread t = new Thread(loadTask);
            t.setDaemon(true);
            t.start();
        });
    }

    // Open Slideshow / Review (F3)
    @FXML
    private void onOpenSlideshow() {
        List<ScannedFile> allFiles = sidebarItems.stream()
                .filter(item -> !item.isHeader())
                .map(SidebarItem::getFile)
                .collect(Collectors.toList());

        if (allFiles.isEmpty()) {
            statusLabel.setText(
                    "Status: No files to review. Scan first.");
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

    // Save current sidebar order to DB
    private void saveOrderToDB() {
        List<SidebarItem> snapshot = List.copyOf(sidebarItems);

        new Thread(() -> {
            int order = 1;
            for (SidebarItem item : snapshot) {
                if (!item.isHeader()) {
                    ScannedFile file = item.getFile();
                    if (file.getDbFileId() != -1)
                        fileManager.updateFileOrder(file.getDbFileId(), order);
                    int docId = getRealDocumentId(file.getDocumentId());
                    if (docId != -1)
                        fileManager.updatePageOrder(
                                file.getReferenceId(), docId, order);
                    order++;
                }
            }
            Platform.runLater(() ->
                    statusLabel.setText("Status: Order saved."));
        }).start();
    }

    // Find which document a sidebar position belongs to
    private int findDocumentIdForPosition(int index) {
        for (int i = index; i >= 0; i--) {
            if (i < sidebarItems.size() && sidebarItems.get(i).isHeader())
                return sidebarItems.get(i).getDocumentId();
        }
        return -1;
    }

    // Get real DB document id — works for live scan and history
    private int getRealDocumentId(int docNumber) {
        int fromScan = scanManager.getRealDocumentId(docNumber);
        if (fromScan != -1) return fromScan;
        return loadedDocumentIdMap.getOrDefault(docNumber, -1);
    }

    private double getProfileBrightness() {
        return (scanManager.getActiveBox() != null)
                ? scanManager.getActiveBox().getBrightness() : 1.0;
    }

    private boolean headerExists(int documentId) {
        return sidebarItems.stream()
                .anyMatch(i -> i.isHeader() && i.getDocumentId() == documentId);
    }

    private String formatCase(String[] c) {
        return "Box: " + c[2] + " | " + c[1] + " | " + c[3];
    }

    // Logout
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }
}