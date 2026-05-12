package dk.easv.scanova.GUI;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.Model.ScannedFile;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class ImagePreviewController {

    @FXML
    private ImageView imageView;

    @FXML
    private StackPane imageContainer;

    @FXML
    private Slider rotationSlider;

    private int currentRotation = 0;

    private ScannedFile currentFile;

    private final FileDAO fileDAO = new FileDAO();

    @FXML
    private void initialize() {

        imageView.fitWidthProperty().bind(
                imageContainer.widthProperty().subtract(20));

        imageView.fitHeightProperty().bind(
                imageContainer.heightProperty().subtract(20));

        rotationSlider.valueProperty().addListener((obs, oldVal, newVal) -> {

            currentRotation = ((int) Math.round(newVal.doubleValue() / 5)) * 5;

            imageView.setRotate(currentRotation);

            Rectangle clip = new Rectangle();

            clip.widthProperty().bind(imageContainer.widthProperty());
            clip.heightProperty().bind(imageContainer.heightProperty());

            imageContainer.setClip(clip);
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

        imageView.setRotate(currentRotation);

        rotationSlider.setValue(currentRotation);
    }
}
