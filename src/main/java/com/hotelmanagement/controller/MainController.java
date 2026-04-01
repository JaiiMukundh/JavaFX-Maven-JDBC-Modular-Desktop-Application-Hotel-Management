package com.hotelmanagement.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class MainController {
    
    @FXML
    private BorderPane mainBorderPane;
    
    @FXML
    private TabPane mainTabPane;
    
    @FXML
    private ComboBox<String> themeCombo;

    @FXML
    private Tab roomTab;
    
    @FXML
    private Tab customerTab;
    
    @FXML
    private Tab bookingTab;
    
    @FXML
    private Tab billingTab;

    @FXML
    public void initialize() {
        System.out.println("Main Controller initialized");
        themeCombo.setItems(FXCollections.observableArrayList("Light", "Dark"));
        themeCombo.setValue("Light");
        applyTheme("light-theme");
        themeCombo.setOnAction(event ->
                applyTheme("Dark".equals(themeCombo.getValue()) ? "dark-theme" : "light-theme"));
        
        // Add tab selection listeners to refresh data when switching tabs
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == bookingTab) {
                // Refresh booking controller when switching to Bookings tab
                BookingController bookingController = ControllerRegistry.getBookingController();
                if (bookingController != null) {
                    bookingController.refreshTable();
                }
            } else if (newTab == billingTab) {
                // Refresh billing controller when switching to Billing tab
                BillingController billingController = ControllerRegistry.getBillingController();
                if (billingController != null) {
                    billingController.refreshTable();
                }
            }
        });
    }

    private void applyTheme(String themeClass) {
        mainBorderPane.getStyleClass().removeAll("light-theme", "dark-theme");
        mainBorderPane.getStyleClass().add(themeClass);
    }
}
