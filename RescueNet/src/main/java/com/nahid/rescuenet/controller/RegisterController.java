package com.nahid.rescuenet.controller;

import com.nahid.rescuenet.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.PreparedStatement;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField guardianPhoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList(
                    "Citizen", "Police", "Fire", "Ambulance", "Hospital", "Admin"
            ));
            roleComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    public void registerButtonClicked(ActionEvent event) {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String guardian = guardianPhoneField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || role == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please fill in all the required fields.");
            return;
        }

        try {
            String query = "INSERT INTO users (name, phone, email, guardian_phone, password, role) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = DatabaseConnection.getInstance().getConnection().prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, guardian);
            ps.setString(5, password);
            ps.setString(6, role);

            int result = ps.executeUpdate();
            if (result > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Registration Success", "Account created successfully! You can now log in.");
                goToLogin(event);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Registration failed. This phone number might already be registered.");
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/nahid/rescuenet/login.fxml"));
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