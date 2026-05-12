package dk.easv.scanova.GUI;

import dk.easv.scanova.DAL.FileDAO;
import dk.easv.scanova.Model.ScannedFile;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImagePreviewController {

    @FXML private ImageView imageView;

    private int currentRotation = 0;
    private ScannedFile currentFile;
    private final FileDAO fileDAO = new FileDAO();

    public void setImage(Image image, ScannedFile file) {
        this.currentFile = file;
        imageView.setImage(image);
        currentRotation = file.getRotation();
        imageView.setRotate(currentRotation);
    }

    @FXML
    private void rotateRight() {
        if (currentFile == null) return;
        currentRotation = (currentRotation + 5) % 360;
        imageView.setRotate(currentRotation);
        currentFile.setRotation(currentRotation);
        fileDAO.updateRotation(currentFile.getFileId(), currentRotation);
    }

    @FXML
    private void rotateLeft() {
        if (currentFile == null) return;
        currentRotation = (currentRotation + 355) % 360;
        imageView.setRotate(currentRotation);
        currentFile.setRotation(currentRotation);
        fileDAO.updateRotation(currentFile.getFileId(), currentRotation);
    }
}