package dk.easv.scanova.GUI;

import dk.easv.scanova.BLL.FileManager;
import dk.easv.scanova.BE.ScannedFile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Slider;

public class ImagePreviewController {

    @FXML private StackPane imageContainer;
    @FXML private ImageView imageView;
    @FXML private Slider rotationSlider;
    @FXML private TextField rotationField;

    private int currentRotation = 0;
    private ScannedFile currentFile;

    private final FileManager fileManager = new FileManager();

    @FXML
    private void initialize() {;

        imageContainer.layoutBoundsProperty().addListener((obs, o, n) -> {
            Rectangle clip = new Rectangle(n.getWidth(), n.getHeight());
            imageContainer.setClip(clip);
        });

        imageContainer.widthProperty().addListener((obs, o, n) ->
                Platform.runLater(this::applyImageLayout));
        imageContainer.heightProperty().addListener((obs, o, n) ->
                Platform.runLater(this::applyImageLayout));

        // SLIDER → FIELD
        rotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentRotation = (int) Math.round(newVal.doubleValue());
            rotationField.setText(String.valueOf(currentRotation));
            applyImageLayout();
        });

        // FIELD → SLIDER
        rotationField.setOnAction(e -> {
            try {
                int value = Integer.parseInt(rotationField.getText());
                value = Math.max(-180, Math.min(180, value));
                currentRotation = value;
                rotationSlider.setValue(value);
                applyImageLayout();

            } catch (NumberFormatException ex) {
                rotationField.setText(String.valueOf(currentRotation));
            }
        });

        rotationSlider.setOnMouseReleased(event -> {
            if (currentFile != null
                    && currentFile.getRotation() != currentRotation) {
                currentFile.setRotation(currentRotation);
                // GUI → BLL → DAL
                fileManager.updateRotation(currentFile.getFileId(), currentRotation);
            }
        });
    }

    public void setImage(Image image, ScannedFile file) {
        this.currentFile = file;
        imageView.setImage(image);

        currentRotation  = file.getRotation();

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

        double angle  = Math.toRadians(currentRotation);
        double imgW   = imageView.getImage().getWidth();
        double imgH   = imageView.getImage().getHeight();

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