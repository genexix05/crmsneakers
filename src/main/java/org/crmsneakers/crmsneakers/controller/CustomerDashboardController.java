package org.crmsneakers.crmsneakers.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.crmsneakers.crmsneakers.MainApplication;
import org.crmsneakers.crmsneakers.model.*;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class CustomerDashboardController {
    @FXML private Label userLabel;
    @FXML private StackPane contentArea;
    @FXML private FlowPane popularSneakersPane;
    @FXML private FlowPane recentRentalsPane;
    @FXML private Text availableSneakersCount;
    @FXML private Text availableSneakersChange;
    @FXML private Text activeRentalsCount;
    @FXML private Text activeRentalsInfo;
    @FXML private Text rentalHistoryCount;
    @FXML private Text rentalHistoryChange;
    
    private UserSession userSession;
    private DatabaseService databaseService;
    
    public void initialize() {
        userSession = UserSession.getInstance();
        databaseService = DatabaseService.getInstance();
        User currentUser = userSession.getCurrentUser();
        
        if (currentUser != null) {
            userLabel.setText("Welcome, " + currentUser.getUsername());
            updateDashboardStats();
            loadPopularSneakers();
            loadRecentRentals();
        }
    }

    private void updateDashboardStats() {
        // Get current customer
        Customer customer = findCustomerByUserId(userSession.getCurrentUser().getId());
        if (customer == null) return;

        // Update available sneakers stats
        List<Product> availableSneakers = databaseService.findAllProducts().stream()
            .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
            .collect(Collectors.toList());
        availableSneakersCount.setText(String.valueOf(availableSneakers.size()));
        
        // Update active rentals stats
        List<Rental> activeRentals = databaseService.findRentalsByCustomerId(customer.getId()).stream()
            .filter(r -> r.getStatus() == Rental.RentalStatus.ACTIVE)
            .collect(Collectors.toList());
        activeRentalsCount.setText(String.valueOf(activeRentals.size()));
        
        // Show next due rental if any
        if (!activeRentals.isEmpty()) {
            Rental nextDueRental = activeRentals.stream()
                .min((r1, r2) -> r1.getEndDate().compareTo(r2.getEndDate()))
                .get();
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextDueRental.getEndDate());
            activeRentalsInfo.setText(daysUntilDue + " days until next return");
        } else {
            activeRentalsInfo.setText("No active rentals");
        }
        
        // Update rental history stats
        List<Rental> completedRentals = databaseService.findRentalsByCustomerId(customer.getId()).stream()
            .filter(r -> r.getStatus() == Rental.RentalStatus.COMPLETED)
            .collect(Collectors.toList());
        rentalHistoryCount.setText(String.valueOf(completedRentals.size()));
        
        // Count last month's rentals
        long lastMonthRentals = completedRentals.stream()
            .filter(r -> r.getEndDate().isAfter(LocalDate.now().minusMonths(1)))
            .count();
        rentalHistoryChange.setText("Last month: " + lastMonthRentals);
    }

    private void loadPopularSneakers() {
        popularSneakersPane.getChildren().clear();
        
        // Get most rented sneakers
        List<Product> popularProducts = databaseService.findAllProducts().stream()
            .filter(p -> p.getStatus() == Product.ProductStatus.ACTIVE)
            .limit(6)  // Show top 6 popular sneakers
            .collect(Collectors.toList());
            
        for (Product product : popularProducts) {
            popularSneakersPane.getChildren().add(createSneakerCard(product));
        }
    }

    private void loadRecentRentals() {
        recentRentalsPane.getChildren().clear();
        
        // Get customer's recent rentals
        Customer customer = findCustomerByUserId(userSession.getCurrentUser().getId());
        if (customer == null) return;
        
        List<Rental> recentRentals = databaseService.findRentalsByCustomerId(customer.getId()).stream()
            .sorted((r1, r2) -> r2.getStartDate().compareTo(r1.getStartDate()))
            .limit(4)  // Show last 4 rentals
            .collect(Collectors.toList());
            
        for (Rental rental : recentRentals) {
            Product product = databaseService.findProductById(rental.getProductId());
            if (product != null) {
                recentRentalsPane.getChildren().add(createRentalCard(rental, product));
            }
        }
    }

    private Pane createSneakerCard(Product product) {
        VBox card = new VBox(10);
        card.getStyleClass().add("sneaker-card");
        
        // Sneaker image
        ImageView imageView = new ImageView();
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(product.getImageUrl());
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
        } else {
            imageView.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
        }
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        
        // Brand and model
        Label nameLabel = new Label(product.getBrand() + " " + product.getModel());
        nameLabel.getStyleClass().add("sneaker-name");
        
        // Size and price
        Label detailsLabel = new Label("Size " + product.getSize() + " - $" + String.format("%.2f", product.getRentalPrice()) + "/day");
        detailsLabel.getStyleClass().add("sneaker-details");
        
        card.getChildren().addAll(imageView, nameLabel, detailsLabel);
        return card;
    }

    private Pane createRentalCard(Rental rental, Product product) {
        VBox card = new VBox(10);
        card.getStyleClass().add("rental-card");
        
        // Sneaker image
        ImageView imageView = new ImageView();
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(product.getImageUrl());
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
        } else {
            imageView.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
        }
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);
        imageView.setPreserveRatio(true);
        
        // Rental details
        Label nameLabel = new Label(product.getBrand() + " " + product.getModel());
        nameLabel.getStyleClass().add("sneaker-name");
        
        Label datesLabel = new Label("From: " + rental.getStartDate() + "\nTo: " + rental.getEndDate());
        datesLabel.getStyleClass().add("rental-dates");
        
        Label statusLabel = new Label(rental.getStatus().toString());
        statusLabel.getStyleClass().add("rental-status");
        
        card.getChildren().addAll(imageView, nameLabel, datesLabel, statusLabel);
        return card;
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
    protected void handleLogout() {
        userSession.clearSession();
        try {
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
        loadView("products-view.fxml");
    }
    
    @FXML
    protected void showMyRentalsView() {
        loadView("customer-rentals-view.fxml");
    }
    
    @FXML
    protected void showProfileView() {
        loadView("customer-profile-view.fxml");
    }
    
    @FXML
    protected void showDashboardView() {
        try {
            contentArea.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(MainApplication.class.getResource("customer-dashboard.fxml"));
            BorderPane root = loader.load();
            StackPane newContentArea = (StackPane) root.getCenter();
            if (newContentArea != null) {
                contentArea.getChildren().addAll(newContentArea.getChildren());
            }
            CustomerDashboardController controller = loader.getController();
            controller.setUserSession(userSession);
            Scene scene = contentArea.getScene();
            if (scene != null) {
                Button dashboardButton = (Button) scene.lookup("Button[text='Dashboard']");
                if (dashboardButton != null) {
                    scene.lookup(".nav-items").lookupAll(".nav-button").forEach(node -> 
                        node.getStyleClass().remove("active")
                    );
                    dashboardButton.getStyleClass().add("active");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading dashboard content: " + e.getMessage());
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
    
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
        User currentUser = userSession.getCurrentUser();
        if (currentUser != null) {
            userLabel.setText("Welcome, " + currentUser.getUsername());
        }
    }
}