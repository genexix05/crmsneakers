package org.crmsneakers.crmsneakers.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import org.bson.types.ObjectId;
import org.crmsneakers.crmsneakers.db.DatabaseService;
import org.crmsneakers.crmsneakers.model.Customer;
import org.crmsneakers.crmsneakers.model.Product;
import org.crmsneakers.crmsneakers.model.Rental;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class RentalsController {
    @FXML
    private TableView<Rental> rentalsTable;

    @FXML
    private TableColumn<Rental, String> customerColumn;

    @FXML
    private TableColumn<Rental, String> sneakerColumn;

    @FXML
    private TableColumn<Rental, String> startDateColumn;

    @FXML
    private TableColumn<Rental, String> endDateColumn;

    @FXML
    private TableColumn<Rental, String> priceColumn;

    @FXML
    private TableColumn<Rental, String> depositColumn;

    @FXML
    private TableColumn<Rental, String> statusColumn;

    @FXML
    private TableColumn<Rental, Rental> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    private DatabaseService databaseService;
    private ObservableList<Rental> rentalsList;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        databaseService = DatabaseService.getInstance();
        rentalsList = FXCollections.observableArrayList();

        setupTableColumns();
        setupFilters();
        setupTableRowDoubleClick();
        loadRentals();
    }

    /**
     * Configuración del evento de doble clic en las filas de la tabla
     */
    private void setupTableRowDoubleClick() {
        rentalsTable.setRowFactory(tv -> {
            TableRow<Rental> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Rental rental = row.getItem();
                    handleRentalAction(rental);
                }
            });
            return row;
        });
    }

    private void setupTableColumns() {
        customerColumn.setCellValueFactory(data -> {
            Customer customer = databaseService.findCustomerById(data.getValue().getCustomerId());
            return new SimpleStringProperty(
                    customer != null ? customer.getFirstName() + " " + customer.getLastName() : "Unknown");
        });

        sneakerColumn.setCellValueFactory(data -> {
            Product product = databaseService.findProductById(data.getValue().getProductId());
            return new SimpleStringProperty(
                    product != null ? product.getBrand() + " " + product.getModel() : "Unknown");
        });

        startDateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStartDate().format(dateFormatter)));

        endDateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getEndDate().format(dateFormatter)));

        priceColumn.setCellValueFactory(
                data -> new SimpleStringProperty("$" + String.format("%.2f", data.getValue().getRentalPrice())));

        depositColumn.setCellValueFactory(
                data -> new SimpleStringProperty("$" + String.format("%.2f", data.getValue().getDepositAmount())));

        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));

        // Configuración de la columna de acciones con botones directos
        actionsColumn.setCellFactory(_ -> new TableCell<>() {
            private final Button actionButton = new Button("Actions");

            {
                // Estilo y configuración del botón
                actionButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white;");
                actionButton.setMaxWidth(Double.MAX_VALUE);
            }

            @Override
            protected void updateItem(Rental rental, boolean empty) {
                super.updateItem(rental, empty);

                if (empty || rental == null) {
                    setGraphic(null);
                    return;
                }

                // Configurar el evento del botón según el estado del alquiler
                actionButton.setOnAction(e -> handleRentalAction(rental));
                setGraphic(actionButton);
            }
        });
    }

    // Método para manejar acciones de alquiler según su estado
    private void handleRentalAction(Rental rental) {
        switch (rental.getStatus()) {
            case ACTIVE:
                showRentalActionDialog(rental);
                break;
            case UNDER_REPAIR:
                showCompleteRepairDialog(rental);
                break;
            case DAMAGED:
                showReturnToStockDialog(rental);
                break;
            default:
                // No hay acciones disponibles para otros estados
                break;
        }
    }

    // Diálogo para mostrar opciones para un alquiler activo
    private void showRentalActionDialog(Rental rental) {
        // Obtener datos del producto
        Product product = databaseService.findProductById(rental.getProductId());

        // Crear diálogo
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Rental Actions");
        dialog.setHeaderText("Manage Rental for " + product.getBrand() + " " + product.getModel());

        // Contenido del diálogo
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Añadir información del producto
        Label brandModelLabel = new Label("Brand/Model:");
        Label brandModelValue = new Label(product.getBrand() + " " + product.getModel());

        Label sizeLabel = new Label("Size:");
        Label sizeValue = new Label(product.getSize());

        Label startDateLabel = new Label("Start Date:");
        Label startDateValue = new Label(rental.getStartDate().format(dateFormatter));

        Label endDateLabel = new Label("End Date:");
        Label endDateValue = new Label(rental.getEndDate().format(dateFormatter));

        Label notesLabel = new Label("Notes:");
        TextArea notesArea = new TextArea();
        notesArea.setPrefRowCount(3);
        notesArea.setPrefColumnCount(20);

        grid.add(brandModelLabel, 0, 0);
        grid.add(brandModelValue, 1, 0);
        grid.add(sizeLabel, 0, 1);
        grid.add(sizeValue, 1, 1);
        grid.add(startDateLabel, 0, 2);
        grid.add(startDateValue, 1, 2);
        grid.add(endDateLabel, 0, 3);
        grid.add(endDateValue, 1, 3);
        grid.add(notesLabel, 0, 4);
        grid.add(notesArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Botones de acción
        ButtonType returnInGoodCondition = new ButtonType("Return in Good Condition", ButtonBar.ButtonData.OK_DONE);
        ButtonType returnForRepair = new ButtonType("Send to Repair", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(returnInGoodCondition, returnForRepair, cancelButton);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent()) {
            String notes = notesArea.getText();

            if (result.get() == returnInGoodCondition) {
                // Finalizar alquiler en buenas condiciones
                databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                        "Returned in good condition. " + notes);
                databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);
                showSuccessAlert("Rental completed successfully. Sneaker is available for rent.");
            } else if (result.get() == returnForRepair) {
                // Enviar a reparación y retener la fianza
                String depositInfo = "Deposit of $" + String.format("%.2f", rental.getDepositAmount()) + " retained.";
                String combinedNotes = notes.isEmpty() ? depositInfo : notes + " " + depositInfo;
                databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.UNDER_REPAIR, combinedNotes);
                databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.UNDER_REPAIR);
                showSuccessAlert("Sneaker sent to repair. " + depositInfo);
            }

            // Recargar la lista de alquileres
            loadRentals();
        }
    }

    // Diálogo para completar reparación
    private void showCompleteRepairDialog(Rental rental) {
        Product product = databaseService.findProductById(rental.getProductId());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Complete Repair");
        alert.setHeaderText("Complete repair for " + product.getBrand() + " " + product.getModel());
        alert.setContentText("Is the repair completed and the sneaker ready to return to stock?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                    "Repair completed and returned to stock");
            databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);
            showSuccessAlert("Repair completed. Sneaker is back in stock.");
            loadRentals();
        }
    }

    // Diálogo para devolver al stock después de daño
    private void showReturnToStockDialog(Rental rental) {
        Product product = databaseService.findProductById(rental.getProductId());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Return to Stock");
        alert.setHeaderText("Return " + product.getBrand() + " " + product.getModel() + " to stock");
        alert.setContentText("Are you sure you want to return this sneaker to stock?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                    "Returned to stock after damage");
            databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);
            showSuccessAlert("Sneaker returned to stock successfully.");
            loadRentals();
        }
    }

    private void setupFilters() {
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.setOnAction(_ -> filterRentals());
        searchField.textProperty().addListener((_, __, ___) -> filterRentals());
    }

    private void loadRentals() {
        List<Rental> rentals = databaseService.findAllRentals();
        rentalsList.setAll(rentals);
        rentalsTable.setItems(rentalsList);
    }

    private void filterRentals() {
        String searchText = searchField.getText().toLowerCase();
        String statusValue = statusFilter.getValue();

        List<Rental> filteredList = rentalsList.stream()
                .filter(rental -> {
                    if (!"All".equals(statusValue) &&
                            !rental.getStatus().toString().equals(statusValue)) {
                        return false;
                    }

                    if (!searchText.isEmpty()) {
                        Customer customer = databaseService.findCustomerById(rental.getCustomerId());
                        Product product = databaseService.findProductById(rental.getProductId());
                        String customerName = customer != null
                                ? (customer.getFirstName() + " " + customer.getLastName()).toLowerCase()
                                : "";
                        String productName = product != null
                                ? (product.getBrand() + " " + product.getModel()).toLowerCase()
                                : "";

                        return customerName.contains(searchText) ||
                                productName.contains(searchText);
                    }

                    return true;
                })
                .collect(Collectors.toList());

        rentalsTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    private void handleComplete(Rental rental) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Complete Rental");
        alert.setHeaderText("Complete Rental and Return Deposit");
        alert.setContentText("Are the sneakers in good condition?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Actualizar el estado del alquiler
            databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                    "Returned in good condition");

            // Actualizar el estado del producto
            databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);

            // Recargar la lista
            loadRentals();
        }
    }

    private void handleRepair(Rental rental) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Send to Repair");
        dialog.setHeaderText("Send Sneakers to Repair");
        dialog.setContentText("Please enter the damage description:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String notes = result.get();

            // Actualizar el estado del alquiler
            databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.UNDER_REPAIR, notes);

            // Actualizar el estado del producto
            databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.UNDER_REPAIR);

            // Recargar la lista
            loadRentals();
        }
    }

    private void handleReturn(Rental rental) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Choose the condition of the returned sneakers:",
                ButtonType.CANCEL);

        ButtonType goodCondition = new ButtonType("Good Condition");
        ButtonType needsRepair = new ButtonType("Needs Repair");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().addAll(goodCondition, needsRepair, cancel);
        alert.setTitle("Return Sneakers");
        alert.setHeaderText("Return Sneakers to Stock");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == goodCondition) {
                // Actualizar el estado del alquiler a completado
                databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                        "Returned in good condition");
                // Actualizar el estado del producto a activo
                databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);

                showSuccessAlert("Sneakers returned successfully and available for rent.");
            } else if (result.get() == needsRepair) {
                // Mostrar diálogo para ingresar notas de reparación
                TextInputDialog repairDialog = new TextInputDialog();
                repairDialog.setTitle("Repair Notes");
                repairDialog.setHeaderText("Enter repair details");
                repairDialog.setContentText("Please describe the issues that need repair:");

                Optional<String> repairNotes = repairDialog.showAndWait();
                if (repairNotes.isPresent()) {
                    // Actualizar el estado del alquiler a dañado
                    databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.DAMAGED, repairNotes.get());
                    // Actualizar el estado del producto a en reparación
                    databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.UNDER_REPAIR);

                    showSuccessAlert("Sneakers sent to repair.");
                }
            }
            // Recargar la lista de alquileres
            loadRentals();
        }
    }

    private void handleCompleteRepair(Rental rental) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Complete Repair");
        alert.setHeaderText("Complete Repair and Return to Stock");
        alert.setContentText("Has the repair been completed successfully?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Actualizar el estado del alquiler
            databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                    "Repair completed and returned to stock");

            // Actualizar el estado del producto
            databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);

            showSuccessAlert("Sneakers repaired and returned to stock successfully.");
            loadRentals();
        }
    }

    private void handleReturnToStock(Rental rental) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Return to Stock");
        alert.setHeaderText("Return Damaged Sneakers to Stock");
        alert.setContentText("Are you sure you want to return these sneakers to stock?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Actualizar el estado del alquiler
            databaseService.updateRentalStatus(rental.getId(), Rental.RentalStatus.COMPLETED,
                    "Returned to stock after damage");

            // Actualizar el estado del producto
            databaseService.updateProductStatus(rental.getProductId(), Product.ProductStatus.ACTIVE);

            showSuccessAlert("Sneakers returned to stock successfully.");
            loadRentals();
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAddRental() {
        // Crear el diálogo modal
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Añadir Renta");
        dialog.setHeaderText("Crear nuevo alquiler");

        // Botones para guardar o cancelar
        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Crear un GridPane para organizar los campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Combobox para seleccionar un producto activo
        ComboBox<Product> productComboBox = new ComboBox<>();
        productComboBox.setItems(FXCollections.observableArrayList(databaseService.findAvailableProducts()));
        productComboBox.setPromptText("Selecciona un producto");

        // Combobox para seleccionar un cliente (usuario)
        ComboBox<Customer> customerComboBox = new ComboBox<>();
        customerComboBox.setItems(FXCollections.observableArrayList(databaseService.findAllCustomers()));
        customerComboBox.setPromptText("Selecciona un cliente");

        // DatePickers para seleccionar fechas de inicio y fin
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(1));

        // Campo para mostrar precio de alquiler calculado automáticamente
        TextField rentalPriceField = new TextField();
        rentalPriceField.setPromptText("Precio de alquiler");
        rentalPriceField.setEditable(false); // No editable para que se calcule solo

        TextField depositField = new TextField();
        depositField.setPromptText("Depósito");

        // Lambda para actualizar el precio de alquiler basado en la selección y fechas
        Runnable updateRentalPrice = () -> {
            Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            if (selectedProduct != null && startDate != null && endDate != null) {
                // Calcular la cantidad de días entre las fechas
                long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
                if (days <= 0) {
                    days = 1; // Evitamos días cero o negativos
                }
                double calculatedPrice = selectedProduct.getRentalPrice() * days;
                // Forzamos que el separador decimal sea punto, usando Locale.US
                rentalPriceField.setText(String.format(Locale.US, "%.2f", calculatedPrice));
            }
        };

        // Actualizar precio al seleccionar el producto o al cambiar las fechas
        productComboBox.setOnAction(e -> updateRentalPrice.run());
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateRentalPrice.run());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateRentalPrice.run());

        // Agregar los controles al grid
        grid.add(new Label("Producto:"), 0, 0);
        grid.add(productComboBox, 1, 0);
        grid.add(new Label("Cliente:"), 0, 1);
        grid.add(customerComboBox, 1, 1);
        grid.add(new Label("Fecha de inicio:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        grid.add(new Label("Fecha de fin:"), 0, 3);
        grid.add(endDatePicker, 1, 3);
        grid.add(new Label("Precio de alquiler:"), 0, 4);
        grid.add(rentalPriceField, 1, 4);
        grid.add(new Label("Depósito:"), 0, 5);
        grid.add(depositField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Mostrar el diálogo y procesar la respuesta
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            // Recuperar valores seleccionados y validar campos
            Product selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
            Customer selectedCustomer = customerComboBox.getSelectionModel().getSelectedItem();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (selectedProduct == null || selectedCustomer == null || startDate == null || endDate == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Todos los campos deben ser completados.");
                alert.showAndWait();
                return;
            }

            double rentalPrice, depositAmount;
            try {
                // Reemplazar comas por puntos para evitar errores de parseo
                String rentalPriceText = rentalPriceField.getText().trim().replace(',', '.');
                rentalPrice = Double.parseDouble(rentalPriceText);

                String depositText = depositField.getText().trim().replace(',', '.');
                if (depositText.isEmpty()) {
                    depositAmount = 0.0;
                } else {
                    depositAmount = Double.parseDouble(depositText);
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Precio de alquiler y depósito deben ser números válidos.");
                alert.showAndWait();
                return;
            }

            // Crear el nuevo alquiler
            Rental newRental = new Rental();
            newRental.setCustomerId(selectedCustomer.getId());
            newRental.setProductId(selectedProduct.getId());
            newRental.setStartDate(startDate);
            newRental.setEndDate(endDate);
            newRental.setRentalPrice(rentalPrice);
            newRental.setDepositAmount(depositAmount);
            newRental.setStatus(Rental.RentalStatus.ACTIVE);

            // Insertar el alquiler en la base de datos
            ObjectId rentalId = databaseService.insertRental(newRental);
            if (rentalId != null) {
                // Actualizar el estado del producto a RENTED (o el estado que manejes para
                // alquilado)
                databaseService.updateProductStatus(selectedProduct.getId(), Product.ProductStatus.RENTED);
                showSuccessAlert("Alquiler añadido exitosamente.");
                loadRentals();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error al insertar el alquiler en la base de datos.");
                alert.showAndWait();
            }
        }
    }

}