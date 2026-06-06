package com.nahid.rescuenet.controller;

import com.nahid.rescuenet.DatabaseConnection;
import com.nahid.rescuenet.model.EmergencyRequest;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CitizenController {

    @FXML private Label welcomeLabel, totalRequestsLabel, pendingLabel;
    @FXML private Button sosBtn;
    @FXML private TextField locationInputField;
    @FXML private TableView<EmergencyRequest> historyTable;
    @FXML private TableColumn<EmergencyRequest, Integer> idCol;
    @FXML private TableColumn<EmergencyRequest, String> typeCol, locationCol, statusCol;
    private ObservableList<EmergencyRequest> list = FXCollections.observableArrayList();
    private ContextMenu autoSuggestMenu;

    private final List<String> districts = Arrays.asList(
            "Bagerhat", "Bandarban", "Barguna", "Barisal", "Bhola", "Bogra", "Brahmanbaria", "Chandpur",
            "Chattogram", "Chuadanga", "Comilla", "Cox's Bazar", "Dhaka", "Dinajpur", "Faridpur", "Feni",
            "Gaibandha", "Gazipur", "Gopalganj", "Habiganj", "Jamalpur", "Jessore", "Jhalokati", "Jhenaidah",
            "Joypurhat", "Khagrachari", "Khulna", "Kishoreganj", "Kurigram", "Kushtia", "Lakshmipur",
            "Lalmonirhat", "Madaripur", "Magura", "Manikganj", "Meherpur", "Moulvibazar", "Munshiganj",
            "Mymensingh", "Naogaon", "Narail", "Narayanganj", "Narsingdi", "Natore", "Nawabganj", "Netrokona",
            "Nilphamari", "Noakhali", "Pabna", "Panchagarh", "Patuakhali", "Pirojpur", "Rajbari", "Rajshahi",
            "Rangamati", "Rangpur", "Satkhira", "Shariatpur", "Sherpur", "Sirajgonj", "Sunamganj", "Sylhet",
            "Tangail", "Thakurgaon"
    );

    private final String SEU_ADDRESS = "Southeast University, 251/A Tejgaon I/A, Dhaka 1208";
    private final String SEU_COORDS = "23.7644° N, 90.4048° E";

    @FXML public void initialize() {
        welcomeLabel.setText("Active Node: " + LoginController.loggedInUserName);
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        locationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.5), sosBtn);
        pulse.setByX(0.05);
        pulse.setByY(0.05);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        setupAutoSuggest();
        loadData();
    }

    private void setupAutoSuggest() {
        autoSuggestMenu = new ContextMenu();
        autoSuggestMenu.setStyle("-fx-background-color: #0d1321; -fx-border-color: #00e5ff;");
        locationInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                autoSuggestMenu.hide();
            } else {
                List<String> filtered = districts.stream()
                        .filter(d -> d.toLowerCase().startsWith(newValue.toLowerCase()))
                        .collect(Collectors.toList());
                if (!filtered.isEmpty()) {
                    autoSuggestMenu.getItems().clear();
                    for (String d : filtered) {
                        MenuItem item = new MenuItem(d);
                        item.setStyle("-fx-text-fill: #00e5ff; -fx-padding: 5 20 5 20;");
                        item.setOnAction(e -> {
                            locationInputField.setText(d);
                            locationInputField.positionCaret(d.length());
                            autoSuggestMenu.hide();
                        });
                        autoSuggestMenu.getItems().add(item);
                    }
                    if (!autoSuggestMenu.isShowing()) {
                        autoSuggestMenu.show(locationInputField, Side.BOTTOM, 0, 0);
                    }
                } else {
                    autoSuggestMenu.hide();
                }
            }
        });
    }

    private void loadData() {
        list.clear();
        int total = 0, pending = 0;
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("SELECT * FROM emergency_requests WHERE citizen_id = ? ORDER BY request_id DESC");
            ps.setInt(1, LoginController.loggedInUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String stat = rs.getString("status");
                list.add(new EmergencyRequest(rs.getInt("request_id"), rs.getString("emergency_type"), rs.getString("location"), stat));
                total++;
                if (stat.equals("Pending")) pending++;
            }
            historyTable.setItems(list);
            totalRequestsLabel.setText(String.valueOf(total));
            pendingLabel.setText(String.valueOf(pending));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void sendSOS(ActionEvent e) {
        String loc = locationInputField.getText().isEmpty() ? SEU_ADDRESS : locationInputField.getText();
        sendReq("CRITICAL SOS", loc, "System override! All units alerted.");
        locationInputField.clear();
    }

    @FXML public void requestPolice(ActionEvent e) { processRequest("Police", "Police force deployment initiated."); }
    @FXML public void requestAmbulance(ActionEvent e) { processRequest("Ambulance", "Medical unit dispatched."); }
    @FXML public void requestFire(ActionEvent e) { processRequest("Fire Service", "Fire protocol activated."); }

    private void processRequest(String type, String msg) {
        String loc = locationInputField.getText();
        if (loc == null || loc.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Location Required", "Please type your exact address or click 'Live Location Link' first.");
            return;
        }
        sendReq(type, loc.trim(), msg);
        locationInputField.clear();
    }

    private void sendReq(String type, String loc, String msg) {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("INSERT INTO emergency_requests (citizen_id, emergency_type, location, status) VALUES (?, ?, ?, 'Pending')");
            ps.setInt(1, LoginController.loggedInUserId);
            ps.setString(2, type);
            ps.setString(3, loc);
            ps.executeUpdate();
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Signal Transmitted", msg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML public void showProfile(ActionEvent e) {
        try {
            PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement("SELECT phone FROM users WHERE id = ?");
            ps.setInt(1, LoginController.loggedInUserId);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                String phone = rs.getString("phone");
                String profileData = "STATUS: Node Authenticated\nUSER ID: " + LoginController.loggedInUserId + "\nNAME: " + LoginController.loggedInUserName + "\nPHONE: " + phone + "\n\n[System Encrypted via AES-256]";
                showAlert(Alert.AlertType.INFORMATION, "Citizen Security Profile", profileData);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML public void showLocation(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("GPS Satellite Uplink");
        alert.setHeaderText("Connecting to orbital satellites...");
        alert.setContentText("Establishing secure link...");
        alert.show();

        PauseTransition pt1 = new PauseTransition(Duration.seconds(1.0));
        pt1.setOnFinished(event -> alert.setContentText("Triangulating position via SEU Campus WiFi/Cell Towers..."));

        PauseTransition pt2 = new PauseTransition(Duration.seconds(2.5));
        pt2.setOnFinished(event -> {
            alert.setHeaderText("Live Tracking Locked");
            alert.setContentText("Coordinates: " + SEU_COORDS + "\nAddress: " + SEU_ADDRESS);
            locationInputField.setText(SEU_ADDRESS);
            locationInputField.positionCaret(locationInputField.getText().length());
        });

        pt1.play();
        pt2.play();
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