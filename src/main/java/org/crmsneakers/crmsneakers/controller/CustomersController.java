package org.crmsneakers.crmsneakers.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Customer;
import org.crmsneakers.crmsneakers.model.Rental;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomersController {

    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> statusFilter;
    
    @FXML
    private ComboBox<String> membershipFilter;
    
    @FXML
    private TableView<Customer> customersTable;
    
    @FXML
    private TableColumn<Customer, String> nameColumn;
    
    @FXML
    private TableColumn<Customer, String> emailColumn;
    
    @FXML
    private TableColumn<Customer, String> phoneColumn;
    
    @FXML
    private TableColumn<Customer, String> membershipColumn;
    
    @FXML
    private TableColumn<Customer, Integer> rentalsColumn;
    
    @FXML
    private TableColumn<Customer, String> lastActivityColumn;
    
    @FXML
    private TableColumn<Customer, Void> actionsColumn;
    
    @FXML
    private Button addCustomerButton;
    
    @FXML
    private ComboBox<String> itemsPerPageComboBox;
    
    @FXML
    private Pagination pagination;
    
    private DatabaseService databaseService;
    private ObservableList<Customer> customersList;
    
    @FXML
    public void initialize() {
        databaseService = DatabaseService.getInstance();
        customersList = FXCollections.observableArrayList();
        
        // Configurar los filtros
        statusFilter.getItems().addAll("All", "Active", "Inactive");
        statusFilter.getSelectionModel().selectFirst();
        
        membershipFilter.getItems().addAll("All", "Regular", "Premium", "VIP");
        membershipFilter.getSelectionModel().selectFirst();
        
        // Configurar las columnas de la tabla
        nameColumn.setCellValueFactory(data -> {
            if (data.getValue() != null) {
                return new SimpleStringProperty(data.getValue().getFirstName() + " " + data.getValue().getLastName());
            }
            return new SimpleStringProperty("");
        });
        
        emailColumn.setCellValueFactory(data -> {
            if (data.getValue() != null && data.getValue().getEmail() != null) {
                return new SimpleStringProperty(data.getValue().getEmail());
            }
            return new SimpleStringProperty("");
        });
        
        phoneColumn.setCellValueFactory(data -> {
            if (data.getValue() != null && data.getValue().getPhone() != null) {
                return new SimpleStringProperty(data.getValue().getPhone());
            }
            return new SimpleStringProperty("");
        });
        
        membershipColumn.setCellValueFactory(data -> 
            new SimpleStringProperty("Regular")); // Por ahora lo dejamos como "Regular" para todos
        
        rentalsColumn.setCellValueFactory(data -> {
            if (data.getValue() != null && data.getValue().getId() != null) {
                // Obtener el número de alquileres para este cliente
                List<Rental> rentals = databaseService.findRentalsByCustomerId(data.getValue().getId());
                return new SimpleIntegerProperty(rentals.size()).asObject();
            }
            return new SimpleIntegerProperty(0).asObject();
        });
        
        lastActivityColumn.setCellValueFactory(data -> {
            if (data.getValue() != null && data.getValue().getId() != null) {
                // Obtener la fecha del último alquiler como "última actividad"
                List<Rental> rentals = databaseService.findRentalsByCustomerId(data.getValue().getId());
                if (rentals.isEmpty()) {
                    return new SimpleStringProperty("No activity");
                } else {
                    try {
                        // Ordenar por fecha y tomar el más reciente
                        rentals.sort((r1, r2) -> r2.getStartDate().compareTo(r1.getStartDate()));
                        LocalDate lastDate = rentals.get(0).getStartDate();
                        return new SimpleStringProperty(lastDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    } catch (Exception e) {
                        System.out.println("Error al procesar fecha: " + e.getMessage());
                        return new SimpleStringProperty("Error");
                    }
                }
            }
            return new SimpleStringProperty("No activity");
        });
        
        // Configurar la columna de acciones
        setupActionsColumn();
        
        // Cargar los datos iniciales
        loadCustomers();
        
        // Configurar el evento de búsqueda
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchCustomers());
        
        // Configurar eventos de filtro
        statusFilter.setOnAction(event -> applyFilters());
        membershipFilter.setOnAction(event -> applyFilters());
        
        // Configurar el botón de añadir cliente
        addCustomerButton.setOnAction(event -> showAddCustomerDialog());
    }
    
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button viewButton = new Button("View");
            private final Button deleteButton = new Button("Delete");

            {
                editButton.getStyleClass().add("action-button");
                viewButton.getStyleClass().add("action-button");
                deleteButton.getStyleClass().add("action-button");
                
                editButton.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    showEditCustomerDialog(customer);
                });
                
                viewButton.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    showCustomerDetails(customer);
                });
                
                deleteButton.setOnAction(event -> {
                    Customer customer = getTableView().getItems().get(getIndex());
                    showDeleteConfirmation(customer);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Crear un contenedor horizontal para los botones
                    ButtonBar buttonBar = new ButtonBar();
                    buttonBar.getButtons().addAll(editButton, viewButton, deleteButton);
                    setGraphic(buttonBar);
                }
            }
        });
    }
    
    private void loadCustomers() {
        System.out.println("Cargando clientes...");
        List<Customer> customers = databaseService.findAllCustomers();
        
        // Log para depurar: ver cuántos clientes se recuperaron
        System.out.println("Clientes recuperados de la base de datos: " + customers.size());
        
        // Si no hay clientes, crear algunos de prueba
        if (customers.isEmpty()) {
            System.out.println("No se encontraron clientes. Creando clientes de prueba...");
            customers = createTestCustomers();
            
            // Guardar los clientes de prueba en la base de datos
            for (Customer customer : customers) {
                databaseService.insertCustomer(customer);
            }
            
            // Volver a cargar desde la base de datos para asegurar que tienen ID
            customers = databaseService.findAllCustomers();
            System.out.println("Clientes después de crear pruebas: " + customers.size());
        }
        
        // Asegurar que la lista customersList se actualice
        customersList.clear();
        customersList.addAll(customers);
        
        // Asignar la lista a la tabla
        customersTable.setItems(null); // Limpiar cualquier vínculo previo
        customersTable.setItems(customersList); // Establecer los nuevos datos
        
        // Forzar actualización de la tabla
        customersTable.refresh();
        
        System.out.println("Tabla de clientes actualizada con " + customersList.size() + " clientes");
    }
    
    // Método para crear clientes de prueba
    private List<Customer> createTestCustomers() {
        List<Customer> testCustomers = new ArrayList<>();
        
        // Cliente 1
        Customer customer1 = new Customer();
        customer1.setFirstName("Juan");
        customer1.setLastName("Pérez");
        customer1.setEmail("juan.perez@example.com");
        customer1.setPhone("666111222");
        customer1.setAddress("Calle Mayor 123, Madrid");
        testCustomers.add(customer1);
        
        // Cliente 2
        Customer customer2 = new Customer();
        customer2.setFirstName("María");
        customer2.setLastName("López");
        customer2.setEmail("maria.lopez@example.com");
        customer2.setPhone("666333444");
        customer2.setAddress("Avenida Principal 45, Barcelona");
        testCustomers.add(customer2);
        
        // Cliente 3
        Customer customer3 = new Customer();
        customer3.setFirstName("Carlos");
        customer3.setLastName("Gómez");
        customer3.setEmail("carlos.gomez@example.com");
        customer3.setPhone("666555666");
        customer3.setAddress("Plaza Mayor 7, Valencia");
        testCustomers.add(customer3);
        
        return testCustomers;
    }
    
    private void searchCustomers() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            customersTable.setItems(customersList);
            return;
        }
        
        // Filtrar la lista por texto de búsqueda
        List<Customer> filteredList = customersList.stream()
                .filter(customer -> 
                    customer.getFirstName().toLowerCase().contains(searchText) || 
                    customer.getLastName().toLowerCase().contains(searchText) ||
                    customer.getEmail().toLowerCase().contains(searchText) ||
                    customer.getPhone().toLowerCase().contains(searchText))
                .collect(Collectors.toList());
        
        ObservableList<Customer> filteredCustomers = FXCollections.observableArrayList(filteredList);
        customersTable.setItems(filteredCustomers);
    }
    
    private void applyFilters() {
        String status = statusFilter.getValue();
        String membership = membershipFilter.getValue();
        
        // Por ahora, solo implementamos un filtro básico
        // En una implementación completa, se añadirían más campos al modelo Customer
        if ("All".equals(status) && "All".equals(membership)) {
            customersTable.setItems(customersList);
            return;
        }
        
        // Ejemplo de filtrado (a expandir según tus necesidades)
        ObservableList<Customer> filteredCustomers = customersList;
        customersTable.setItems(filteredCustomers);
    }
    
    @FXML
    private void showAddCustomerDialog() {
        try {
            Dialog<Customer> dialog = new Dialog<>();
            dialog.setTitle("Add New Customer");
            dialog.setHeaderText("Enter customer details");
            dialog.getDialogPane().getStyleClass().add("dialog-pane");
            
            // Load the FXML for add customer
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/add-customer-view.fxml"));
            GridPane dialogContent = loader.load();
            dialog.getDialogPane().setContent(dialogContent);
            
            // Get references to form fields
            TextField firstNameField = (TextField) dialogContent.lookup("#firstNameField");
            TextField lastNameField = (TextField) dialogContent.lookup("#lastNameField");
            TextField emailField = (TextField) dialogContent.lookup("#emailField");
            TextField phoneField = (TextField) dialogContent.lookup("#phoneField");
            TextField addressField = (TextField) dialogContent.lookup("#addressField");
            Button saveButton = (Button) dialogContent.lookup("#saveButton");
            Button cancelButton = (Button) dialogContent.lookup("#cancelButton");
            
            // Set the button types for the dialog
            ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
            
            // Connect the save button to the add button
            saveButton.setOnAction(event -> {
                // Simulate clicking the Add button
                Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
                addButton.fire();
            });
            
            // Connect the cancel button
            cancelButton.setOnAction(event -> dialog.close());
            
            // Enable/Disable add button depending on whether required fields are filled
            Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
            addButton.setDisable(true);
            
            // Validation for required fields
            firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                boolean valid = !newValue.trim().isEmpty() && !lastNameField.getText().trim().isEmpty();
                addButton.setDisable(!valid);
            });
            
            lastNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                boolean valid = !newValue.trim().isEmpty() && !firstNameField.getText().trim().isEmpty();
                addButton.setDisable(!valid);
            });
            
            // Convert the result to a customer object when the add button is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    try {
                        Customer customer = new Customer();
                        customer.setFirstName(firstNameField.getText().trim());
                        customer.setLastName(lastNameField.getText().trim());
                        customer.setEmail(emailField.getText().trim());
                        customer.setPhone(phoneField.getText().trim());
                        customer.setAddress(addressField.getText().trim());
                        return customer;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Could not create customer");
                        alert.setContentText("An error occurred: " + e.getMessage());
                        alert.showAndWait();
                    }
                }
                return null;
            });
            
            // Show dialog and handle result
            Optional<Customer> result = dialog.showAndWait();
            
            result.ifPresent(customer -> {
                // Insert the new customer into the database
                ObjectId id = databaseService.insertCustomer(customer);
                if (id != null) {
                    // Reload the customers list to show the new one
                    loadCustomers();
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load form");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void showEditCustomerDialog(Customer customer) {
        try {
            Dialog<Customer> dialog = new Dialog<>();
            dialog.setTitle("Edit Customer");
            dialog.setHeaderText("Edit customer details");
            dialog.getDialogPane().getStyleClass().add("dialog-pane");
            
            // Load the FXML for edit customer (reusing the add customer view)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/add-customer-view.fxml"));
            GridPane dialogContent = loader.load();
            dialog.getDialogPane().setContent(dialogContent);
            
            // Get references to form fields
            TextField firstNameField = (TextField) dialogContent.lookup("#firstNameField");
            TextField lastNameField = (TextField) dialogContent.lookup("#lastNameField");
            TextField emailField = (TextField) dialogContent.lookup("#emailField");
            TextField phoneField = (TextField) dialogContent.lookup("#phoneField");
            TextField addressField = (TextField) dialogContent.lookup("#addressField");
            Button saveButton = (Button) dialogContent.lookup("#saveButton");
            Button cancelButton = (Button) dialogContent.lookup("#cancelButton");
            
            // Fill the form with customer data
            firstNameField.setText(customer.getFirstName());
            lastNameField.setText(customer.getLastName());
            emailField.setText(customer.getEmail());
            phoneField.setText(customer.getPhone());
            addressField.setText(customer.getAddress());
            
            // Set the button types for the dialog
            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // Connect the save button to the save button in dialog
            saveButton.setOnAction(event -> {
                // Simulate clicking the Save button
                Button dialogSaveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
                dialogSaveButton.fire();
            });
            
            // Connect the cancel button
            cancelButton.setOnAction(event -> dialog.close());
            
            // Enable/Disable save button depending on whether required fields are filled
            Node saveDialogButton = dialog.getDialogPane().lookupButton(saveButtonType);
            
            // Validation for required fields
            firstNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                boolean valid = !newValue.trim().isEmpty() && !lastNameField.getText().trim().isEmpty();
                saveDialogButton.setDisable(!valid);
            });
            
            lastNameField.textProperty().addListener((observable, oldValue, newValue) -> {
                boolean valid = !newValue.trim().isEmpty() && !firstNameField.getText().trim().isEmpty();
                saveDialogButton.setDisable(!valid);
            });
            
            // Convert the result to a customer object when the save button is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        // Create a new customer object with updated values
                        Customer updatedCustomer = new Customer();
                        updatedCustomer.setId(customer.getId()); // Keep the same ID
                        updatedCustomer.setFirstName(firstNameField.getText().trim());
                        updatedCustomer.setLastName(lastNameField.getText().trim());
                        updatedCustomer.setEmail(emailField.getText().trim());
                        updatedCustomer.setPhone(phoneField.getText().trim());
                        updatedCustomer.setAddress(addressField.getText().trim());
                        
                        // Keep the same userId if it exists
                        if (customer.getUserId() != null) {
                            updatedCustomer.setUserId(customer.getUserId());
                        }
                        
                        return updatedCustomer;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Could not update customer");
                        alert.setContentText("An error occurred: " + e.getMessage());
                        alert.showAndWait();
                    }
                }
                return null;
            });
            
            // Show dialog and handle result
            Optional<Customer> result = dialog.showAndWait();
            
            result.ifPresent(updatedCustomer -> {
                // Update the customer in the database
                boolean success = databaseService.updateCustomer(updatedCustomer);
                if (success) {
                    // Reload the customers list to show the updated data
                    loadCustomers();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Update Failed");
                    alert.setContentText("Could not update the customer. Please try again.");
                    alert.showAndWait();
                }
            });
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load form");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void showCustomerDetails(Customer customer) {
        // Implementación de visualización de detalles del cliente
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Customer Details");
        alert.setHeaderText("Details for: " + customer.getFirstName() + " " + customer.getLastName());
        alert.setContentText(
            "Email: " + customer.getEmail() + "\n" +
            "Phone: " + customer.getPhone() + "\n" +
            "Address: " + customer.getAddress()
        );
        alert.showAndWait();
    }
    
    private void showDeleteConfirmation(Customer customer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Customer");
        alert.setHeaderText("Delete Customer");
        alert.setContentText("Are you sure you want to delete " + customer.getFirstName() + " " + customer.getLastName() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Check for associated rentals first
            List<Rental> rentals = databaseService.findRentalsByCustomerId(customer.getId());
            if (!rentals.isEmpty()) {
                Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                warningAlert.setTitle("Cannot Delete Customer");
                warningAlert.setHeaderText("Customer Has Active Rentals");
                warningAlert.setContentText("This customer has " + rentals.size() + " rental records. Cannot delete customers with rental history.");
                warningAlert.showAndWait();
                return;
            }
            
            // Delete the customer from database
            boolean success = databaseService.deleteCustomer(customer.getId());
            if (success) {
                loadCustomers(); // Reload the customers list
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Delete Failed");
                errorAlert.setContentText("Could not delete the customer. Please try again.");
                errorAlert.showAndWait();
            }
        }
    }
}