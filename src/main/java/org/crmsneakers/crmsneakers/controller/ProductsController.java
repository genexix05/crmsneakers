package org.crmsneakers.crmsneakers.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.Node;
import java.time.LocalDate;
import java.util.Optional;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Customer;
import org.crmsneakers.crmsneakers.model.Product;
import org.crmsneakers.crmsneakers.model.Rental;
import org.crmsneakers.crmsneakers.model.User;
import org.bson.types.ObjectId;
import java.util.List;
import java.util.stream.Collectors;

public class ProductsController {
    @FXML
    private GridPane addProductGrid;

    @FXML
    private TextField productNameField;
    @FXML
    private TextField brandField;
    @FXML
    private TextField modelField;
    @FXML
    private TextField sizeField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField stockField;
    @FXML
    private FlowPane sneakerGrid;

    @FXML
    private ComboBox<String> brandFilterComboBox;

    @FXML
    private ComboBox<String> sizeFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private TextField searchField;

    @FXML
    private Label totalItemsLabel;

    private DatabaseService databaseService;
    private ObservableList<Product> productsList;
    @FXML
    private TextField imageUrlField;

    @FXML
    private Text pageTitle;

    @FXML
    private Text pageSubtitle;

    // Propiedad para controlar si se muestra el botón de añadir producto (admins y
    // operadores)
    private BooleanProperty adminOrOperator = new SimpleBooleanProperty(false);

    public boolean isAdminOrOperator() {
        return adminOrOperator.get();
    }

    public BooleanProperty adminOrOperatorProperty() {
        return adminOrOperator;
    }

    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    public void initialize() {
        databaseService = DatabaseService.getInstance();
        productsList = FXCollections.observableArrayList();

        // Configurar padding del grid de zapatillas
        sneakerGrid.setPadding(new Insets(10));

        // Inicializar filtros
        brandFilterComboBox.getSelectionModel().selectFirst();
        sizeFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.getSelectionModel().selectFirst();

        // Comprobar rol del usuario y actualizar la UI
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Permitir que admin y operador vean el botón de añadir producto
            adminOrOperator.set(currentUser.getRole() == User.UserRole.ADMIN ||
                    currentUser.getRole() == User.UserRole.OPERATOR);

            if (currentUser.getRole() == User.UserRole.CUSTOMER) {
                pageTitle.setText("Available Sneakers");
                pageSubtitle.setText("Browse and rent your favorite sneakers");
            } else {
                pageSubtitle.setText("Manage your sneaker collection");
            }
        }

        // Cargar datos iniciales desde la base de datos
        refreshProductList();
    }

    /**
     * Crea la tarjeta (card) de cada zapatilla
     */
    private Pane createSneakerCard(Product product) {
        VBox card = new VBox(10);
        card.getStyleClass().add("sneaker-card");

        // Imagen de la zapatilla
        ImageView imageView = new ImageView();
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(product.getImageUrl());
                imageView.setImage(image);
            } catch (Exception e) {
                imageView.setImage(
                        new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
        } else {
            imageView.setImage(
                    new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
        }
        imageView.setFitWidth(250);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("sneaker-image");
        imageContainer.setMinHeight(180);

        // Badge de estado
        Label statusBadge = new Label(product.getStatus().toString());
        statusBadge.getStyleClass().addAll("status-badge", getStatusStyleClass(product.getStatus()));
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new Insets(10));
        imageContainer.getChildren().add(statusBadge);

        // Etiqueta de marca
        Label brandLabel = new Label(product.getBrand());
        brandLabel.getStyleClass().add("sneaker-brand");

        // Etiqueta del modelo
        Label nameLabel = new Label(product.getModel());
        nameLabel.getStyleClass().add("sneaker-name");

        // Información de la talla
        HBox sizeBox = new HBox(5);
        sizeBox.setAlignment(Pos.CENTER_LEFT);
        Label sizeLabel = new Label("Size " + product.getSize());
        sizeLabel.getStyleClass().add("sneaker-size");
        sizeBox.getChildren().add(sizeLabel);

        // Precio de alquiler
        Label priceLabel = new Label("$" + String.format("%.0f", product.getRentalPrice()));
        priceLabel.getStyleClass().add("sneaker-price");

        // Fila inferior con acciones
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getRole() == User.UserRole.ADMIN) {
                // Solo los administradores verán el botón de edición/eliminación
                Button moreButton = new Button("•••");
                moreButton.getStyleClass().add("more-button");
                moreButton.setOnAction(e -> showProductActions(product, moreButton));
                bottomRow.getChildren().addAll(priceLabel, spacer, moreButton);
            } else if (currentUser.getRole() == User.UserRole.CUSTOMER
                    && product.getStatus() == Product.ProductStatus.ACTIVE) {
                Button rentButton = new Button("Rent");
                rentButton.getStyleClass().add("rent-button");
                rentButton.setOnAction(e -> showRentDialog(product));
                bottomRow.getChildren().addAll(priceLabel, spacer, rentButton);
            } else {
                bottomRow.getChildren().addAll(priceLabel, spacer);
            }
        } else {
            bottomRow.getChildren().addAll(priceLabel, spacer);
        }

        card.getChildren().addAll(imageContainer, brandLabel, nameLabel, sizeBox, bottomRow);
        return card;
    }

    private String getStatusStyleClass(Product.ProductStatus status) {
        switch (status) {
            case ACTIVE:
                return "available";
            case UNDER_REPAIR:
                return "maintenance";
            case RENTED:
                return "rented";
            case DISABLED:
                return "disabled";
            default:
                return "";
        }
    }

    private void showProductActions(Product product, Button sourceButton) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> handleEditProduct(product));

        // Si el producto está deshabilitado, se muestra "Activate"; si no, "Disable"
        MenuItem toggleItem;
        if (product.getStatus() == Product.ProductStatus.DISABLED) {
            toggleItem = new MenuItem("Activate");
            toggleItem.setOnAction(e -> handleActivateProduct(product));
        } else {
            toggleItem = new MenuItem("Disable");
            toggleItem.setOnAction(e -> handleDisableProduct(product));
        }

        contextMenu.getItems().addAll(editItem, toggleItem);
        contextMenu.show(sourceButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void handleDisableProduct(Product product) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Disable Product");
        confirmAlert.setHeaderText("Disable Product");
        confirmAlert.setContentText(
                "Are you sure you want to disable this product? It will no longer be visible to customers.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean updated = databaseService.updateProductStatus(product.getId(), Product.ProductStatus.DISABLED);
            if (updated) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Product Disabled");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("Product has been disabled successfully.");
                infoAlert.showAndWait();
                refreshProductList();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to disable product");
                errorAlert.setContentText("An error occurred while disabling the product.");
                errorAlert.showAndWait();
            }
        }
    }

    private void handleActivateProduct(Product product) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Activate Product");
        confirmAlert.setHeaderText("Activate Product");
        confirmAlert.setContentText("Are you sure you want to activate this product?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean updated = databaseService.updateProductStatus(product.getId(), Product.ProductStatus.ACTIVE);
            if (updated) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Product Activated");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("Product has been activated successfully.");
                infoAlert.showAndWait();
                refreshProductList();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to activate product");
                errorAlert.setContentText("An error occurred while activating the product.");
                errorAlert.showAndWait();
            }
        }
    }

    @FXML
    protected void showAddProductDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter product details");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/crmsneakers/crmsneakers/add-product-view.fxml"));
            GridPane dialogContent = loader.load();
            dialog.getDialogPane().setContent(dialogContent);

            // Referencias a los campos del formulario
            TextField productNameField = (TextField) dialogContent.lookup("#productNameField");
            TextField brandField = (TextField) dialogContent.lookup("#brandField");
            TextField modelField = (TextField) dialogContent.lookup("#modelField");
            TextField sizeField = (TextField) dialogContent.lookup("#sizeField");
            TextField priceField = (TextField) dialogContent.lookup("#priceField");
            TextField stockField = (TextField) dialogContent.lookup("#stockField");
            TextField imageUrlField = (TextField) dialogContent.lookup("#imageUrlField");
            Button saveButton = (Button) dialogContent.lookup("#saveButton");
            Button cancelButton = (Button) dialogContent.lookup("#cancelButton");

            // Configuración para subir imagen
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Sneaker Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

            Button uploadButton = new Button("Choose Image");
            Label imageLabel = new Label();
            uploadButton.setOnAction(e -> {
                File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
                if (selectedFile != null) {
                    try {
                        String destPath = "src/main/resources/org/crmsneakers/crmsneakers/images/"
                                + selectedFile.getName();
                        Files.copy(selectedFile.toPath(), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
                        imageLabel.setText(selectedFile.getName());
                        imageUrlField.setText("/org/crmsneakers/crmsneakers/images/" + selectedFile.getName());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });

            HBox imageUploadBox = new HBox(10, uploadButton, imageLabel);
            imageUploadBox.setAlignment(Pos.CENTER_LEFT);
            dialogContent.add(imageUploadBox, 1, 8);

            ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            saveButton.setOnAction(event -> {
                Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
                addButton.fire();
            });
            cancelButton.setOnAction(event -> dialog.close());

            Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
            addButton.setDisable(true);
            brandField.textProperty().addListener((observable, oldValue, newValue) -> {
                addButton.setDisable(newValue.trim().isEmpty());
            });

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    try {
                        Product product = new Product();
                        if (productNameField.getText() != null && !productNameField.getText().isEmpty()) {
                            product.setName(productNameField.getText());
                        }
                        product.setBrand(brandField.getText());
                        product.setModel(modelField.getText());
                        product.setSize(sizeField.getText());
                        product.setRentalPrice(Double.parseDouble(priceField.getText()));
                        product.setImageUrl(imageUrlField.getText());
                        product.setStatus(Product.ProductStatus.ACTIVE);
                        // Si es necesario manejar stock, hacerlo aquí
                        return product;
                    } catch (NumberFormatException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Please check your inputs");
                        alert.setContentText("Make sure price and stock are valid numbers.");
                        alert.showAndWait();
                        return null;
                    }
                }
                return null;
            });

            Optional<Product> result = dialog.showAndWait();
            result.ifPresent(product -> {
                databaseService.saveProduct(product);
                refreshProductList();
            });

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Form");
            alert.setHeaderText("Could not load the add product form");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void refreshProductList() {
        // Cargar todos los productos desde la BD
        List<Product> products = databaseService.findAllProducts();
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.UserRole.CUSTOMER) {
            // Filtrar los productos deshabilitados para clientes
            products = products.stream()
                    .filter(product -> product.getStatus() != Product.ProductStatus.DISABLED)
                    .collect(Collectors.toList());
        }
        productsList.setAll(products);
        displayProducts(productsList);
        updateTotalItemsLabel();
    }

    private void displayProducts(List<Product> products) {
        sneakerGrid.getChildren().clear();
        for (Product product : products) {
            Pane card = createSneakerCard(product);
            sneakerGrid.getChildren().add(card);
        }
    }

    @FXML
    protected void filterProducts() {
        String selectedBrand = brandFilterComboBox.getValue();
        String selectedSize = sizeFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();

        String brand = "All".equals(selectedBrand) ? null : selectedBrand;
        String size = "All".equals(selectedSize) ? null : selectedSize;
        Product.ProductStatus status = null;
        if (!"All".equals(selectedStatus)) {
            try {
                status = Product.ProductStatus.valueOf(selectedStatus);
            } catch (Exception e) {
                // Si ocurre error, se deja null
            }
        }

        List<Product> filteredProducts = databaseService.findProducts(brand, size, status);
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.UserRole.CUSTOMER) {
            filteredProducts = filteredProducts.stream()
                    .filter(product -> product.getStatus() != Product.ProductStatus.DISABLED)
                    .collect(Collectors.toList());
        }
        productsList.setAll(filteredProducts);
        displayProducts(productsList);
        updateTotalItemsLabel();
    }

    @FXML
    protected void searchProducts() {
        String searchText = searchField.getText().trim();
        List<Product> searchResults = databaseService.searchProducts(searchText);
        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.UserRole.CUSTOMER) {
            searchResults = searchResults.stream()
                    .filter(product -> product.getStatus() != Product.ProductStatus.DISABLED)
                    .collect(Collectors.toList());
        }
        productsList.setAll(searchResults);
        displayProducts(productsList);
        updateTotalItemsLabel();
    }

    private void handleEditProduct(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Edit Product");
        dialog.setHeaderText("Edit product details");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/crmsneakers/crmsneakers/edit-product-view.fxml"));
            GridPane dialogContent = loader.load();
            dialog.getDialogPane().setContent(dialogContent);

            TextField productNameField = (TextField) dialogContent.lookup("#productNameField");
            TextField brandField = (TextField) dialogContent.lookup("#brandField");
            TextField modelField = (TextField) dialogContent.lookup("#modelField");
            TextField sizeField = (TextField) dialogContent.lookup("#sizeField");
            TextField priceField = (TextField) dialogContent.lookup("#priceField");
            TextField stockField = (TextField) dialogContent.lookup("#stockField");
            TextField imageUrlField = (TextField) dialogContent.lookup("#imageUrlField");
            Button saveButton = (Button) dialogContent.lookup("#saveButton");
            Button cancelButton = (Button) dialogContent.lookup("#cancelButton");

            // Precargar datos del producto
            productNameField.setText(product.getName());
            brandField.setText(product.getBrand());
            modelField.setText(product.getModel());
            sizeField.setText(product.getSize());
            priceField.setText(String.valueOf(product.getRentalPrice()));
            imageUrlField.setText(product.getImageUrl());

            // Configurar subida de imagen
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Sneaker Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            Button uploadButton = new Button("Choose Image");
            Label imageLabel = new Label();
            uploadButton.setOnAction(e -> {
                File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
                if (selectedFile != null) {
                    try {
                        String destPath = "src/main/resources/org/crmsneakers/crmsneakers/images/"
                                + selectedFile.getName();
                        Files.copy(selectedFile.toPath(), Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
                        imageLabel.setText(selectedFile.getName());
                        imageUrlField.setText("/org/crmsneakers/crmsneakers/images/" + selectedFile.getName());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            HBox imageUploadBox = new HBox(10, uploadButton, imageLabel);
            imageUploadBox.setAlignment(Pos.CENTER_LEFT);
            dialogContent.add(imageUploadBox, 1, 8);

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            saveButton.setOnAction(event -> {
                Button btn = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
                btn.fire();
            });
            cancelButton.setOnAction(event -> dialog.close());

            Node saveButtonNode = dialog.getDialogPane().lookupButton(saveButtonType);
            saveButtonNode.setDisable(true);
            brandField.textProperty().addListener((obs, oldVal, newVal) -> {
                saveButtonNode.setDisable(newVal.trim().isEmpty());
            });

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        product.setName(productNameField.getText());
                        product.setBrand(brandField.getText());
                        product.setModel(modelField.getText());
                        product.setSize(sizeField.getText());
                        product.setRentalPrice(Double.parseDouble(priceField.getText()));
                        product.setImageUrl(imageUrlField.getText());
                        // Actualizar stock si es necesario
                        return product;
                    } catch (NumberFormatException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Please check your inputs");
                        alert.setContentText("Make sure price and stock are valid numbers.");
                        alert.showAndWait();
                        return null;
                    }
                }
                return null;
            });

            Optional<Product> result = dialog.showAndWait();
            result.ifPresent(updatedProduct -> {
                databaseService.updateProduct(updatedProduct);
                refreshProductList();
            });

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Form");
            alert.setHeaderText("Could not load the edit product form");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleDeleteProduct(Product product) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Product");
        confirmAlert.setHeaderText("Delete Product");
        confirmAlert.setContentText("Are you sure you want to delete this product?");
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = databaseService.deleteProduct(product.getId());
            if (deleted) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Product Deleted");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("Product has been deleted successfully.");
                infoAlert.showAndWait();
                refreshProductList();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Deletion Error");
                errorAlert.setHeaderText("Failed to delete product");
                errorAlert.setContentText("An error occurred while deleting the product.");
                errorAlert.showAndWait();
            }
        }
    }

    private void updateTotalItemsLabel() {
        int totalItems = sneakerGrid.getChildren().size();
        totalItemsLabel.setText("Total Items: " + totalItems);
    }

    private void showRentDialog(Product product) {
        Dialog<Rental> dialog = new Dialog<>();
        dialog.setTitle("Rent Sneaker");
        dialog.setHeaderText("Rent " + product.getBrand() + " " + product.getModel());

        ButtonType rentButtonType = new ButtonType("Rent Now", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(rentButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ImageView productImage = new ImageView();
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(product.getImageUrl());
                productImage.setImage(image);
            } catch (Exception e) {
                productImage.setImage(
                        new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
            }
        } else {
            productImage.setImage(
                    new Image(getClass().getResourceAsStream("/org/crmsneakers/crmsneakers/placeholder.png")));
        }
        productImage.setFitWidth(150);
        productImage.setPreserveRatio(true);
        grid.add(new HBox(10, productImage), 0, 0, 2, 1);

        grid.add(new Label("Product:"), 0, 1);
        grid.add(new Label(product.getBrand() + " " + product.getModel()), 1, 1);

        grid.add(new Label("Size:"), 0, 2);
        grid.add(new Label(product.getSize()), 1, 2);

        grid.add(new Label("Daily Rental Price:"), 0, 3);
        grid.add(new Label("$" + String.format("%.2f", product.getRentalPrice())), 1, 3);

        double fixedDeposit = product.getRentalPrice() * 2;

        grid.add(new Label("Required Deposit:"), 0, 4);
        grid.add(new Label("$" + String.format("%.2f", fixedDeposit)), 1, 4);

        grid.add(new Label("Start Date:"), 0, 5);
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        grid.add(startDatePicker, 1, 5);

        grid.add(new Label("End Date:"), 0, 6);
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        grid.add(endDatePicker, 1, 6);

        grid.add(new Label("Notes:"), 0, 7);
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        grid.add(notesArea, 1, 7);

        grid.add(new Label("Total Rental Price:"), 0, 8);
        Label totalPriceLabel = new Label("$"
                + String.format("%.2f", calculateTotalPrice(product, LocalDate.now(), LocalDate.now().plusDays(7))));
        grid.add(totalPriceLabel, 1, 8);

        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            LocalDate endDate = endDatePicker.getValue();
            if (endDate != null && !newValue.isAfter(endDate)) {
                totalPriceLabel.setText("$" + String.format("%.2f", calculateTotalPrice(product, newValue, endDate)));
            }
        });

        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            LocalDate startDate = startDatePicker.getValue();
            if (startDate != null && !startDate.isAfter(newValue)) {
                totalPriceLabel.setText("$" + String.format("%.2f", calculateTotalPrice(product, startDate, newValue)));
            }
        });

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == rentButtonType) {
                try {
                    LocalDate startDate = startDatePicker.getValue();
                    LocalDate endDate = endDatePicker.getValue();

                    if (startDate == null || endDate == null) {
                        showAlert("Please select valid dates for the rental.");
                        return null;
                    }

                    if (startDate.isAfter(endDate)) {
                        showAlert("Start date cannot be after end date.");
                        return null;
                    }

                    if (startDate.isBefore(LocalDate.now())) {
                        showAlert("Start date cannot be in the past.");
                        return null;
                    }

                    Rental rental = new Rental();
                    rental.setProductId(product.getId());

                    User currentUser = UserSession.getInstance().getCurrentUser();
                    Customer customer = findCustomerByUserId(currentUser.getId());

                    if (customer != null) {
                        rental.setCustomerId(customer.getId());
                    } else {
                        showAlert("Your customer profile was not found. Please contact support.");
                        return null;
                    }

                    rental.setStartDate(startDate);
                    rental.setEndDate(endDate);
                    rental.setRentalPrice(calculateTotalPrice(product, startDate, endDate));
                    rental.setDepositAmount(fixedDeposit);
                    rental.setStatus(Rental.RentalStatus.ACTIVE);
                    rental.setNotes(notesArea.getText());

                    return rental;
                } catch (Exception e) {
                    showAlert("An error occurred: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        });

        Optional<Rental> result = dialog.showAndWait();
        result.ifPresent(rental -> {
            ObjectId rentalId = databaseService.insertRental(rental);
            if (rentalId != null) {
                databaseService.updateProductStatus(product.getId(), Product.ProductStatus.RENTED);
                refreshProductList();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Rental Confirmed");
                alert.setHeaderText("Your rental has been confirmed!");
                alert.setContentText("You have successfully rented " + product.getBrand() + " " + product.getModel() +
                        " from " + rental.getStartDate() + " to " + rental.getEndDate() +
                        ".\nTotal cost: $" + String.format("%.2f", rental.getRentalPrice()) +
                        "\nDeposit: $" + String.format("%.2f", rental.getDepositAmount()) +
                        "\nThe deposit will be refunded upon return of the sneakers in good condition.");
                alert.showAndWait();
            } else {
                showAlert("Failed to create rental. Please try again.");
            }
        });
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

    private double calculateTotalPrice(Product product, LocalDate startDate, LocalDate endDate) {
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        return product.getRentalPrice() * days;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Rental Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
