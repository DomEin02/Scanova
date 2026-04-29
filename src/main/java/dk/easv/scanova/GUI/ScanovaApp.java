package dk.easv.scanova.GUI;

import dk.easv.scanova.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class ScanovaApp extends Application {

    @Override
    public void start(Stage stage) {
        SceneManager.setStage(stage);

        SceneManager.load("loginView.fxml");

        stage.setTitle("Scanova");
        stage.setResizable(false); // keep login screen fixed size
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}