package org.crmsneakers.crmsneakers.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.crmsneakers.crmsneakers.MainApplication;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.User;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label messageLabel;
    
    private DatabaseService databaseService;
    
    public void initialize() {
        databaseService = DatabaseService.getInstance();
    }
    
    @FXML
    protected void handleRegisterNavigation() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("register-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            stage.setTitle("CRM Sneakers - Registration");
            stage.show();
        } catch (IOException e) {
            messageLabel.setText("Error loading registration page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    protected void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password");
            return;
        }
        
        User user = databaseService.findUserByUsername(username);
        
        if (user != null && user.getPassword().equals(password)) {
            // Authentication successful
            try {
                // Store the authenticated user in a session or similar mechanism
                UserSession.getInstance().setCurrentUser(user);
                
                // Load the appropriate dashboard based on user role
                String viewName;
                switch (user.getRole()) {
                    case ADMIN:
                        viewName = "admin-dashboard.fxml";
                        break;
                    case OPERATOR:
                        viewName = "operator-dashboard.fxml";
                        break;
                    case CUSTOMER:
                        viewName = "customer-dashboard.fxml";
                        break;
                    default:
                        messageLabel.setText("Invalid user role");
                        return;
                }
                
                FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(viewName));
                Parent root = loader.load();
                
                Stage stage = (Stage) usernameField.getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("CRM Sneakers - " + user.getRole());
                stage.show();
                
            } catch (IOException e) {
                messageLabel.setText("Error loading dashboard: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            messageLabel.setText("Invalid username or password");
        }
    }
}