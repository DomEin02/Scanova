package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ScanManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.ScannedFile;
import dk.easv.scanova.Model.SidebarItem;
import dk.easv.scanova.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

public class ScanViewController {

    @FXML
    private ImageView imagePreview;
    @FXML
    private Label scanCountLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label documentCountLabel;
    @FXML
    private ListView<SidebarItem> fileListView;

    private final ObservableList<SidebarItem> sidebarItems = FXCollections.observableArrayList();
    private final ScanManager scanManager = new ScanManager();
    private int lastInsertedDocId = -1;

    @FXML
    public void initialize() {

        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        // if not returned
        statusLabel.setText("Ready · Logged in as: "
                + SessionManager.getInstance().getCurrentUser());

        fileListView.setItems(sidebarItems);
        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SidebarItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.isHeader()) {
                    setText("📁  Document " + item.getDocumentId());
                } else {
                    setText("    File #" + item.getFile().getFileId());
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
                            imagePreview.setImage(SwingFXUtils.toFXImage(buffered, null));
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Could not load image — " + e.getMessage());
                    }
                });
        }

        @FXML
        private void onStartScan () {
            statusLabel.setText("Status: Scanning...");
            sidebarItems.clear();
            lastInsertedDocId = -1;

            Task<Void> scanTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    scanManager.initSession();
                    // Task 3 — show total on start
                    Platform.runLater(() ->
                            scanCountLabel.setText("Scans: 0 / " + scanManager.getTotalAvailable()));

                    while (scanManager.hasMore()) {
                        List<ScannedFile> fetched = scanManager.fetchNext();
                        if (fetched == null) break;

                        Platform.runLater(() -> {
                            for (ScannedFile file : fetched) {
                                // Add to sidebar
                                if (file.getDocumentId() != lastInsertedDocId) {
                                    sidebarItems.add(new SidebarItem(file.getDocumentId()));
                                    lastInsertedDocId = file.getDocumentId();
                                }
                                sidebarItems.add(new SidebarItem(file));
                                // display TIFF in ImageView
                                try {
                                    BufferedImage buffered = ImageIO.read(
                                            new ByteArrayInputStream(file.getImageData()));
                                    if (buffered != null) {
                                        Image image = SwingFXUtils.toFXImage(buffered, null);
                                        imagePreview.setImage(image);
                                    }
                                } catch (Exception e) {
                                    statusLabel.setText("Status: Could not display image — " + e.getMessage());
                                }
                                // update counter on each fetch
                                scanCountLabel.setText("Scans: " + scanManager.getTotalFilesFetched()
                                        + " / " + scanManager.getTotalAvailable());
                                documentCountLabel.setText("Documents: "
                                        + scanManager.getAllDocuments().size());
                            }
                        });
                    }
                    Platform.runLater(() ->
                            statusLabel.setText("Status: Done — "
                                    + scanManager.getTotalFilesFetched() + " files scanned")
                    );
                    return null;
                }
            };
            scanTask.setOnFailed(e -> Platform.runLater(() ->
                    statusLabel.setText("Status: Error — " + scanTask.getException().getMessage())));

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