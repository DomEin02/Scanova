package dk.easv.scanova.GUI;

import dk.easv.scanova.Model.ScannedFile;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

public class SlideshowController {

    @FXML private ImageView slideshowImageView;
    @FXML private Label     fileIndexLabel;
    @FXML private Label     docLabel;
    @FXML private Label     autoPlayLabel;

    private List<ScannedFile> files;
    private int      currentIndex = 0;
    private Timeline autoSlideTimer;

    private static final int AUTO_SECONDS = 3;

    public void setFiles(List<ScannedFile> files) {
        this.files        = files;
        this.currentIndex = 0;
        showCurrentFile();
        startAutoSlide();

        Platform.runLater(() -> {
            Scene scene = slideshowImageView.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case RIGHT, SPACE -> { stopAutoSlide(); onNext(); }
                        case LEFT         -> { stopAutoSlide(); onPrevious(); }
                        case P            -> toggleAutoSlide();
                        case ESCAPE       -> slideshowImageView
                                .getScene().getWindow().hide();
                        default           -> {}
                    }
                });
            }
        });
    }

    @FXML
    public void onNext() {
        if (files == null || files.isEmpty()) return;
        if (currentIndex < files.size() - 1) {
            currentIndex++;
            showCurrentFile();
        } else {
            stopAutoSlide();
        }
    }

    @FXML
    public void onPrevious() {
        if (files == null || files.isEmpty()) return;
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentFile();
        }
    }

    private void showCurrentFile() {
        if (files == null || files.isEmpty()) return;
        ScannedFile file = files.get(currentIndex);

        fileIndexLabel.setText(
                "File " + (currentIndex + 1) + " of " + files.size());
        docLabel.setText("Document " + file.getDocumentId()
                + "  |  File #" + file.getFileId());

        try {
            BufferedImage buffered = ImageIO.read(
                    new ByteArrayInputStream(file.getImageData()));
            if (buffered != null) {
                Image image = SwingFXUtils.toFXImage(buffered, null);
                slideshowImageView.setImage(image);
                slideshowImageView.setRotate(file.getRotation());
            }
        } catch (Exception e) {
            System.out.println("Slideshow could not load image: "
                    + e.getMessage());
        }
    }

    private void startAutoSlide() {
        stopAutoSlide();
        autoSlideTimer = new Timeline(
                new KeyFrame(Duration.seconds(AUTO_SECONDS), e -> onNext()));
        autoSlideTimer.setCycleCount(Animation.INDEFINITE);
        autoSlideTimer.play();
        updateAutoPlayLabel(true);
    }

    private void stopAutoSlide() {
        if (autoSlideTimer != null) autoSlideTimer.stop();
        updateAutoPlayLabel(false);
    }

    private void toggleAutoSlide() {
        if (autoSlideTimer != null
                && autoSlideTimer.getStatus() == Animation.Status.RUNNING) {
            stopAutoSlide();
        } else {
            startAutoSlide();
        }
    }

    private void updateAutoPlayLabel(boolean playing) {
        if (autoPlayLabel != null)
            autoPlayLabel.setText(playing
                    ? "⏵ Auto-play  (P = pause)"
                    : "⏸ Paused  (P = resume)");
    }
}