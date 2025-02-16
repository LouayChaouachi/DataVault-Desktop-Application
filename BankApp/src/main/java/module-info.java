module com.example.lol {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics;
    requires javafx.base;


    opens com.example.lol to javafx.fxml;
    exports com.example.lol;
}