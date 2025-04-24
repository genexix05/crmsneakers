package org.crmsneakers.crmsneakers.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.User;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UsersController {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> roleFilter;
    
    @FXML
    private TableView<User> usersTable;
    
    @FXML
    private TableColumn<User, String> usernameColumn;
    
    @FXML
    private TableColumn<User, String> emailColumn;
    
    @FXML
    private TableColumn<User, String> phoneColumn;
    
    @FXML
    private TableColumn<User, String> roleColumn;
    
    @FXML
    private TableColumn<User, Void> actionsColumn;
    
    @FXML
    private Label totalItemsLabel;
    
    private DatabaseService databaseService;
    private ObservableList<User> usersList;
    
    @FXML
    public void initialize() {
        databaseService = DatabaseService.getInstance();
        usersList = FXCollections.observableArrayList();
        
        // Configurar el ComboBox de roles
        roleFilter.getItems().addAll("All", "ADMIN", "OPERATOR");
        roleFilter.setValue("All");
        
        // Configurar columnas de la tabla
        setupTableColumns();
        
        // Configurar eventos de filtrado
        searchField.textProperty().addListener((_, __, ___) -> filterUsers());
        roleFilter.getSelectionModel().selectedItemProperty().addListener((_, __, ___) -> filterUsers());
        
        // Cargar usuarios iniciales
        loadUsers();
    }
    
    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getUsername()));
            
        emailColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getEmail()));
            
        phoneColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getPhone()));
            
        roleColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getRole().toString()));
            
        // Configurar columna de acciones
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            
            {
                editButton.getStyleClass().add("edit-button");
                deleteButton.getStyleClass().add("danger-button");
                
                editButton.setOnAction(e -> showEditDialog(getTableRow().getItem()));
                deleteButton.setOnAction(e -> handleDeleteUser(getTableRow().getItem()));
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editButton, deleteButton);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void loadUsers() {
        // Obtener todos los usuarios que son ADMIN u OPERATOR
        List<User> users = databaseService.findAllUsers().stream()
            .filter(user -> user.getRole() == User.UserRole.ADMIN || 
                          user.getRole() == User.UserRole.OPERATOR)
            .collect(Collectors.toList());
            
        usersList.clear();
        usersList.addAll(users);
        usersTable.setItems(usersList);
        updateTotalLabel();
    }
    
    private void filterUsers() {
        String searchText = searchField.getText().toLowerCase();
        String selectedRole = roleFilter.getValue();
        
        List<User> filteredUsers = usersList.stream()
            .filter(user -> {
                boolean matchesSearch = searchText.isEmpty() ||
                    user.getUsername().toLowerCase().contains(searchText) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText)) ||
                    (user.getPhone() != null && user.getPhone().toLowerCase().contains(searchText));
                    
                boolean matchesRole = selectedRole == null || 
                    selectedRole.equals("All") ||
                    user.getRole().toString().equals(selectedRole);
                    
                return matchesSearch && matchesRole;
            })
            .collect(Collectors.toList());
            
        usersTable.setItems(FXCollections.observableArrayList(filteredUsers));
        updateTotalLabel();
    }
    
    @FXML
    private void showAddUserDialog() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new administrator or operator account");
        
        // Botones
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Crear grid para el formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        
        ComboBox<User.UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(User.UserRole.ADMIN, User.UserRole.OPERATOR);
        roleComboBox.setPromptText("Select Role");
        
        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleComboBox, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Habilitar/Deshabilitar botón de guardar según validación
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        
        // Validar campos
        usernameField.textProperty().addListener((_, __, ___) -> 
            saveButton.setDisable(usernameField.getText().trim().isEmpty() || 
                                passwordField.getText().trim().isEmpty() ||
                                roleComboBox.getValue() == null));
                                
        passwordField.textProperty().addListener((_, __, ___) -> 
            saveButton.setDisable(usernameField.getText().trim().isEmpty() || 
                                passwordField.getText().trim().isEmpty() ||
                                roleComboBox.getValue() == null));
                                
        roleComboBox.valueProperty().addListener((_, __, ___) -> 
            saveButton.setDisable(usernameField.getText().trim().isEmpty() || 
                                passwordField.getText().trim().isEmpty() ||
                                roleComboBox.getValue() == null));
        
        // Convertir el resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                User newUser = new User();
                newUser.setUsername(usernameField.getText().trim());
                newUser.setPassword(passwordField.getText().trim());
                newUser.setEmail(emailField.getText().trim());
                newUser.setPhone(phoneField.getText().trim());
                newUser.setRole(roleComboBox.getValue());
                return newUser;
            }
            return null;
        });
        
        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> {
            if (databaseService.saveUser(user) != null) {
                loadUsers();
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "User created successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to create user. Please try again.");
            }
        });
    }
    
    private void showEditDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user information");
        
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField emailField = new TextField(user.getEmail());
        TextField phoneField = new TextField(user.getPhone());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave blank to keep current password");
        
        ComboBox<User.UserRole> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(User.UserRole.ADMIN, User.UserRole.OPERATOR);
        roleComboBox.setValue(user.getRole());
        
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Phone:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleComboBox, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                User updatedUser = new User();
                updatedUser.setId(user.getId());
                updatedUser.setUsername(user.getUsername());
                updatedUser.setEmail(emailField.getText().trim());
                updatedUser.setPhone(phoneField.getText().trim());
                updatedUser.setRole(roleComboBox.getValue());
                
                String newPassword = passwordField.getText().trim();
                if (!newPassword.isEmpty()) {
                    updatedUser.setPassword(newPassword);
                } else {
                    updatedUser.setPassword(user.getPassword());
                }
                
                return updatedUser;
            }
            return null;
        });
        
        Optional<User> result = dialog.showAndWait();
        result.ifPresent(updatedUser -> {
            if (databaseService.updateUser(updatedUser)) {
                loadUsers();
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "User updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to update user. Please try again.");
            }
        });
    }
    
    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete User");
        alert.setContentText("Are you sure you want to delete the user: " + user.getUsername() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (databaseService.deleteUser(user.getId())) {
                loadUsers();
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                    "User deleted successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", 
                    "Failed to delete user. Please try again.");
            }
        }
    }
    
    private void updateTotalLabel() {
        int count = usersTable.getItems().size();
        totalItemsLabel.setText("Total Users: " + count);
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}