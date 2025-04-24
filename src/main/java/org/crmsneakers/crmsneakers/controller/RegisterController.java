package org.crmsneakers.crmsneakers.controller;

import javafx.application.Platform;
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
import org.crmsneakers.crmsneakers.model.Customer;
import org.bson.types.ObjectId;
import java.io.IOException;

public class RegisterController {
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
    
    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField addressField;
    
    private DatabaseService databaseService;
    
    public void initialize() {
        databaseService = DatabaseService.getInstance();
    }
    
    @FXML
    protected void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String address = addressField.getText().trim();
        
        // Validate input fields
        if (username.isEmpty() || email.isEmpty() || phone.isEmpty() || 
            password.isEmpty() || confirmPassword.isEmpty() ||
            firstName.isEmpty() || lastName.isEmpty() || address.isEmpty()) {
            messageLabel.setText("Please fill in all fields");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match");
            return;
        }
        
        // Check if username already exists
        if (databaseService.findUserByUsername(username) != null) {
            messageLabel.setText("Username already exists");
            return;
        }
        
        // Create new user with CUSTOMER role
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setPassword(password);
        newUser.setRole(User.UserRole.CUSTOMER);
        
        // Save user to database
        ObjectId userId = databaseService.saveUser(newUser);
        
        if (userId != null) {
            // Create and save the associated customer
            Customer newCustomer = new Customer();
            newCustomer.setFirstName(firstName);
            newCustomer.setLastName(lastName);
            newCustomer.setEmail(email);
            newCustomer.setPhone(phone);
            newCustomer.setAddress(address);
            newCustomer.setUserId(userId); // Link the customer to the user
            
            ObjectId customerId = databaseService.insertCustomer(newCustomer);
            
            if (customerId != null) {
                // Show success message and redirect to login
                messageLabel.setText("Registration successful! Please login.");
                
                // Clear fields
                clearFields();
                
                // Redirect to login after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(() -> redirectToLogin());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                // Si falla la creación del cliente, eliminar el usuario creado
                if (databaseService.deleteUser(userId)) {
                    messageLabel.setText("Error creating customer profile. Please try again.");
                } else {
                    messageLabel.setText("Critical error: User created but customer profile failed. Please contact support.");
                }
            }
        } else {
            messageLabel.setText("Error creating user account. Please try again.");
        }
    }
    
    @FXML
    protected void handleBackToLogin() {
        redirectToLogin();
    }
    
    private void clearFields() {
        usernameField.clear();
        emailField.clear();
        phoneField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        firstNameField.clear();
        lastNameField.clear();
        addressField.clear();
    }
    
    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            stage.setTitle("CRM Sneakers - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Error returning to login page");
        }
    }
}