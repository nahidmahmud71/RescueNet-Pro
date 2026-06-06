module com.nahid.rescuenet {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    opens com.nahid.rescuenet to javafx.fxml;
    exports com.nahid.rescuenet;
    exports com.nahid.rescuenet.controller;
    opens com.nahid.rescuenet.controller to javafx.fxml;
    exports com.nahid.rescuenet.model;
}