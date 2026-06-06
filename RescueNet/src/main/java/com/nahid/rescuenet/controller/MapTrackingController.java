package com.nahid.rescuenet.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import java.awt.Desktop;
import java.net.URI;

public class MapTrackingController {

    @FXML private Label targetLocLabel;
    @FXML private Circle pingDot;
    @FXML private Circle radarRing;

    private String currentLocation = "";

    public void setLocationData(String location) {
        this.currentLocation = location;
        targetLocLabel.setText("Tracking Origin: " + location);

        FadeTransition ft = new FadeTransition(Duration.seconds(0.8), pingDot);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(FadeTransition.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.seconds(1.5), radarRing);
        st.setByX(5f);
        st.setByY(5f);
        st.setCycleCount(ScaleTransition.INDEFINITE);
        st.play();

        FadeTransition ringFade = new FadeTransition(Duration.seconds(1.5), radarRing);
        ringFade.setFromValue(1.0);
        ringFade.setToValue(0.0);
        ringFade.setCycleCount(FadeTransition.INDEFINITE);
        ringFade.play();
    }

    @FXML
    public void openRealGoogleMaps(ActionEvent event) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                String query = currentLocation.replace(" ", "+").replace(",", "%2C");
                String url = "https://www.google.com/maps/search/?api=1&query=" + query;
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}