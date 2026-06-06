package com.nahid.rescuenet.controller;

import com.nahid.rescuenet.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;

    public static int loggedInUserId;
    public static String loggedInUserName;

    @FXML
    public void loginButtonClicked(ActionEvent event) {
        String phone = phoneField.getText();
        String password = passwordField.getText();

        if (phone == null || phone.isEmpty() || password == null || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Failed", "Please enter both phone number and password.");
            return;
        }

        try {
            String query = "SELECT * FROM users WHERE phone = ? AND password = ?";
            PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(query);
            ps.setString(1, phone);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                loggedInUserId = rs.getInt("id");
                loggedInUserName = rs.getString("name");
                String role = rs.getString("role");

                String fxmlFile = "";


                switch (role) {
                    case "Admin": fxmlFile = "/com/nahid/rescuenet/admindashboard.fxml"; break;
                    case "Police": fxmlFile = "/com/nahid/rescuenet/policedashboard.fxml"; break;
                    case "Fire": fxmlFile = "/com/nahid/rescuenet/firedashboard.fxml"; break;
                    case "Ambulance": fxmlFile = "/com/nahid/rescuenet/ambulancedashboard.fxml"; break;
                    case "Hospital": fxmlFile = "/com/nahid/rescuenet/hospitaldashboard.fxml"; break;
                    case "Citizen": fxmlFile = "/com/nahid/rescuenet/citizendashboard.fxml"; break; // সিটিজেন অ্যাড করা হয়েছে
                    default: fxmlFile = "/com/nahid/rescuenet/citizendashboard.fxml"; break;
                }

                Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root, 1100, 750));
                stage.centerOnScreen();
            } else {
                showAlert(Alert.AlertType.ERROR, "Access Denied", "Invalid phone number or password. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect to the RescueNet database. Make sure XAMPP MySQL is running.");
        }
    }

    @FXML
    public void goToRegister(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/nahid/rescuenet/register.fxml"));
        ((Node) event.getSource()).getScene().setRoot(root);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}