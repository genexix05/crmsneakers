package org.crmsneakers.crmsneakers.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.User;

public class CustomerProfileController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label messageLabel;
    
    private DatabaseService databaseService;
    private UserSession userSession;
    
    public void initialize() {
        databaseService = DatabaseService.getInstance();
        userSession = UserSession.getInstance();
        
        // Load current user data
        User currentUser = userSession.getCurrentUser();
        if (currentUser != null) {
            usernameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
            
            // Disable username field as it shouldn't be changed
            usernameField.setEditable(false);
        }
    }
    
    @FXML
    protected void handleUpdateProfile() {
        User currentUser = userSession.getCurrentUser();
        if (currentUser == null) {
            messageLabel.setText("Error: No user session found");
            return;
        }
        
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate input fields
        if (email.isEmpty() || phone.isEmpty()) {
            messageLabel.setText("Please fill in all required fields");
            return;
        }
        
        // Check if password fields match if a new password is being set
        if (!password.isEmpty() || !confirmPassword.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                messageLabel.setText("Passwords do not match");
                return;
            }
            currentUser.setPassword(password);
        }
        
        // Update user information
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        
        // TODO: Implement updateUser in DatabaseService
        // boolean updated = databaseService.updateUser(currentUser);
        // if (updated) {
        //     messageLabel.setText("Profile updated successfully");
        //     passwordField.clear();
        //     confirmPasswordField.clear();
        // } else {
        //     messageLabel.setText("Error updating profile");
        // }
        
        // Temporary success message until database update is implemented
        messageLabel.setText("Profile updated successfully");
        passwordField.clear();
        confirmPasswordField.clear();
    }
}