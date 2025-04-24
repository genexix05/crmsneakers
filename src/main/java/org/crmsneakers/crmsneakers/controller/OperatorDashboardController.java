package org.crmsneakers.crmsneakers.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.crmsneakers.crmsneakers.MainApplication;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Product;
import org.crmsneakers.crmsneakers.model.Rental;
import org.crmsneakers.crmsneakers.model.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class OperatorDashboardController {
    @FXML
    private Text userLabel;
    
    @FXML
    private Button userButton;

    @FXML
    private Button maintenanceButton;
    
    // Referencias a las estadísticas
    @FXML
    private Text availableSneakersValue;
    
    @FXML
    private Text availableSneakersChange;
    
    @FXML
    private Text activeRentalsValue;
    
    @FXML
    private Text activeRentalsChange;
    
    @FXML
    private Text pendingReturnsValue;
    
    @FXML
    private Text pendingReturnsChange;
    
    @FXML
    private TableView<Object> recentActivitiesTable;
    
    @FXML
    private TableView<Object> pendingTasksTable;
    
    private UserSession userSession;
    private DatabaseService databaseService;
    
    public void initialize() {
        userSession = UserSession.getInstance();
        databaseService = DatabaseService.getInstance();
        
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser != null) {
            userLabel.setText("Welcome back, " + currentUser.getUsername());
        }
        
        // Cargar y mostrar las estadísticas actuales
        loadDashboardStatistics();
    }

    /**
     * Carga las estadísticas del dashboard desde la base de datos
     */
    private void loadDashboardStatistics() {
        // 1. Obtener productos disponibles
        List<Product> availableProducts = databaseService.findAvailableProducts();
        int availableCount = availableProducts.size();
        
        // 2. Obtener todos los productos para calcular porcentajes
        List<Product> allProducts = databaseService.findAllProducts();
        int totalProductsCount = allProducts.size();
        
        // 3. Obtener alquileres activos
        List<Rental> activeRentals = databaseService.findAllRentals().stream()
            .filter(r -> r.getStatus() == Rental.RentalStatus.ACTIVE)
            .collect(Collectors.toList());
        int activeRentalsCount = activeRentals.size();
        
        // 4. Obtener alquileres con devolución pendiente (fecha fin anterior a hoy)
        LocalDate today = LocalDate.now();
        List<Rental> pendingReturns = activeRentals.stream()
            .filter(r -> r.getEndDate().isBefore(today) || r.getEndDate().isEqual(today))
            .collect(Collectors.toList());
        int pendingReturnsCount = pendingReturns.size();
        
        // 5. Obtener alquileres con devolución atrasada (fecha fin más de 2 días antes)
        List<Rental> overdueReturns = pendingReturns.stream()
            .filter(r -> r.getEndDate().isBefore(today.minusDays(2)))
            .collect(Collectors.toList());
        int overdueReturnsCount = overdueReturns.size();
        
        // Actualizar las tarjetas de estadísticas
        if (availableSneakersValue != null) {
            // Actualizar valor de zapatillas disponibles
            availableSneakersValue.setText(String.valueOf(availableCount));
            
            // Calcular y mostrar el porcentaje de disponibilidad
            if (totalProductsCount > 0) {
                int availablePercentage = (availableCount * 100) / totalProductsCount;
                availableSneakersChange.setText("↑ " + availablePercentage + "% del inventario");
            } else {
                availableSneakersChange.setText("No hay productos en inventario");
            }
        }
        
        if (activeRentalsValue != null) {
            // Actualizar valor de alquileres activos
            activeRentalsValue.setText(String.valueOf(activeRentalsCount));
            
            // Mostrar información sobre cambios recientes en alquileres
            // Aquí podríamos comparar con datos históricos, pero para simplificar usaremos una métrica fija
            LocalDate yesterday = LocalDate.now().minusDays(1);
            long newRentalsToday = activeRentals.stream()
                .filter(r -> r.getStartDate().isEqual(today))
                .count();
                
            activeRentalsChange.setText("↑ " + newRentalsToday + " desde ayer");
        }
        
        if (pendingReturnsValue != null) {
            // Actualizar valor de devoluciones pendientes
            pendingReturnsValue.setText(String.valueOf(pendingReturnsCount));
            
            // Mostrar información sobre devoluciones atrasadas
            if (overdueReturnsCount > 0) {
                pendingReturnsChange.setText(overdueReturnsCount + " con retraso");
                pendingReturnsChange.getStyleClass().add("warning");
            } else if (pendingReturnsCount > 0) {
                pendingReturnsChange.setText("Todas dentro de plazo");
                pendingReturnsChange.getStyleClass().add("positive");
            } else {
                pendingReturnsChange.setText("No hay devoluciones pendientes");
            }
        }
        
        // También podríamos cargar los datos para las tablas de actividades recientes y tareas pendientes
        // loadRecentActivities();
        // loadPendingTasks();
    }

    /**
     * Actualiza la clase de estilo para el botón activo
     */
    private void updateActiveButton(Button activeButton) {
        // Obtener todos los botones de navegación de la barra lateral
        BorderPane root = (BorderPane) userButton.getScene().getRoot();
        VBox sidebar = (VBox) root.getLeft();
        VBox navItems = (VBox) sidebar.lookup(".nav-items");
        
        // Quitar clase "active" de todos los botones de navegación
        if (navItems != null) {
            for (javafx.scene.Node node : navItems.getChildren()) {
                if (node instanceof Button) {
                    Button navButton = (Button) node;
                    navButton.getStyleClass().remove("active");
                }
            }
        }
        
        // Añadir clase "active" al botón actual
        if (activeButton != null) {
            if (!activeButton.getStyleClass().contains("active")) {
                activeButton.getStyleClass().add("active");
            }
        }
    }

    /**
     * Muestra un diálogo de error
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    protected void handleLogout() {
        // Clear the user session
        userSession.clearSession();
        
        try {
            // Load the login view
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) userLabel.getScene().getWindow();
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            stage.setTitle("CRM Sneakers - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    protected void showProductsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/products-view.fxml"));
            Parent productsView = loader.load();
            
            BorderPane root = (BorderPane) userButton.getScene().getRoot();
            root.setCenter(productsView);
            
            // Actualizar el botón activo
            Button productsButton = (Button) root.lookup("Button[text='Inventory']");
            updateActiveButton(productsButton);
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error loading products view: " + e.getMessage());
        }
    }
    
    @FXML
    protected void showRentalsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/rentals-view.fxml"));
            Parent rentalsView = loader.load();
            
            BorderPane root = (BorderPane) userButton.getScene().getRoot();
            root.setCenter(rentalsView);
            
            // Actualizar el botón activo
            Button rentalsButton = (Button) root.lookup("Button[text='Rentals']");
            updateActiveButton(rentalsButton);
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error loading rentals view: " + e.getMessage());
        }
    }
    
    @FXML
    protected void showCustomersView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/customers-view.fxml"));
            Parent customersView = loader.load();
            
            BorderPane root = (BorderPane) userButton.getScene().getRoot();
            root.setCenter(customersView);
            
            // Actualizar el botón activo
            Button customersButton = (Button) root.lookup("Button[text='Customers']");
            updateActiveButton(customersButton);
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error loading customers view: " + e.getMessage());
        }
    }
    
    @FXML
    protected void showDashboardView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/operator-dashboard.fxml"));
            Parent dashboardView = loader.load();
            
            // Transferir la sesión de usuario al nuevo controlador si es necesario
            OperatorDashboardController controller = loader.getController();
            controller.setUserSession(userSession);
            
            // Actualizar el contenido central 
            BorderPane root = (BorderPane) userButton.getScene().getRoot();
            root.setCenter(dashboardView.lookup("VBox"));
            
            // Actualizar el botón activo
            Button dashboardButton = (Button) root.lookup("Button[text='Dashboard']");
            updateActiveButton(dashboardButton);
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error loading dashboard view: " + e.getMessage());
        }
    }
    
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser != null) {
            userLabel.setText("Welcome back, " + currentUser.getUsername());
        }
    }

    @FXML
    private void showMaintenance() {
        try {
            // Cargar la vista de mantenimiento
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/crmsneakers/crmsneakers/maintenance-view.fxml"));
            Parent maintenanceView = loader.load();
            
            // Reemplazar el contenido central con la vista de mantenimiento
            BorderPane root = (BorderPane) userButton.getScene().getRoot();
            root.setCenter(maintenanceView);
            
            // Actualizar el botón activo
            Button maintenanceButton = (Button) root.lookup("Button[text='Maintenance']");
            if (maintenanceButton != null) {
                updateActiveButton(maintenanceButton);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error loading maintenance view: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Unexpected error: " + e.getMessage());
        }
    }
}