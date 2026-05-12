package dk.easv.scanova.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.Model.ScannedFile;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;

public class ImagePreviewController {

    @FXML
    private StackPane imageContainer;

    @FXML
    private ImageView imageView;

    @FXML
    private Slider rotationSlider;

    private int currentRotation = 0;

    private ScannedFile currentFile;

    private final FileDAO fileDAO = new FileDAO();

    @FXML
    private void initialize() {
        imageContainer.widthProperty().addListener((obs, o, n) ->
                Platform.runLater(this::applyImageLayout)
        );

        imageContainer.heightProperty().addListener((obs, o, n) ->
                Platform.runLater(this::applyImageLayout)
        );

        rotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {

            currentRotation = ((int) Math.round(newVal.doubleValue() / 5)) * 5;

            applyImageLayout();
        });

        rotationSlider.setOnMouseReleased(event -> {

            if (currentFile != null &&

                    currentFile.getRotation() != currentRotation) {

                currentFile.setRotation(currentRotation);

                fileDAO.updateRotation(currentFile.getFileId(), currentRotation);
            }
        });
    }

    public void setImage(Image image, ScannedFile file) {

        this.currentFile = file;

        imageView.setImage(image);

        currentRotation = file.getRotation();

        rotationSlider.setValue(currentRotation);

        Platform.runLater(this::applyImageLayout);
    }

    private void applyImageLayout() {

        if (imageView.getImage() == null) return;
        if (imageContainer.getWidth() <= 0 || imageContainer.getHeight() <= 0) return;

        double angle = Math.toRadians(currentRotation);

        double imgW = imageView.getImage().getWidth();
        double imgH = imageView.getImage().getHeight();

        double containerW = imageContainer.getLayoutBounds().getWidth();
        double containerH = imageContainer.getLayoutBounds().getHeight();

        if (containerW < 10 || containerH < 10) return;

        double rotatedW =
                Math.abs(imgW * Math.cos(angle)) +
                        Math.abs(imgH * Math.sin(angle));

        double rotatedH =
                Math.abs(imgW * Math.sin(angle)) +
                        Math.abs(imgH * Math.cos(angle));

        double scaleX = containerW / rotatedW;
        double scaleY = containerH / rotatedH;

        double scale = Math.min(scaleX, scaleY);
        scale = Math.min(scale, 1.0);

        imageView.setRotate(currentRotation);
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);
    }
}
