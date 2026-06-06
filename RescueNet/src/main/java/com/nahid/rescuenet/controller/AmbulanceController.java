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
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AmbulanceController {

    @FXML private Label medicNameLabel, activeAlertsLabel, dispatchedLabel;
    @FXML private TableView<EmergencyRequest> emergencyTable;
    @FXML private TableColumn<EmergencyRequest, Integer> idCol;
    @FXML private TableColumn<EmergencyRequest, String> typeCol, priorityCol, locationCol, guardianCol, statusCol;
    private ObservableList<EmergencyRequest> list = FXCollections.observableArrayList();

    @FXML public void initialize() {
        medicNameLabel.setText("Paramedic On Duty: " + LoginController.loggedInUserName);
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));

        priorityCol.setCellValueFactory(c -> {
            String type = c.getValue().getType();
            if(type.equals("CRITICAL SOS")) return new SimpleStringProperty("HIGH (Code Red)");
            return new SimpleStringProperty("STANDARD (Code Yellow)");
        });

        locationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));
        guardianCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGuardianPhone()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            ObservableList<EmergencyRequest> tempData = FXCollections.observableArrayList();
            int active = 0, dispatch = 0;
            try {
                String query = "SELECT er.*, u.email, u.guardian_phone FROM emergency_requests er " +
                        "JOIN users u ON er.citizen_id = u.id " +
                        "WHERE er.emergency_type = 'Ambulance' OR er.emergency_type = 'CRITICAL SOS' " +
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
                    else if(stat.equals("Dispatched")) dispatch++;
                }
                final int finalActive = active;
                final int finalDispatch = dispatch;

                Platform.runLater(() -> {
                    list.setAll(tempData);
                    emergencyTable.setItems(list);
                    activeAlertsLabel.setText(String.valueOf(finalActive));
                    dispatchedLabel.setText(String.valueOf(finalDispatch));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void acceptEmergency(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req == null) {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Please select a dispatch request.");
            return;
        }
        updateStatusDB(req.getId(), "Dispatched");
        showAlert(Alert.AlertType.INFORMATION, "Unit Dispatched", "Ambulance is en route to the location. Sirens active.");
    }

    @FXML public void markSecured(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req != null) {
            updateStatusDB(req.getId(), "Patient Secured");
            showAlert(Alert.AlertType.INFORMATION, "Patient Secured", "Patient picked up safely. Navigating to the nearest ER.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Select a patient to secure.");
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

    @FXML public void notifyBloodBank(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Please select a critical patient first.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Hospital Alerted", "Nearest ER and Blood Bank have been notified of an incoming trauma patient.");
    }

    @FXML public void openLiveMap(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Select Target", "Please select a patient location to map the route.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nahid/rescuenet/map_tracking.fxml"));
            Scene scene = new Scene(loader.load());
            MapTrackingController controller = loader.getController();
            controller.setLocationData(req.getLocation());
            Stage stage = new Stage();
            stage.setTitle("Ambulance GPS Navigation");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML public void viewHospitals(ActionEvent e) {
        showAlert(Alert.AlertType.INFORMATION, "Nearest ER Wards", "1. Dhaka Medical College (3.2 km) - ER Available\n2. Square Hospital (4.5 km) - ER Full\nRouting priority to DMC.");
    }

    @FXML public void checkTraffic(ActionEvent e) {
        showAlert(Alert.AlertType.INFORMATION, "Live Traffic Grid", "Route to Tejgaon I/A is heavily congested. ETA increased by 12 mins. Suggesting alternate route via Link Road.");
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