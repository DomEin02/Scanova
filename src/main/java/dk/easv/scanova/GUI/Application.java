package dk.easv.scanova.GUI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/dk/easv/scanova/scanView.fxml")
        );

        Scene scene = new Scene(loader.load(), 1200, 800);

        stage.setTitle("Scanova");
        stage.setScene(scene);
        stage.show();

        System.out.println("APP STARTED");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
