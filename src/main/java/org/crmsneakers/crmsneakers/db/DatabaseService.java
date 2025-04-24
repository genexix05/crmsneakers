package org.crmsneakers.crmsneakers.db;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.crmsneakers.crmsneakers.model.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class DatabaseService {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "crmsneakers";
    
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    // Collections
    private MongoCollection<Document> usersCollection;
    private MongoCollection<Document> customersCollection;
    private MongoCollection<Document> productsCollection;
    private MongoCollection<Document> rentalsCollection;
    
    // Singleton instance
    private static DatabaseService instance;
    
    private DatabaseService() {
        connect();
    }
    
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    private void connect() {
        try {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
            
            // Initialize collections
            usersCollection = database.getCollection("users");
            customersCollection = database.getCollection("customers");
            productsCollection = database.getCollection("products");
            rentalsCollection = database.getCollection("rentals");
            
            System.out.println("Successfully connected to MongoDB");
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
        }
    }
    
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed");
        }
    }
    
    // User methods
    public ObjectId insertUser(User user) {
        Document doc = new Document()
                .append("username", user.getUsername())
                .append("password", user.getPassword())
                .append("role", user.getRole().toString());
        
        InsertOneResult result = usersCollection.insertOne(doc);
        if (result.wasAcknowledged() && result.getInsertedId() != null) {
            return result.getInsertedId().asObjectId().getValue();
        }
        return null;
    }
    
    public ObjectId saveUser(User user) {
        Document doc = new Document()
                .append("username", user.getUsername())
                .append("password", user.getPassword())
                .append("role", user.getRole().toString())
                .append("email", user.getEmail())
                .append("phone", user.getPhone());
        
        InsertOneResult result = usersCollection.insertOne(doc);
        if (result.wasAcknowledged() && result.getInsertedId() != null) {
            return result.getInsertedId().asObjectId().getValue();
        }
        return null;
    }
    
    public User findUserByUsername(String username) {
        Document doc = usersCollection.find(Filters.eq("username", username)).first();
        if (doc != null) {
            User user = new User();
            user.setId(doc.getObjectId("_id"));
            user.setUsername(doc.getString("username"));
            user.setPassword(doc.getString("password"));
            user.setRole(User.UserRole.valueOf(doc.getString("role")));
            return user;
        }
        return null;
    }

    public boolean deleteUser(ObjectId userId) {
        DeleteResult result = usersCollection.deleteOne(Filters.eq("_id", userId));
        return result.wasAcknowledged() && result.getDeletedCount() > 0;
    }
    
    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        usersCollection.find().forEach(doc -> {
            User user = new User();
            user.setId(doc.getObjectId("_id"));
            user.setUsername(doc.getString("username"));
            user.setPassword(doc.getString("password"));
            user.setRole(User.UserRole.valueOf(doc.getString("role")));
            user.setEmail(doc.getString("email"));
            user.setPhone(doc.getString("phone"));
            users.add(user);
        });
        return users;
    }

    public boolean updateUser(User user) {
        if (user.getId() == null) {
            return false;
        }

        Document updateDoc = new Document();
        if (user.getEmail() != null) {
            updateDoc.append("email", user.getEmail());
        }
        if (user.getPhone() != null) {
            updateDoc.append("phone", user.getPhone());
        }
        if (user.getPassword() != null) {
            updateDoc.append("password", user.getPassword());
        }
        if (user.getRole() != null) {
            updateDoc.append("role", user.getRole().toString());
        }

        UpdateResult result = usersCollection.updateOne(
            Filters.eq("_id", user.getId()),
            new Document("$set", updateDoc));

        return result.wasAcknowledged() && result.getModifiedCount() > 0;
    }
    
    // Customer methods
    public ObjectId insertCustomer(Customer customer) {
        Document doc = new Document()
                .append("firstName", customer.getFirstName())
                .append("lastName", customer.getLastName())
                .append("email", customer.getEmail())
                .append("phone", customer.getPhone())
                .append("address", customer.getAddress());
        
        if (customer.getUserId() != null) {
            doc.append("userId", customer.getUserId());
        }
        
        InsertOneResult result = customersCollection.insertOne(doc);
        if (result.wasAcknowledged() && result.getInsertedId() != null) {
            return result.getInsertedId().asObjectId().getValue();
        }
        return null;
    }
    
    public List<Customer> findAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        customersCollection.find().forEach(doc -> {
            Customer customer = new Customer();
            customer.setId(doc.getObjectId("_id"));
            customer.setFirstName(doc.getString("firstName"));
            customer.setLastName(doc.getString("lastName"));
            customer.setEmail(doc.getString("email"));
            customer.setPhone(doc.getString("phone"));
            customer.setAddress(doc.getString("address"));
            if (doc.containsKey("userId")) {
                customer.setUserId(doc.getObjectId("userId"));
            }
            customers.add(customer);
        });
        return customers;
    }
    
    public Customer findCustomerById(ObjectId id) {
        Document doc = customersCollection.find(Filters.eq("_id", id)).first();
        if (doc != null) {
            Customer customer = new Customer();
            customer.setId(doc.getObjectId("_id"));
            customer.setFirstName(doc.getString("firstName"));
            customer.setLastName(doc.getString("lastName"));
            customer.setEmail(doc.getString("email"));
            customer.setPhone(doc.getString("phone"));
            customer.setAddress(doc.getString("address"));
            if (doc.containsKey("userId")) {
                customer.setUserId(doc.getObjectId("userId"));
            }
            return customer;
        }
        return null;
    }
    
    public boolean updateCustomer(Customer customer) {
        if (customer.getId() == null) {
            return false;
        }
        
        Document doc = new Document()
                .append("firstName", customer.getFirstName())
                .append("lastName", customer.getLastName())
                .append("email", customer.getEmail())
                .append("phone", customer.getPhone())
                .append("address", customer.getAddress());
        
        if (customer.getUserId() != null) {
            doc.append("userId", customer.getUserId());
        }
        
        UpdateResult result = customersCollection.updateOne(
                Filters.eq("_id", customer.getId()),
                new Document("$set", doc));
                
        return result.wasAcknowledged() && result.getModifiedCount() > 0;
    }
    
    public boolean deleteCustomer(ObjectId id) {
        DeleteResult result = customersCollection.deleteOne(Filters.eq("_id", id));
        return result.wasAcknowledged() && result.getDeletedCount() > 0;
    }
    
    // Product methods

    public boolean saveProduct(Product product) {
        if (product.getId() == null) {
            // Si el producto es nuevo, lo insertamos y comprobamos si se insertó correctamente.
            ObjectId id = insertProduct(product);
            return id != null;
        } else {
            // Si el producto ya existe, lo actualizamos.
            return updateProduct(product);
        }
    }    

    public ObjectId insertProduct(Product product) {
        Document doc = new Document()
                .append("name", product.getName())
                .append("brand", product.getBrand())
                .append("model", product.getModel())
                .append("size", product.getSize())
                .append("rentalPrice", product.getRentalPrice())
                .append("status", product.getStatus().toString())
                .append("imageUrl", product.getImageUrl())
                .append("description", product.getDescription());
        
        InsertOneResult result = productsCollection.insertOne(doc);
        if (result.wasAcknowledged() && result.getInsertedId() != null) {
            return result.getInsertedId().asObjectId().getValue();
        }
        return null;
    }
    
    public List<Product> findAllProducts() {
        List<Product> products = new ArrayList<>();
        productsCollection.find().forEach(doc -> {
            Product product = documentToProduct(doc);
            products.add(product);
        });
        return products;
    }
    
    public List<Product> findAvailableProducts() {
        List<Product> products = new ArrayList<>();
        productsCollection.find(Filters.eq("status", Product.ProductStatus.ACTIVE.toString()))
                .forEach(doc -> {
                    Product product = documentToProduct(doc);
                    products.add(product);
                });
        return products;
    }
    
    public Product findProductById(ObjectId id) {
        Document doc = productsCollection.find(Filters.eq("_id", id)).first();
        if (doc != null) {
            return documentToProduct(doc);
        }
        return null;
    }
    
    public boolean updateProductStatus(ObjectId id, Product.ProductStatus status) {
        UpdateResult result = productsCollection.updateOne(
                Filters.eq("_id", id),
                Updates.set("status", status.toString()));
        return result.wasAcknowledged() && result.getModifiedCount() > 0;
    }
    
    // MÉTODOS NUEVOS:
    
    // 1. Filtrar productos según marca, talla y estado
    public List<Product> findProducts(String brand, String size, Product.ProductStatus status) {
        List<Bson> filters = new ArrayList<>();
        if (brand != null && !brand.isEmpty()) {
            filters.add(Filters.eq("brand", brand));
        }
        if (size != null && !size.isEmpty()) {
            filters.add(Filters.eq("size", size));
        }
        if (status != null) {
            filters.add(Filters.eq("status", status.toString()));
        }
        Bson finalFilter;
        if (filters.isEmpty()) {
            finalFilter = new Document(); // Sin filtro, devolver todos
        } else if (filters.size() == 1) {
            finalFilter = filters.get(0);
        } else {
            finalFilter = Filters.and(filters);
        }
        List<Product> products = new ArrayList<>();
        productsCollection.find(finalFilter).forEach(doc -> {
            Product product = documentToProduct(doc);
            products.add(product);
        });
        return products;
    }
    
    // 2. Buscar productos por una consulta de texto (nombre, marca o modelo)
    public List<Product> searchProducts(String query) {
        String regex = ".*" + query + ".*";
        Bson filter = Filters.or(
            Filters.regex("name", regex, "i"),
            Filters.regex("brand", regex, "i"),
            Filters.regex("model", regex, "i")
        );
        List<Product> products = new ArrayList<>();
        productsCollection.find(filter).forEach(doc -> {
            Product product = documentToProduct(doc);
            products.add(product);
        });
        return products;
    }
    
    // 3. Actualizar un producto (actualiza todos sus campos)
    public boolean updateProduct(Product product) {
        if (product.getId() == null) {
            return false;
        }
        Document doc = new Document()
                .append("name", product.getName())
                .append("brand", product.getBrand())
                .append("model", product.getModel())
                .append("size", product.getSize())
                .append("rentalPrice", product.getRentalPrice())
                .append("status", product.getStatus().toString())
                .append("imageUrl", product.getImageUrl())
                .append("description", product.getDescription());
        
        UpdateResult result = productsCollection.updateOne(
                    Filters.eq("_id", product.getId()),
                    new Document("$set", doc));
        return result.wasAcknowledged() && result.getModifiedCount() > 0;
    }
    
    // 4. Eliminar un producto por su ID
    public boolean deleteProduct(ObjectId id) {
        DeleteResult result = productsCollection.deleteOne(Filters.eq("_id", id));
        return result.wasAcknowledged() && result.getDeletedCount() > 0;
    }
    
    private Product documentToProduct(Document doc) {
        Product product = new Product();
        product.setId(doc.getObjectId("_id"));
        product.setName(doc.getString("name"));
        product.setBrand(doc.getString("brand"));
        product.setModel(doc.getString("model"));
        product.setSize(doc.getString("size"));
        product.setRentalPrice(doc.getDouble("rentalPrice"));
        product.setStatus(Product.ProductStatus.valueOf(doc.getString("status")));
        product.setImageUrl(doc.getString("imageUrl"));
        product.setDescription(doc.getString("description"));
        return product;
    }
    
    // Rental methods
    public ObjectId insertRental(Rental rental) {
        Document doc = new Document()
                .append("customerId", rental.getCustomerId())
                .append("productId", rental.getProductId())
                .append("startDate", rental.getStartDate().toString())
                .append("endDate", rental.getEndDate().toString())
                .append("rentalPrice", rental.getRentalPrice())
                .append("depositAmount", rental.getDepositAmount())
                .append("status", rental.getStatus().toString());
        
        if (rental.getNotes() != null) {
            doc.append("notes", rental.getNotes());
        }
        
        InsertOneResult result = rentalsCollection.insertOne(doc);
        if (result.wasAcknowledged() && result.getInsertedId() != null) {
            return result.getInsertedId().asObjectId().getValue();
        }
        return null;
    }
    
    public List<Rental> findActiveRentals() {
        List<Rental> rentals = new ArrayList<>();
        rentalsCollection.find(Filters.eq("status", Rental.RentalStatus.ACTIVE.toString()))
                .forEach(doc -> {
                    Rental rental = documentToRental(doc);
                    rentals.add(rental);
                });
        return rentals;
    }
    
    public List<Rental> findRentalsByCustomerId(ObjectId customerId) {
        List<Rental> rentals = new ArrayList<>();
        rentalsCollection.find(Filters.eq("customerId", customerId))
                .forEach(doc -> {
                    Rental rental = documentToRental(doc);
                    rentals.add(rental);
                });
        return rentals;
    }
    
    public List<Rental> findAllRentals() {
        List<Rental> rentals = new ArrayList<>();
        rentalsCollection.find().forEach(doc -> {
            Rental rental = documentToRental(doc);
            rentals.add(rental);
        });
        return rentals;
    }
    
    public boolean updateRentalStatus(ObjectId id, Rental.RentalStatus status, String notes) {
        Bson updates = Updates.combine(
                Updates.set("status", status.toString()),
                Updates.set("notes", notes));
        
        UpdateResult result = rentalsCollection.updateOne(
                Filters.eq("_id", id),
                updates);
        return result.wasAcknowledged() && result.getModifiedCount() > 0;
    }
    
    private Rental documentToRental(Document doc) {
        Rental rental = new Rental();
        rental.setId(doc.getObjectId("_id"));
        rental.setCustomerId(doc.getObjectId("customerId"));
        rental.setProductId(doc.getObjectId("productId"));
        rental.setStartDate(LocalDate.parse(doc.getString("startDate")));
        rental.setEndDate(LocalDate.parse(doc.getString("endDate")));
        rental.setRentalPrice(doc.getDouble("rentalPrice"));
        rental.setDepositAmount(doc.getDouble("depositAmount"));
        rental.setStatus(Rental.RentalStatus.valueOf(doc.getString("status")));
        if (doc.containsKey("notes")) {
            rental.setNotes(doc.getString("notes"));
        }
        return rental;
    }
}
