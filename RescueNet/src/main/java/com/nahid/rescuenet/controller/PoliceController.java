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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PoliceController {

    @FXML private Label officerNameLabel, activeAlertsLabel, dispatchedLabel;
    @FXML private TableView<EmergencyRequest> emergencyTable;
    @FXML private TableColumn<EmergencyRequest, Integer> idCol;
    @FXML private TableColumn<EmergencyRequest, String> typeCol, locationCol, emailCol, guardianCol, statusCol;
    private ObservableList<EmergencyRequest> list = FXCollections.observableArrayList();

    @FXML public void initialize() {
        officerNameLabel.setText("Officer On Duty: " + LoginController.loggedInUserName);
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        locationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
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
                        "WHERE er.emergency_type = 'Police' OR er.emergency_type = 'CRITICAL SOS' " +
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

    @FXML public void requestMedicalBackup(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Select an incident to dispatch Medical Backup.");
            return;
        }
        new Thread(() -> {
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("INSERT INTO emergency_requests (citizen_id, emergency_type, location, status) SELECT citizen_id, 'Ambulance', location, 'Pending' FROM emergency_requests WHERE request_id = ?");
                ps.setInt(1, req.getId());
                ps.executeUpdate();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Backup Requested", "Medical unit has been alerted to the location."));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", ex.getMessage()));
            }
        }).start();
    }

    @FXML public void acceptEmergency(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req == null) {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Please select an emergency request from the board.");
            return;
        }

        updateStatusDB(req.getId(), "Dispatched");

        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    String subject = "URGENT: RescueNet Police Dispatch Notification";
                    String body = "Dear Guardian,\n\nWe have received an emergency alert from this user. " +
                            "A Police unit has been dispatched to the location: " + req.getLocation() + ".\n\n" +
                            "Please stay calm. Our forces are on the way.\n\n- RescueNet Command Center";

                    String encodedSubject = subject.replace(" ", "%20");
                    String encodedBody = body.replace(" ", "%20").replace("\n", "%0A");
                    String gmailUrl = String.format("https://mail.google.com/mail/?view=cm&fs=1&to=%s&su=%s&body=%s", req.getEmail(), encodedSubject, encodedBody);

                    Desktop.getDesktop().browse(new URI(gmailUrl));
                    Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Dispatch Initiated", "SWAT team deployed. Browser opened to notify guardian."));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @FXML public void markResolved(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req != null) {
            updateStatusDB(req.getId(), "Resolved");
            showAlert(Alert.AlertType.INFORMATION, "Operation Complete", "Threat Neutralized. Zone secured.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Target Required", "Please select a request to neutralize.");
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

    @FXML public void openLiveMap(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Select Target", "Please select an emergency request from the table first.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nahid/rescuenet/map_tracking.fxml"));
            Scene scene = new Scene(loader.load());
            MapTrackingController controller = loader.getController();
            controller.setLocationData(req.getLocation());
            Stage stage = new Stage();
            stage.setTitle("Live Satellite Tracking - Real Google Maps");
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Map Error", ex.getMessage());
        }
    }

    @FXML public void deployDrone(ActionEvent e) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/nahid/rescuenet/drone_view.fxml"));
            DroneController controller = new DroneController();
            fxmlLoader.setController(controller);
            Scene scene = new Scene(fxmlLoader.load());
            Stage droneStage = new Stage();
            droneStage.setTitle("Tactical UAV Live Feed");
            droneStage.setScene(scene);
            droneStage.setResizable(false);
            droneStage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Drone System Error", "Error: " + ex.getMessage());
        }
    }

    @FXML public void logout(ActionEvent e) throws Exception {
        Stage s = (Stage) ((Node) e.getSource()).getScene().getWindow();
        s.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/nahid/rescuenet/login.fxml")), 1100, 750));
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}