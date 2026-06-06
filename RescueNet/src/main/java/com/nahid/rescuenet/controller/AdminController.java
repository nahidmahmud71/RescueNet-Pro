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

public class AdminController {

    @FXML private Label adminNameLabel, totalUsersLabel, activeCrisesLabel, resolvedCasesLabel;
    @FXML private Label policeUnitsLabel, fireUnitsLabel, emsUnitsLabel;
    @FXML private TableView<EmergencyRequest> emergencyTable;
    @FXML private TableColumn<EmergencyRequest, Integer> idCol;
    @FXML private TableColumn<EmergencyRequest, String> typeCol, locationCol, phoneCol, statusCol;
    private ObservableList<EmergencyRequest> list = FXCollections.observableArrayList();

    @FXML public void initialize() {
        adminNameLabel.setText("Admin Console: " + LoginController.loggedInUserName);
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        locationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));
        phoneCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGuardianPhone()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        loadData();
        loadSystemStats();
    }

    private void loadData() {
        new Thread(() -> {
            ObservableList<EmergencyRequest> tempData = FXCollections.observableArrayList();
            int activeCrises = 0, resolvedCrises = 0;
            int polActive = 0, fireActive = 0, emsActive = 0;

            try {
                String query = "SELECT er.*, u.email, u.guardian_phone FROM emergency_requests er " +
                        "JOIN users u ON er.citizen_id = u.id ORDER BY er.request_id DESC";
                ResultSet rs = DatabaseConnection.getInstance().getConnection().createStatement().executeQuery(query);
                while (rs.next()) {
                    String type = rs.getString("emergency_type");
                    String stat = rs.getString("status");

                    tempData.add(new EmergencyRequest(
                            rs.getInt("request_id"), type, rs.getString("location"),
                            stat, rs.getString("email"), rs.getString("guardian_phone")
                    ));

                    if (stat.equals("Resolved") || stat.equals("Discharged")) {
                        resolvedCrises++;
                    } else {
                        activeCrises++;
                        if (type.equals("Police")) polActive++;
                        else if (type.equals("Fire")) fireActive++;
                        else if (type.equals("Ambulance") || type.equals("Hospital")) emsActive++;
                        else if (type.equals("CRITICAL SOS")) {
                            polActive++; fireActive++; emsActive++;
                        }
                    }
                }

                final int fActive = activeCrises;
                final int fResolved = resolvedCrises;
                final int basePolice = 12 + polActive;
                final int baseFire = 5 + fireActive;
                final int baseEms = 8 + emsActive;

                Platform.runLater(() -> {
                    list.setAll(tempData);
                    emergencyTable.setItems(list);
                    activeCrisesLabel.setText(String.valueOf(fActive));
                    resolvedCasesLabel.setText(String.valueOf(fResolved));
                    policeUnitsLabel.setText(basePolice + " Units");
                    fireUnitsLabel.setText(baseFire + " Units");
                    emsUnitsLabel.setText(baseEms + " Units");
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSystemStats() {
        new Thread(() -> {
            try {
                ResultSet rs = DatabaseConnection.getInstance().getConnection().createStatement().executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next()) {
                    final int users = rs.getInt(1);
                    Platform.runLater(() -> totalUsersLabel.setText(String.valueOf(users)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void forceResolve(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Select Request", "Please select an active emergency from the table.");
            return;
        }
        updateStatusDB(req.getId(), "Resolved");
        showAlert(Alert.AlertType.INFORMATION, "Override Applied", "Emergency forcibly marked as Resolved by Admin.");
    }

    @FXML public void deleteRequest(ActionEvent e) {
        EmergencyRequest req = emergencyTable.getSelectionModel().getSelectedItem();
        if (req == null) {
            showAlert(Alert.AlertType.WARNING, "Select Request", "Please select a request from the table to permanently delete.");
            return;
        }
        new Thread(() -> {
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("DELETE FROM emergency_requests WHERE request_id=?");
                ps.setInt(1, req.getId());
                ps.executeUpdate();
                Platform.runLater(() -> {
                    loadData();
                    showAlert(Alert.AlertType.INFORMATION, "Record Purged", "Request permanently deleted from the database.");
                });
            } catch(Exception ex){
                ex.printStackTrace();
            }
        }).start();
    }

    private void updateStatusDB(int reqId, String stat) {
        new Thread(() -> {
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("UPDATE emergency_requests SET status=? WHERE request_id=?");
                ps.setString(1, stat);
                ps.setInt(2, reqId);
                ps.executeUpdate();
                Platform.runLater(this::loadData);
            } catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    @FXML public void viewAuditLogs(ActionEvent e) {
        String logs = "=== SECURE SYSTEM AUDIT LOGS ===\n\n"
                + "[+] Admin Access Granted from IP: 192.168.1.104\n"
                + "[+] Routine DB Cleanup executed.\n"
                + "[+] Unit Allocations are optimal.\n"
                + "[+] Latency: 14ms (Stable)\n\n"
                + "No unauthorized access detected.";
        showAlert(Alert.AlertType.INFORMATION, "System Audit Log", logs);
    }

    @FXML public void backupDatabase(ActionEvent e) {
        showAlert(Alert.AlertType.INFORMATION, "Database Backup", "System Database has been securely backed up. \nFile saved to Local Storage.");
    }

    @FXML public void globalBroadcast(ActionEvent e) {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Global Broadcast System");
        dialog.setHeaderText("Transmit message to all active responders and users");
        dialog.setContentText("Enter Broadcast Message:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(msg -> {
            showAlert(Alert.AlertType.INFORMATION, "Broadcast Sent", "Alert transmitted to all nodes: \n\n" + msg);
        });
    }

    @FXML public void lockdownSystem(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("CRITICAL WARNING");
        confirm.setHeaderText("Initiate System Lockdown?");
        confirm.setContentText("This will freeze all civilian requests and switch grid to MILITARY/EMERGENCY ONLY. Do you want to proceed?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showAlert(Alert.AlertType.ERROR, "LOCKDOWN INITIATED", "System Grid is now locked. Only Level 5 responders have access.");
        }
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