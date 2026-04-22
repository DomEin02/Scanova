module dk.easv.scanova {
    requires javafx.controls;
    requires javafx.fxml;


    opens dk.easv.scanova to javafx.fxml;
    exports dk.easv.scanova;
}