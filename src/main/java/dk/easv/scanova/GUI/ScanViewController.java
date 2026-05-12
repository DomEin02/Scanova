package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ScanManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.ScannedFile;
import dk.easv.scanova.model.SidebarItem;
import dk.easv.scanova.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

public class ScanViewController {

    @FXML private Label scanCountLabel;
    @FXML private Label statusLabel;
    @FXML private Label documentCountLabel;
    @FXML private ListView<SidebarItem> fileListView;

    @FXML private ImagePreviewController imagePreviewComponent;

    private final ObservableList<SidebarItem> sidebarItems = FXCollections.observableArrayList();
    private final ScanManager scanManager = new ScanManager();

    @FXML
    public void initialize() {

        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        statusLabel.setText("Ready · Logged in as: " +
                SessionManager.getInstance().getCurrentUser());

        fileListView.setItems(sidebarItems);

        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SidebarItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                if (item.isHeader()) {
                    setText("📁 Document " + item.getDocumentId());
                } else {
                    setText("    File #" + item.getFile().getFileId());
                }
            }
        });

        fileListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldItem, newItem) -> {

                    if (newItem == null || newItem.isHeader()) return;

                    ScannedFile file = newItem.getFile();

                    try {
                        BufferedImage buffered = ImageIO.read(
                                new ByteArrayInputStream(file.getImageData())
                        );

                        if (buffered != null) {
                            Image image = SwingFXUtils.toFXImage(buffered, null);
                            imagePreviewComponent.setImage(image, file);
                        }

                    } catch (Exception e) {
                        statusLabel.setText("Could not load image — " + e.getMessage());
                    }
                });
    }

    // 🔥 FIX: no duplicates, no lastInsertedDocId hack
    private boolean headerExists(int documentId) {
        return sidebarItems.stream()
                .anyMatch(i -> i.isHeader() && i.getDocumentId() == documentId);
    }

    @FXML
    private void onStartScan() {

        statusLabel.setText("Status: Scanning...");
        sidebarItems.clear();

        Task<Void> scanTask = new Task<>() {

            @Override
            protected Void call() throws Exception {

                scanManager.initSession();

                Platform.runLater(() ->
                        scanCountLabel.setText("Scans: 0 / " + scanManager.getTotalAvailable()));

                while (scanManager.hasMore()) {

                    List<ScannedFile> fetched = scanManager.fetchNext();
                    if (fetched == null) break;

                    Platform.runLater(() -> {

                        for (ScannedFile file : fetched) {

                            // ✔ add header ONLY once per document
                            if (!headerExists(file.getDocumentId())) {
                                sidebarItems.add(new SidebarItem(file.getDocumentId()));
                            }

                            // ✔ add file
                            sidebarItems.add(new SidebarItem(file));

                            // preview latest file
                            try {
                                BufferedImage buffered = ImageIO.read(
                                        new ByteArrayInputStream(file.getImageData())
                                );

                                if (buffered != null) {
                                    Image image = SwingFXUtils.toFXImage(buffered, null);
                                    imagePreviewComponent.setImage(image, file);
                                }

                            } catch (Exception e) {
                                statusLabel.setText("Could not display image — " + e.getMessage());
                            }

                            // counters
                            int totalScans = scanManager.getAllDocuments()
                                    .stream()
                                    .mapToInt(d -> d.getFiles().size())
                                    .sum();

                            scanCountLabel.setText(
                                    "Scans: " + totalScans +
                                            " / " + scanManager.getTotalAvailable()
                            );

                            documentCountLabel.setText(
                                    "Documents: " + scanManager.getAllDocuments().size()
                            );
                        }
                    });
                }

                Platform.runLater(() ->
                        statusLabel.setText("Status: Done")
                );

                return null;
            }
        };

        scanTask.setOnFailed(e ->
                Platform.runLater(() ->
                        statusLabel.setText("Error — " + scanTask.getException().getMessage())
                )
        );

        Thread thread = new Thread(scanTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.load("loginView.fxml");
    }
}