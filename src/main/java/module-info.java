module org.crmsneakers.crmsneakers {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires org.mongodb.driver.sync.client;
    requires javafx.graphics;


    opens org.crmsneakers.crmsneakers to javafx.fxml;
    exports org.crmsneakers.crmsneakers;
    
    // Export controller package to allow FXML loader to access it
    exports org.crmsneakers.crmsneakers.controller;
    opens org.crmsneakers.crmsneakers.controller to javafx.fxml;
    
    // Export model package
    exports org.crmsneakers.crmsneakers.model;
}