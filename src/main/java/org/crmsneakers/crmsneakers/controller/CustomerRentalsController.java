package org.crmsneakers.crmsneakers.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.bson.types.ObjectId;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Customer;
import org.crmsneakers.crmsneakers.model.Product;
import org.crmsneakers.crmsneakers.model.Rental;
import org.crmsneakers.crmsneakers.model.User;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerRentalsController {
    @FXML
    private Button dashboardButton;

    @FXML
    private TableView<Rental> rentalsTable;

    @FXML
    private TableColumn<Rental, String> productColumn;

    @FXML
    private TableColumn<Rental, String> startDateColumn;

    @FXML
    private TableColumn<Rental, String> endDateColumn;

    @FXML
    private TableColumn<Rental, String> statusColumn;

    private DatabaseService databaseService;
    private UserSession userSession;
    private Customer currentCustomer;
    private ObservableList<Rental> rentalsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        databaseService = DatabaseService.getInstance();
        userSession = UserSession.getInstance();
        
        // Configurar las columnas de la tabla
        productColumn.setCellValueFactory(data -> {
            Product product = databaseService.findProductById(data.getValue().getProductId());
            if (product != null) {
                return new SimpleStringProperty(product.getBrand() + " " + product.getModel());
            }
            return new SimpleStringProperty("Unknown product");
        });
        
        startDateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        
        endDateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        
        statusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus().toString()));

        // Cargar los alquileres del cliente actual
        loadCustomerRentals();
    }

    private void loadCustomerRentals() {
        User currentUser = userSession.getCurrentUser();
        if (currentUser == null) {
            System.err.println("No user is currently logged in");
            return;
        }

        // Buscar al cliente asociado al usuario actual
        // En un sistema real, esto podría ser una relación directa o una búsqueda por campo
        currentCustomer = findCustomerByUserId(currentUser.getId());
        
        if (currentCustomer != null) {
            // Cargar los alquileres del cliente
            List<Rental> rentals = databaseService.findRentalsByCustomerId(currentCustomer.getId());
            rentalsList.clear();
            rentalsList.addAll(rentals);
            rentalsTable.setItems(rentalsList);
        } else {
            System.err.println("No customer record found for the current user");
        }
    }
    
    private Customer findCustomerByUserId(ObjectId userId) {
        if (userId == null) {
            System.err.println("Warning: Attempting to find customer with null userId");
            return null;
        }
        return databaseService.findAllCustomers().stream()
            .filter(c -> c.getUserId() != null && c.getUserId().equals(userId))
            .findFirst()
            .orElse(null);
    }

    @FXML
    public void backToDashboard() {
        // Este método será implementado por el contenedor padre (CustomerDashboardController)
        // para manejar la navegación
    }
}