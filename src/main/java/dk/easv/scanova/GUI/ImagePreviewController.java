package dk.easv.scanova.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Slider;
import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.Model.ScannedFile;

public class ImagePreviewController {

    @FXML private StackPane imageContainer;
    @FXML private ImageView imageView;
    @FXML private Slider    rotationSlider;

    private int         currentRotation = 0;
    private ScannedFile currentFile;
    private final FileDAO fileDAO = new FileDAO();

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

        // Slider changes rotation — handles negative values too
        rotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentRotation = ((int) Math.round(newVal.doubleValue() / 5)) * 5;
            applyImageLayout();
        });

        // Save rotation to DB when slider is released
        rotationSlider.setOnMouseReleased(event -> {
            if (currentFile != null
                    && currentFile.getRotation() != currentRotation) {
                currentFile.setRotation(currentRotation);
                fileDAO.updateRotation(currentFile.getFileId(), currentRotation);
            }
        });
    }

    public void setImage(Image image, ScannedFile file) {
        this.currentFile = file;
        imageView.setImage(image);
        currentRotation  = file.getRotation();

        // Convert stored rotation to slider value
        // e.g. 270 stored → shows as -90 on slider
        double sliderValue = currentRotation > 180
                ? currentRotation - 360
                : currentRotation;
        rotationSlider.setValue(sliderValue);

        Platform.runLater(this::applyImageLayout);
    }

    private void applyImageLayout() {
        if (imageView.getImage() == null) return;

        double containerW = imageContainer.getWidth();
        double containerH = imageContainer.getHeight();

        if (containerW < 10 || containerH < 10) return;

        double angle = Math.toRadians(currentRotation);
        double imgW  = imageView.getImage().getWidth();
        double imgH  = imageView.getImage().getHeight();

        // Calculate bounding box of rotated image
        double rotatedW = Math.abs(imgW * Math.cos(angle))
                + Math.abs(imgH * Math.sin(angle));
        double rotatedH = Math.abs(imgW * Math.sin(angle))
                + Math.abs(imgH * Math.cos(angle));

        // Scale so rotated image fits inside container
        double scale = Math.min(
                containerW / rotatedW,
                containerH / rotatedH
        );
        scale = Math.min(scale, 1.0); // never upscale

        // Apply rotation and scale
        imageView.setRotate(currentRotation);
        imageView.setFitWidth(imgW  * scale);
        imageView.setFitHeight(imgH * scale);

        // Update clip
        Rectangle clip = new Rectangle(containerW, containerH);
        imageContainer.setClip(clip);
    }
}