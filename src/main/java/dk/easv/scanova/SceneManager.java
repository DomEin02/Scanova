package dk.easv.scanova;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage stage;

    public static void setStage(Stage s) { stage = s; }

    public static void load(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("/dk/easv/scanova/" + fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Failed to load screen: " + fxml);
            e.printStackTrace();
        }
    }
}