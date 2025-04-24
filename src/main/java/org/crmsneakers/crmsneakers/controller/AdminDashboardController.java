package org.crmsneakers.crmsneakers.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.beans.property.SimpleStringProperty;
import java.time.format.DateTimeFormatter;
import org.crmsneakers.crmsneakers.MainApplication;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Customer;
import org.crmsneakers.crmsneakers.model.Product;
import org.crmsneakers.crmsneakers.model.Rental;
import org.crmsneakers.crmsneakers.model.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminDashboardController {
    @FXML
    private Label userLabel;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private Button maintenanceButton;

    @FXML
    private LineChart<String, Number> revenueChart;

    @FXML
    private PieChart satisfactionChart;

    // Referencias a elementos de estadísticas
    @FXML
    private Text availableSneakersValue;
    
    @FXML
    private Text availableSneakersChange;
    
    @FXML
    private Text activeRentalsValue;
    
    @FXML
    private Text activeRentalsChange;
    
    @FXML
    private Text pendingReservationsValue;
    
    @FXML
    private Text pendingReservationsChange;
    
    @FXML
    private Text monthlyRevenueValue;
    
    @FXML
    private Text monthlyRevenueChange;
    
    @FXML
    private TableView<Rental> recentRentalsTable;
    
    @FXML
    private TableColumn<Rental, String> rentalCustomerColumn;
    
    @FXML
    private TableColumn<Rental, String> rentalProductColumn;
    
    @FXML
    private TableColumn<Rental, String> rentalStartDateColumn;
    
    @FXML
    private TableColumn<Rental, String> rentalEndDateColumn;
    
    @FXML
    private TableColumn<Rental, String> rentalStatusColumn;
    
    @FXML
    private TableView<Product> inventoryAlertsTable;
    
    @FXML
    private TableColumn<Product, String> alertProductColumn;
    
    @FXML
    private TableColumn<Product, String> alertStatusColumn;
    
    @FXML
    private TableColumn<Product, String> alertConditionColumn;
    
    @FXML
    private TableColumn<Product, String> alertActionColumn;
    
    private UserSession userSession;
    private DatabaseService databaseService;
    
    public void initialize() {
        userSession = UserSession.getInstance();
        databaseService = DatabaseService.getInstance();
        
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser != null) {
            userLabel.setText("Welcome, " + currentUser.getUsername());
        }
        
        // Cargar estadísticas del dashboard
        loadDashboardStatistics();
        
        // Inicializar gráficos
        loadRevenueChart();
        loadSatisfactionChart();
        
        setupRecentRentalsTable();
        setupInventoryAlertsTable();
        loadRecentRentals();
        loadInventoryAlerts();
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
        
        // 4. Calculamos los ingresos mensuales (alquileres que comenzaron este mes)
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        double monthlyRevenue = databaseService.findAllRentals().stream()
            .filter(r -> r.getStartDate().isAfter(firstDayOfMonth) || r.getStartDate().isEqual(firstDayOfMonth))
            .mapToDouble(Rental::getRentalPrice)
            .sum();
            
        // 5. Calculamos los ingresos del mes anterior para comparación
        LocalDate firstDayOfPreviousMonth = firstDayOfMonth.minusMonths(1);
        LocalDate lastDayOfPreviousMonth = firstDayOfMonth.minusDays(1);
        double previousMonthRevenue = databaseService.findAllRentals().stream()
            .filter(r -> (r.getStartDate().isAfter(firstDayOfPreviousMonth) || r.getStartDate().isEqual(firstDayOfPreviousMonth)) 
                && (r.getStartDate().isBefore(firstDayOfMonth)))
            .mapToDouble(Rental::getRentalPrice)
            .sum();
            
        // Calcular el cambio porcentual en ingresos
        double revenueChangePercent = 0;
        if (previousMonthRevenue > 0) {
            revenueChangePercent = ((monthlyRevenue - previousMonthRevenue) / previousMonthRevenue) * 100;
        }
        
        // Actualizar las tarjetas de estadísticas
        if (availableSneakersValue != null) {
            // Actualizar valor de zapatillas disponibles
            availableSneakersValue.setText(String.valueOf(availableCount));
            
            // Calcular y mostrar el porcentaje de disponibilidad
            if (totalProductsCount > 0) {
                int availablePercentage = (availableCount * 100) / totalProductsCount;
                availableSneakersChange.setText("↑ " + availablePercentage + "% del total");
            } else {
                availableSneakersChange.setText("No hay productos en inventario");
            }
        }
        
        if (activeRentalsValue != null) {
            // Actualizar valor de alquileres activos
            activeRentalsValue.setText(String.valueOf(activeRentalsCount));
            
            // Mostrar información sobre cambios recientes en alquileres
            LocalDate yesterday = LocalDate.now().minusDays(1);
            long newRentalsToday = activeRentals.stream()
                .filter(r -> r.getStartDate().isEqual(LocalDate.now()))
                .count();
                
            activeRentalsChange.setText("↑ " + newRentalsToday + " desde ayer");
        }
        
        if (pendingReservationsValue != null) {
            // Para simplificar, usamos una métrica fija ya que no tenemos un modelo de reservas
            int pendingReservationsCount = 32; // Valor ficticio
            pendingReservationsValue.setText(String.valueOf(pendingReservationsCount));
            pendingReservationsChange.setText("5 requieren atención");
        }
        
        if (monthlyRevenueValue != null) {
            // Formatear y mostrar ingresos mensuales
            monthlyRevenueValue.setText("$" + String.format("%,.0f", monthlyRevenue));
            
            // Mostrar cambio porcentual comparado con el mes anterior
            String changeDirection = revenueChangePercent >= 0 ? "↑" : "↓";
            monthlyRevenueChange.setText(changeDirection + " " + String.format("%.1f", Math.abs(revenueChangePercent)) + "% este mes");
            
            // Aplicar estilo apropiado según si es positivo o negativo
            if (revenueChangePercent >= 0) {
                monthlyRevenueChange.getStyleClass().add("positive");
            } else {
                monthlyRevenueChange.getStyleClass().add("negative");
            }
        }
    }
    
    /**
     * Carga el gráfico de ingresos con datos de los últimos 30 días
     */
    private void loadRevenueChart() {
        if (revenueChart == null) return;
        
        // Limpiar datos existentes
        revenueChart.getData().clear();
        
        // Crear serie para los datos de ingresos
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ingresos diarios");
        
        // Obtener fecha actual y hace 30 días
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        
        // Obtener todos los alquileres
        List<Rental> allRentals = databaseService.findAllRentals();
        
        // Crear mapa para acumular ingresos por fecha
        Map<LocalDate, Double> revenueByDate = new HashMap<>();
        
        // Inicializar el mapa con ceros para todos los días del rango
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            revenueByDate.put(date, 0.0);
        }
        
        // Calcular ingresos diarios basados en fechas de inicio de alquiler
        for (Rental rental : allRentals) {
            LocalDate rentalStart = rental.getStartDate();
            if (!rentalStart.isBefore(startDate) && !rentalStart.isAfter(today)) {
                // Sumar el precio del alquiler a la fecha correspondiente
                revenueByDate.put(rentalStart, revenueByDate.getOrDefault(rentalStart, 0.0) + rental.getRentalPrice());
            }
        }
        
        // Formatear fechas para el eje X
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        
        // Agregar datos a la serie
        for (int i = 0; i < 30; i++) {
            LocalDate date = startDate.plusDays(i);
            String formattedDate = date.format(formatter);
            double revenue = revenueByDate.getOrDefault(date, 0.0);
            series.getData().add(new XYChart.Data<>(formattedDate, revenue));
        }
        
        // Agregar la serie al gráfico
        revenueChart.getData().add(series);
    }
    
    /**
     * Carga el gráfico de satisfacción con datos simulados
     */
    private void loadSatisfactionChart() {
        if (satisfactionChart == null) return;
        
        // Crear datos para el gráfico de satisfacción
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
            new PieChart.Data("Excelente", 45),
            new PieChart.Data("Buena", 30),
            new PieChart.Data("Normal", 15),
            new PieChart.Data("Mejorable", 10)
        );
        
        // Asignar datos al gráfico
        satisfactionChart.setData(pieChartData);
    }
    
    /**
     * Actualiza la clase de estilo para el botón activo
     */
    private void updateActiveButton(Button activeButton) {
        // Lista de todos los botones de navegación
        Button dashboardButton = (Button) userLabel.getScene().lookup("Button[text='Dashboard']");
        Button inventoryButton = (Button) userLabel.getScene().lookup("Button[text='Inventory']");
        Button customersButton = (Button) userLabel.getScene().lookup("Button[text='Customers']");
        Button rentalsButton = (Button) userLabel.getScene().lookup("Button[text='Rentals']");
        Button settingsButton = (Button) userLabel.getScene().lookup("Button[text='Settings']");
        
        Button[] navButtons = {
            dashboardButton, inventoryButton, customersButton, 
            rentalsButton, maintenanceButton, settingsButton
        };
        
        // Quitar clase "active" de todos los botones
        for (Button button : navButtons) {
            if (button != null) {
                button.getStyleClass().remove("active");
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
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
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
    protected void showUsersView() {
        loadView("users-view.fxml");
    }
    
    @FXML
    protected void showCustomersView() {
        loadView("customers-view.fxml");
    }
    
    @FXML
    protected void showProductsView() {
        loadView("products-view.fxml");
    }
    
    @FXML
    protected void showRentalsView() {
        loadView("rentals-view.fxml");
    }
    
    @FXML
    protected void showReportsView() {
        loadView("reports-view.fxml");
    }
    
    @FXML
    protected void showDashboardView() {
        // Clear the content area and show the dashboard content
        contentArea.getChildren().clear();
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("admin-dashboard.fxml"));
            Parent dashboardView = loader.load();
            AdminDashboardController controller = loader.getController();
            // Transfer the user session to the new controller
            controller.setUserSession(userSession);
            
            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene scene = new Scene(dashboardView);
            stage.setScene(scene);
            stage.setTitle("CRM Sneakers - Admin Dashboard");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void showMaintenance() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("maintenance-view.fxml"));
            Parent maintenanceView = loader.load();
            
            // Reemplazar el contenido central con la vista de mantenimiento
            contentArea.getChildren().clear();
            contentArea.getChildren().add(maintenanceView);
            
            // Actualizar el botón activo sin usar lookup
            if (maintenanceButton != null) {
                VBox sidebar = (VBox) maintenanceButton.getParent();
                for (javafx.scene.Node node : sidebar.getChildren()) {
                    if (node instanceof Button) {
                        node.getStyleClass().remove("active");
                    }
                }
                maintenanceButton.getStyleClass().add("active");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error loading maintenance view: " + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Unexpected error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser != null) {
            userLabel.setText("Welcome, " + currentUser.getUsername());
        }
    }
    
    private void loadView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource(fxmlFile));
            Parent view = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupRecentRentalsTable() {
        // Configure recent rentals table columns
        rentalCustomerColumn.setCellValueFactory(data -> {
            Rental rental = data.getValue();
            Customer customer = databaseService.findCustomerById(rental.getCustomerId());
            return new SimpleStringProperty(customer != null ? customer.getFirstName() : "Unknown");
        });
        
        rentalProductColumn.setCellValueFactory(data -> {
            Rental rental = data.getValue();
            Product product = databaseService.findProductById(rental.getProductId());
            return new SimpleStringProperty(product != null ? product.getBrand() + " " + product.getModel() : "Unknown");
        });
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        rentalStartDateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStartDate().format(dateFormatter)));
        
        rentalEndDateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getEndDate().format(dateFormatter)));
        
        rentalStatusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus().toString()));
    }

    private void setupInventoryAlertsTable() {
        alertProductColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getBrand() + " " + data.getValue().getModel()));
        
        alertStatusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus().toString()));
        
        alertConditionColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(getConditionText(data.getValue())));
        
        alertActionColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(getActionRequired(data.getValue())));
    }

    private void loadRecentRentals() {
        // Get last 5 rentals ordered by start date
        List<Rental> recentRentals = databaseService.findAllRentals().stream()
            .sorted((r1, r2) -> r2.getStartDate().compareTo(r1.getStartDate()))
            .limit(5)
            .collect(Collectors.toList());
        
        recentRentalsTable.setItems(FXCollections.observableArrayList(recentRentals));
    }

    private void loadInventoryAlerts() {
        List<Product> productsInMaintenance = databaseService.findAllProducts().stream()
            .filter(p -> p.getStatus() == Product.ProductStatus.UNDER_REPAIR)
            .collect(Collectors.toList());
        
        inventoryAlertsTable.setItems(FXCollections.observableArrayList(productsInMaintenance));
    }

    private String getConditionText(Product product) {
        switch (product.getStatus()) {
            case UNDER_REPAIR:
                return "En mantenimiento";
            case DAMAGED:
                return "Dañado";
            case ACTIVE:
                return "Bueno";
            case RENTED:
                return "En alquiler";
            default:
                return "Desconocido";
        }
    }

    private String getActionRequired(Product product) {
        switch (product.getStatus()) {
            case UNDER_REPAIR:
                return "Completar reparación";
            case DAMAGED:
                return "Evaluar daños";
            case ACTIVE:
                return "Ninguna";
            case RENTED:
                return "Esperar devolución";
            default:
                return "Verificar estado";
        }
    }
}