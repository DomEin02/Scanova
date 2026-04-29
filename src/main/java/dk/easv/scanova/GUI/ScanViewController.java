package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.ScanManager;
import dk.easv.scanova.BLL.SessionManager;
import dk.easv.scanova.Model.ScannedFile;
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
    private ListView<ScannedFile> fileListView;

    private final ObservableList<ScannedFile> fileList = FXCollections.observableArrayList();
    private final ScanManager scanManager = new ScanManager();

    @FXML
    public void initialize() {

        if (!SessionManager.getInstance().isLoggedIn()) {
            statusLabel.setText("Access denied — please log in.");
            return;
        }

        // if not returned
        statusLabel.setText("Ready · Logged in as: "
                + SessionManager.getInstance().getCurrentUser());

            fileListView.setItems(fileList);
            fileListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(ScannedFile file, boolean empty) {
                    super.updateItem(file, empty);
                    setText(empty || file == null ? null
                            : "Doc " + file.getDocumentId() + "  |  File #" + file.getFileId());
                }
            });
        }

        @FXML
        private void onStartScan () {
            statusLabel.setText("Status: Scanning...");
            fileList.clear();

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
                                fileList.add(file);
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