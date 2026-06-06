package com.nahid.rescuenet.controller;

import com.nahid.rescuenet.DatabaseConnection;
import com.nahid.rescuenet.model.EmergencyRequest;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class FireController {

    @FXML private Label commanderNameLabel, activeFiresLabel, deployedLabel;
    @FXML private TableView<EmergencyRequest> emergencyTable;
    @FXML private TableColumn<EmergencyRequest, Integer> idCol;
    @FXML private TableColumn<EmergencyRequest, String> typeCol, structureCol, locationCol, windCol, statusCol;
    private ObservableList<EmergencyRequest> list = FXCollections.observableArrayList();

    @FXML public void initialize() {
        commanderNameLabel.setText("Station Commander: " + LoginController.loggedInUserName);
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());

        typeCol.setCellValueFactory(c -> {
            String type = c.getValue().getType();
            if(type.equals("CRITICAL SOS")) return new SimpleStringProperty("CLASS A (Extreme)");
            return new SimpleStringProperty("CLASS B (Standard)");
        });

        structureCol.setCellValueFactory(c -> {
            int id = c.getValue().getId();
            if(id % 3 == 0) return new SimpleStringProperty("Industrial Factory");
            else if(id % 2 == 0) return new SimpleStringProperty("Commercial Plaza");
            else return new SimpleStringProperty("Residential Block");
        });

        locationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));

        windCol.setCellValueFactory(c -> {
            int speed = 10 + (c.getValue().getId() % 15);
            return new SimpleStringProperty("High Risk (" + speed + " km/h)");
        });

        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            ObservableList<EmergencyRequest> tempData = FXCollections.observableArrayList();
            int active = 0, deployed = 0;
            try {
                String query = "SELECT er.*, u.email, u.guardian_phone FROM emergency_requests er " +
                        "JOIN users u ON er.citizen_id = u.id " +
                        "WHERE er.emergency_type = 'Fire' OR er.emergency_type = 'CRITICAL SOS' " +
                        "ORDER BY er.request_id DESC";
                ResultSet rs = DatabaseConnection.getInstance().getConnection().createStatement().executeQuery(query);
                while (rs.next()) {
                    String stat = rs.getString("status");
                    tempData.add(new EmergencyRequest(
                            rs.getInt("request_id"),
                            rs.getString("emergency_type"),
                            rs.getString("location"),
                            stat,
                            rs.getString("email"),
                            rs.getString("guardian_phone")
                    ));
                    if(stat.equals("Pending")) active++;
                    else if(stat.equals("Dispatched") || stat.equals("Hazmat Deployed")) deployed++;
                }
                final int finalActive = active;
                final int finalDeployed = deployed;

                Platform.runLater(() -> {
                    list.setAll(tempData);
                    emergencyTable.setItems(list);
                    activeFiresLabel.setText(String.valueOf(finalActive));
                    deployedLabel.setText(String.valueOf(finalDeployed));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void deployFireEngine(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req == null) {
            showAlert(Alert.AlertType.WARNING, "Select Target", "Please click on a fire incident in the table first.");
            return;
        }
        updateStatusDB(req.getId(), "Dispatched");
        showAlert(Alert.AlertType.INFORMATION, "Engine Dispatched", "Fire Engine and Water Tanker are en route.");
    }

    @FXML public void markExtinguished(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req != null) {
            updateStatusDB(req.getId(), "Resolved");
            showAlert(Alert.AlertType.INFORMATION, "Fire Extinguished", "Fire neutralized. The area is now secure.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Select Target", "Please click on an active incident in the table first.");
        }
    }

    private void updateStatusDB(int reqId, String stat) {
        new Thread(() -> {
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("UPDATE emergency_requests SET status=?, responder_id=? WHERE request_id=?");
                ps.setString(1, stat);
                ps.setInt(2, LoginController.loggedInUserId);
                ps.setInt(3, reqId);
                ps.executeUpdate();
                Platform.runLater(this::loadData);
            } catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void deployHazmat(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Please click on a factory/industrial fire in the table first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Hazmat Protocol");
        confirm.setHeaderText("Deploy Specialized Hazmat Team?");
        confirm.setContentText("Warning: Chemical foam units will be dispatched. Confirm?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            updateStatusDB(req.getId(), "Hazmat Deployed");
            showAlert(Alert.AlertType.INFORMATION, "Hazmat Deployed", "Level-A Hazmat Suits deployed.");
        }
    }

    @FXML public void viewHydrantMap(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Location Required", "Please click on an incident in the table to scan for nearby hydrants.");
            return;
        }
        String details = "Target Zone: " + req.getLocation() + "\n\n"
                + "1. Main Street Hydrant: Active (120 PSI)\n"
                + "2. Underground Reserve: 5000L Available\n"
                + "Connecting GPS coordinates to fire engine terminal...";
        showAlert(Alert.AlertType.INFORMATION, "Live Hydrant Scan", details);
    }

    @FXML public void requestHeli(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Please click on a massive fire incident in the table first.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Aerial Support", "Helicopter water drop requested for Case ID: " + req.getId() + ".\nETA: 12 minutes.");
    }

    @FXML public void logout(ActionEvent e) throws Exception {
        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/com/nahid/rescuenet/login.fxml"));
        ((javafx.scene.Node) e.getSource()).getScene().setRoot(root);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}