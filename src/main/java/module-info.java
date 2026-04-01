module com.hotelmanagement {
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive java.sql;

    opens com.hotelmanagement to javafx.fxml;
    opens com.hotelmanagement.controller to javafx.fxml;
    opens com.hotelmanagement.model to javafx.fxml;
    
    exports com.hotelmanagement;
    exports com.hotelmanagement.controller;
    exports com.hotelmanagement.model;
    exports com.hotelmanagement.dao;
    exports com.hotelmanagement.utils;
}
