package dk.easv.scanova.GUI;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImagePreviewController {

    @FXML
    private ImageView imageView;

    private double currentRotation = 0;

    public void setImage(Image image) {
        imageView.setImage(image);
        currentRotation = 0;
        imageView.setRotate(0);
    }

    @FXML
    private void rotateRight() {
        currentRotation += 90;
        imageView.setRotate(currentRotation);
    }

    @FXML
    private void rotateLeft() {
        currentRotation -= 90;
        imageView.setRotate(currentRotation);
    }
}
