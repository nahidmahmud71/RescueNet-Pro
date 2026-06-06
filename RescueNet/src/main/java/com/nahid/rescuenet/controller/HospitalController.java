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

public class HospitalController {

    @FXML private Label doctorNameLabel, incomingLabel, icuLabel;
    @FXML private TableView<EmergencyRequest> emergencyTable;
    @FXML private TableColumn<EmergencyRequest, Integer> idCol;
    @FXML private TableColumn<EmergencyRequest, String> typeCol, locationCol, emailCol, guardianCol, statusCol;
    private ObservableList<EmergencyRequest> list = FXCollections.observableArrayList();

    private int totalIcuBeds = 20;
    private int occupiedBeds = 0;

    @FXML public void initialize() {
        doctorNameLabel.setText("Chief Medical Officer: Dr. " + LoginController.loggedInUserName);
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
            int incoming = 0;
            int admittedNow = 0;
            try {
                String query = "SELECT er.*, u.email, u.guardian_phone FROM emergency_requests er " +
                        "JOIN users u ON er.citizen_id = u.id " +
                        "WHERE er.emergency_type = 'Hospital' OR er.emergency_type = 'Ambulance' OR er.emergency_type = 'CRITICAL SOS' " +
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
                    if(stat.equals("Pending") || stat.equals("Dispatched") || stat.equals("Patient Secured")) {
                        incoming++;
                    } else if(stat.equals("Admitted")) {
                        admittedNow++;
                    }
                }

                final int finalIncoming = incoming;
                this.occupiedBeds = admittedNow;
                final int availableBeds = totalIcuBeds - occupiedBeds;

                Platform.runLater(() -> {
                    list.setAll(tempData);
                    emergencyTable.setItems(list);
                    incomingLabel.setText(String.valueOf(finalIncoming));
                    icuLabel.setText(String.valueOf(availableBeds));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void admitPatient(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req == null) {
            showAlert(Alert.AlertType.WARNING, "Patient Required", "Please select a patient from the ER Board.");
            return;
        }
        if(req.getStatus().equals("Admitted")) {
            showAlert(Alert.AlertType.INFORMATION, "Notice", "Patient is already admitted to the ICU.");
            return;
        }
        if((totalIcuBeds - occupiedBeds) <= 0) {
            showAlert(Alert.AlertType.ERROR, "ICU Full", "No available ICU beds. Please refer to another hospital.");
            return;
        }
        updateStatusDB(req.getId(), "Admitted");
        showAlert(Alert.AlertType.INFORMATION, "ER Admission Success", "Patient transferred to ICU. Bed assigned.");
    }

    @FXML public void dischargePatient(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if(req == null) {
            showAlert(Alert.AlertType.WARNING, "Patient Required", "Select an admitted patient to discharge.");
            return;
        }
        updateStatusDB(req.getId(), "Discharged");
        showAlert(Alert.AlertType.INFORMATION, "Patient Discharged", "Patient condition stable. ICU Bed is now free.");
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

    @FXML public void requestBlood(ActionEvent e) {
        TextInputDialog dialog = new TextInputDialog("O+");
        dialog.setTitle("Blood Bank Requisition");
        dialog.setHeaderText("Request Emergency Blood Units");
        dialog.setContentText("Enter Blood Group and Units (e.g., O+ 2 Bags):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(blood -> {
            showAlert(Alert.AlertType.INFORMATION, "Request Sent", "Urgent request for " + blood + " has been sent to the Central Blood Bank.");
        });
    }

    @FXML public void viewMedicalHistory(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Select Patient", "Select a patient to fetch NID database records.");
            return;
        }

        String history = "=== NID LINKED MEDICAL RECORD ===\n\n"
                + "Patient ID : " + req.getId() + "\n"
                + "Blood Group: B+ (Positive)\n"
                + "Allergies  : Penicillin, Dust\n"
                + "Diabetic   : No\n"
                + "Past Issues: Asthma (Inhaler prescribed 2023)\n\n"
                + "ER Triage Notes: Requires immediate oxygen support.";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("National Health Database");
        alert.setHeaderText("Patient Medical History Fetched");

        TextArea area = new TextArea(history);
        area.setWrapText(true);
        area.setEditable(false);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
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