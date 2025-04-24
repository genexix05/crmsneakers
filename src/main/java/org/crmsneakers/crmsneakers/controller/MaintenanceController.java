package org.crmsneakers.crmsneakers.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.bson.types.ObjectId;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Product;
import org.crmsneakers.crmsneakers.model.Rental;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MaintenanceController {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private TableView<MaintenanceItem> maintenanceTable;
    
    @FXML
    private TableColumn<MaintenanceItem, ImageView> imageColumn;
    
    @FXML
    private TableColumn<MaintenanceItem, String> brandColumn;
    
    @FXML
    private TableColumn<MaintenanceItem, String> modelColumn;
    
    @FXML
    private TableColumn<MaintenanceItem, String> sizeColumn;
    
    @FXML
    private TableColumn<MaintenanceItem, String> issueColumn;
    
    @FXML
    private TableColumn<MaintenanceItem, String> rentalIdColumn;
    
    @FXML
    private TableColumn<MaintenanceItem, MaintenanceItem> actionsColumn;
    
    @FXML
    private Label totalItemsLabel;
    
    private DatabaseService databaseService;
    private ObservableList<MaintenanceItem> maintenanceItems;
    
    // Clase interna para manejar productos en mantenimiento con su información de alquiler asociada
    public static class MaintenanceItem {
        private final Product product;
        private final Rental rental;
        
        public MaintenanceItem(Product product, Rental rental) {
            this.product = product;
            this.rental = rental;
        }
        
        public Product getProduct() {
            return product;
        }
        
        public Rental getRental() {
            return rental;
        }
    }
    
    @FXML
    public void initialize() {
        try {
            databaseService = DatabaseService.getInstance();
            maintenanceItems = FXCollections.observableArrayList();
            
            // Configurar las columnas de la tabla
            setupTableColumns();
            
            // Agregar evento al campo de búsqueda
            if (searchField != null) {
                searchField.textProperty().addListener((_, __, ___) -> filterItems());
            }
            
            // Configurar el evento de doble click en la tabla
            maintenanceTable.setRowFactory(__ -> {
                TableRow<MaintenanceItem> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && (!row.isEmpty())) {
                        showMaintenanceActionDialog(row.getItem());
                    }
                });
                return row;
            });
            
            // Cargar los datos iniciales
            loadMaintenanceItems();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Error initializing maintenance view: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    private void setupTableColumns() {
        // Columna de imagen
        imageColumn.setCellValueFactory(data -> {
            Product product = data.getValue().getProduct();
            ImageView imageView = new ImageView();
            imageView.setFitHeight(60);
            imageView.setFitWidth(80);
            imageView.setPreserveRatio(true);
            
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                try {
                    Image image = new Image(product.getImageUrl());
                    imageView.setImage(image);
                } catch (Exception e) {
                    // Usar imagen de placeholder si falla la carga
                    imageView.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
                }
            } else {
                // Usar imagen de placeholder
                imageView.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
            
            return new SimpleObjectProperty<>(imageView);
        });
        
        // Columna de marca
        brandColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProduct().getBrand()));
        
        // Columna de modelo
        modelColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProduct().getModel()));
        
        // Columna de talla
        sizeColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProduct().getSize()));
        
        // Columna de descripción del problema
        issueColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getRental().getNotes()));
        
        // Columna de ID del alquiler (oculta por defecto)
        rentalIdColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getRental().getId().toString()));
        
        // Columna de acciones
        actionsColumn.setCellFactory(__ -> new TableCell<>() {
            private final Button completeButton = new Button("Complete Repair");
            private final Button detailsButton = new Button("Details");
            
            {
                completeButton.getStyleClass().add("action-button");
                detailsButton.getStyleClass().add("action-button");
                
                completeButton.setOnAction(__ -> handleCompleteRepair(getTableRow().getItem()));
                detailsButton.setOnAction(__ -> showDetailsDialog(getTableRow().getItem()));
            }
            
            @Override
            protected void updateItem(MaintenanceItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, completeButton, detailsButton);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        });
    }
    
    private void loadMaintenanceItems() {
        // Obtener todos los alquileres que tienen estado UNDER_REPAIR
        List<Rental> rentalsUnderRepair = databaseService.findAllRentals().stream()
            .filter(rental -> rental.getStatus() == Rental.RentalStatus.UNDER_REPAIR)
            .collect(Collectors.toList());
        
        maintenanceItems.clear();
        
        // Para cada alquiler, obtener el producto asociado y crear un MaintenanceItem
        for (Rental rental : rentalsUnderRepair) {
            Product product = databaseService.findProductById(rental.getProductId());
            if (product != null && product.getStatus() == Product.ProductStatus.UNDER_REPAIR) {
                maintenanceItems.add(new MaintenanceItem(product, rental));
            }
        }
        
        maintenanceTable.setItems(maintenanceItems);
        updateTotalItemsLabel();
    }
    
    private void filterItems() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            maintenanceTable.setItems(maintenanceItems);
            updateTotalItemsLabel();
            return;
        }
        
        // Filtrar los elementos que coincidan con el texto de búsqueda
        List<MaintenanceItem> filteredItems = maintenanceItems.stream()
            .filter(item -> 
                item.getProduct().getBrand().toLowerCase().contains(searchText) ||
                item.getProduct().getModel().toLowerCase().contains(searchText) ||
                item.getProduct().getSize().toLowerCase().contains(searchText) ||
                (item.getRental().getNotes() != null && 
                 item.getRental().getNotes().toLowerCase().contains(searchText))
            )
            .collect(Collectors.toList());
        
        maintenanceTable.setItems(FXCollections.observableArrayList(filteredItems));
        updateTotalItemsLabel();
    }
    
    private void updateTotalItemsLabel() {
        totalItemsLabel.setText("Total Items: " + maintenanceTable.getItems().size());
    }
    
    private void handleCompleteRepair(MaintenanceItem item) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Complete Repair");
        confirmAlert.setHeaderText("Complete Repair for " + item.getProduct().getBrand() + " " + item.getProduct().getModel());
        confirmAlert.setContentText("Has the repair been completed? The sneaker will be moved back to stock.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Mostrar un diálogo para ingresar notas de reparación
            TextInputDialog notesDialog = new TextInputDialog();
            notesDialog.setTitle("Repair Notes");
            notesDialog.setHeaderText("Add repair resolution notes");
            notesDialog.setContentText("Please enter details about the repair:");
            
            Optional<String> notesResult = notesDialog.showAndWait();
            String notes = notesResult.orElse("");
            String finalNotes = "Repair completed. " + notes;
            
            // Actualizar el estado del alquiler
            databaseService.updateRentalStatus(item.getRental().getId(), 
                Rental.RentalStatus.COMPLETED, finalNotes);
            
            // Actualizar el estado del producto
            databaseService.updateProductStatus(item.getProduct().getId(), 
                Product.ProductStatus.ACTIVE);
            
            // Mostrar mensaje de éxito
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Success");
            successAlert.setHeaderText(null);
            successAlert.setContentText("The sneaker has been repaired and returned to stock successfully.");
            successAlert.showAndWait();
            
            // Recargar la lista de productos en mantenimiento
            loadMaintenanceItems();
        }
    }
    
    private void showDetailsDialog(MaintenanceItem item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Maintenance Details");
        dialog.setHeaderText("Details for " + item.getProduct().getBrand() + " " + item.getProduct().getModel());
        
        // Crear contenido del diálogo
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(400);
        
        // Imagen del producto
        ImageView productImage = new ImageView();
        productImage.setFitHeight(150);
        productImage.setFitWidth(200);
        productImage.setPreserveRatio(true);
        
        if (item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().isEmpty()) {
            try {
                Image image = new Image(item.getProduct().getImageUrl());
                productImage.setImage(image);
            } catch (Exception e) {
                productImage.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
        } else {
            productImage.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
        }
        
        // Crear la tabla de información
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);
        infoGrid.addRow(0, new Label("Brand:"), new Label(item.getProduct().getBrand()));
        infoGrid.addRow(1, new Label("Model:"), new Label(item.getProduct().getModel()));
        infoGrid.addRow(2, new Label("Size:"), new Label(item.getProduct().getSize()));
        infoGrid.addRow(3, new Label("Rental ID:"), new Label(item.getRental().getId().toString()));
        
        // Área para las notas de mantenimiento
        Label notesLabel = new Label("Maintenance Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setText(item.getRental().getNotes() != null ? item.getRental().getNotes() : "");
        notesArea.setEditable(false);
        notesArea.setPrefRowCount(5);
        notesArea.setWrapText(true);
        
        // Añadir todos los elementos al contenido
        content.getChildren().addAll(productImage, infoGrid, notesLabel, notesArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        dialog.showAndWait();
    }

    private void showMaintenanceActionDialog(MaintenanceItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sneaker Maintenance Actions");
        dialog.setHeaderText("Maintenance Actions for " + item.getProduct().getBrand() + " " + item.getProduct().getModel());
        
        // Crear contenido del diálogo
        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(400);
        
        // Imagen del producto
        ImageView productImage = new ImageView();
        productImage.setFitHeight(150);
        productImage.setFitWidth(200);
        productImage.setPreserveRatio(true);
        
        if (item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().isEmpty()) {
            try {
                Image image = new Image(item.getProduct().getImageUrl());
                productImage.setImage(image);
            } catch (Exception e) {
                productImage.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
        } else {
            productImage.setImage(new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
        }
        
        // Información del producto
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(10);
        infoGrid.addRow(0, new Label("Brand:"), new Label(item.getProduct().getBrand()));
        infoGrid.addRow(1, new Label("Model:"), new Label(item.getProduct().getModel()));
        infoGrid.addRow(2, new Label("Size:"), new Label(item.getProduct().getSize()));
        infoGrid.addRow(3, new Label("Rental ID:"), new Label(item.getRental().getId().toString()));
        
        // Área para las notas de mantenimiento
        Label notesLabel = new Label("Maintenance Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setText(item.getRental().getNotes() != null ? item.getRental().getNotes() : "");
        notesArea.setEditable(false);
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        
        content.getChildren().addAll(productImage, infoGrid, notesLabel, notesArea);
        
        // Botones de acción
        ButtonType returnToStockType = new ButtonType("Return to Stock", ButtonBar.ButtonData.LEFT);
        ButtonType discardType = new ButtonType("Discard Sneaker", ButtonBar.ButtonData.RIGHT);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        dialog.getDialogPane().getButtonTypes().addAll(returnToStockType, discardType, cancelType);
        
        // Estilizar el botón de descartar en rojo
        Button discardButton = (Button) dialog.getDialogPane().lookupButton(discardType);
        if (discardButton != null) {
            discardButton.getStyleClass().add("danger-button");
        }
        
        dialog.getDialogPane().setContent(content);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == returnToStockType) {
                handleReturnToStock(item);
            } else if (result.get() == discardType) {
                handleDiscardSneaker(item);
            }
        }
    }

    private void handleReturnToStock(MaintenanceItem item) {
        TextInputDialog notesDialog = new TextInputDialog();
        notesDialog.setTitle("Return to Stock");
        notesDialog.setHeaderText("Add repair resolution notes");
        notesDialog.setContentText("Please enter details about the repair:");
        
        Optional<String> notesResult = notesDialog.showAndWait();
        String notes = notesResult.orElse("");
        String finalNotes = "Repair completed and returned to stock. " + notes;
        
        // Actualizar el estado del alquiler
        databaseService.updateRentalStatus(item.getRental().getId(), 
            Rental.RentalStatus.COMPLETED, finalNotes);
        
        // Actualizar el estado del producto
        databaseService.updateProductStatus(item.getProduct().getId(), 
            Product.ProductStatus.ACTIVE);
        
        showSuccessAlert("The sneaker has been repaired and returned to stock successfully.");
        loadMaintenanceItems();
    }

    private void handleDiscardSneaker(MaintenanceItem item) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Discard Sneaker");
        confirmAlert.setHeaderText("Are you sure you want to discard this sneaker?");
        confirmAlert.setContentText("This action cannot be undone. The sneaker will be permanently removed from inventory.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TextInputDialog notesDialog = new TextInputDialog();
            notesDialog.setTitle("Discard Notes");
            notesDialog.setHeaderText("Add discard notes");
            notesDialog.setContentText("Please enter the reason for discarding:");
            
            Optional<String> notesResult = notesDialog.showAndWait();
            String notes = notesResult.orElse("");
            String finalNotes = "Sneaker discarded due to irreparable damage. " + notes;
            
            // Actualizar el estado del alquiler
            databaseService.updateRentalStatus(item.getRental().getId(), 
                Rental.RentalStatus.COMPLETED, finalNotes);
            
            // Actualizar el estado del producto a DISCARDED o eliminarlo
            databaseService.updateProductStatus(item.getProduct().getId(), 
                Product.ProductStatus.DISCARDED);
            
            showSuccessAlert("The sneaker has been discarded successfully.");
            loadMaintenanceItems();
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}