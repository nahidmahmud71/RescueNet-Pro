package com.nahid.rescuenet.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import java.util.Random;

public class DroneController {

    @FXML private Label recLabel;
    @FXML private Label altLabel;
    @FXML private Label coordLabel;
    @FXML private Label windLabel;
    @FXML private Label targetLabel;
    @FXML private Line scanLine;

    @FXML public void initialize() {
        try {
            Timeline blinker = new Timeline(
                    new KeyFrame(Duration.seconds(0.5), e -> {
                        if (recLabel != null) recLabel.setVisible(!recLabel.isVisible());
                    })
            );
            blinker.setCycleCount(Animation.INDEFINITE);
            blinker.play();

            if (scanLine != null) {
                TranslateTransition scanner = new TranslateTransition(Duration.seconds(2), scanLine);
                scanner.setByY(360f);
                scanner.setAutoReverse(true);
                scanner.setCycleCount(Animation.INDEFINITE);
                scanner.play();
            }

            Random rand = new Random();
            Timeline dataUpdater = new Timeline(
                    new KeyFrame(Duration.seconds(0.8), e -> {
                        double alt = 1450.00 + (rand.nextDouble() * 10 - 5);
                        double lat = 23.8100 + (rand.nextDouble() * 0.0050);
                        double lon = 90.4100 + (rand.nextDouble() * 0.0050);
                        int wind = 10 + rand.nextInt(5);

                        Platform.runLater(() -> {
                            if (altLabel != null) altLabel.setText(String.format("ALT: %.2f FT", alt));
                            if (coordLabel != null) coordLabel.setText(String.format("POS: %.4f, %.4f", lat, lon));
                            if (windLabel != null) windLabel.setText("WIND: " + wind + " KNOTS NE");
                        });
                    })
            );
            dataUpdater.setCycleCount(Animation.INDEFINITE);
            dataUpdater.play();

            Timeline lockTimer = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> {
                Platform.runLater(() -> {
                    if (targetLabel != null) {
                        targetLabel.setText("TARGET: LOCKED");
                        targetLabel.setStyle("-fx-font-family: Consolas; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00fa9a;");
                    }
                });
            }));
            lockTimer.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}