package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.FileManager;
import dk.easv.scanova.Model.ScannedFile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Slider;

public class ImagePreviewController {

    @FXML private StackPane imageContainer;
    @FXML private ImageView imageView;
    @FXML private Slider    rotationSlider;

    private int         currentRotation  = 0;
    private double      profileBrightness = 1.0;
    private ScannedFile currentFile;

    // GUI → BLL — never GUI → DAL
    private final FileManager fileManager = new FileManager();

    @FXML
    private void initialize() {
        // Clip container so rotated corners never escape
        imageContainer.layoutBoundsProperty().addListener((obs, o, n) -> {
            Rectangle clip = new Rectangle(n.getWidth(), n.getHeight());
            imageContainer.setClip(clip);
        });

        // Update layout when container resizes
        imageContainer.widthProperty().addListener((obs, o, n) ->
                Platform.runLater(this::applyImageLayout));
        imageContainer.heightProperty().addListener((obs, o, n) ->
                Platform.runLater(this::applyImageLayout));

        // Slider changes rotation
        rotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentRotation = ((int) Math.round(newVal.doubleValue() / 5)) * 5;
            applyImageLayout();
        });

        // Save rotation to DB when slider is released — GUI → BLL
        rotationSlider.setOnMouseReleased(event -> {
            if (currentFile != null
                    && currentFile.getRotation() != currentRotation) {
                currentFile.setRotation(currentRotation);
                if (currentFile.getDbFileId() != -1) {
                    fileManager.updateRotation(
                            currentFile.getDbFileId(), currentRotation);
                }
            }
        });
    }

    // ── Set image with profile brightness ─────────────────────────────────────
    public void setImage(Image image, ScannedFile file, double brightness) {
        this.currentFile        = file;
        this.profileBrightness  = brightness;
        currentRotation         = file.getRotation();

        // Apply brightness from profile
        Image displayImage = applyBrightness(image, brightness);
        imageView.setImage(displayImage);

        // Set slider to file's current rotation
        double sliderValue = currentRotation > 180
                ? currentRotation - 360
                : currentRotation;
        rotationSlider.setValue(sliderValue);

        Platform.runLater(this::applyImageLayout);
    }

    // ── Set image without brightness — fallback ───────────────────────────────
    public void setImage(Image image, ScannedFile file) {
        setImage(image, file, 1.0);
    }

    // ── Apply brightness to image ─────────────────────────────────────────────
    private Image applyBrightness(Image image, double brightness) {
        if (Math.abs(brightness - 1.0) < 0.01) return image; // no change

        int width  = (int) image.getWidth();
        int height = (int) image.getHeight();

        WritableImage output = new WritableImage(width, height);
        PixelReader   reader = image.getPixelReader();
        PixelWriter   writer = output.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                Color brightened = new Color(
                        Math.min(1.0, color.getRed()   * brightness),
                        Math.min(1.0, color.getGreen() * brightness),
                        Math.min(1.0, color.getBlue()  * brightness),
                        color.getOpacity()
                );
                writer.setColor(x, y, brightened);
            }
        }
        return output;
    }

    // ── Apply rotation and scale to image ─────────────────────────────────────
    private void applyImageLayout() {
        if (imageView.getImage() == null) return;

        double containerW = imageContainer.getWidth();
        double containerH = imageContainer.getHeight();

        if (containerW < 10 || containerH < 10) return;

        double angle = Math.toRadians(currentRotation);
        double imgW  = imageView.getImage().getWidth();
        double imgH  = imageView.getImage().getHeight();

        double rotatedW = Math.abs(imgW * Math.cos(angle))
                + Math.abs(imgH * Math.sin(angle));
        double rotatedH = Math.abs(imgW * Math.sin(angle))
                + Math.abs(imgH * Math.cos(angle));

        double scale = Math.min(containerW / rotatedW, containerH / rotatedH);
        scale = Math.min(scale, 1.0);

        imageView.setRotate(currentRotation);
        imageView.setFitWidth(imgW  * scale);
        imageView.setFitHeight(imgH * scale);

        Rectangle clip = new Rectangle(containerW, containerH);
        imageContainer.setClip(clip);
    }
}