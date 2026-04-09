package com.hotelmanagement.controller;

import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class CustomerController {

    @FXML
    private TextField customerNameField;
    @FXML
    private TextField contactNumberField;
    @FXML
    private TextField searchCustomerField;
    @FXML
    private ComboBox<String> selectedRoomCombo;
    @FXML
    private Button addCustomerBtn;
    @FXML
    private Button deleteCustomerBtn;
    @FXML
    private Button refreshCustomerBtn;
    @FXML
    private TableView<Customer> customerTableView;

    private CustomerDAO customerDAO;
    private RoomDAO roomDAO;
    private final ObservableList<Customer> masterCustomers = FXCollections.observableArrayList();
    private FilteredList<Customer> filteredCustomers;

    @FXML
    @SuppressWarnings("unchecked")
    public void initialize() {
        ControllerRegistry.setCustomerController(this);
        customerDAO = new CustomerDAO();
        roomDAO = new RoomDAO();
        // Setup table columns with PropertyValueFactory
        TableColumn<Customer, Integer> custIdCol = (TableColumn<Customer, Integer>) customerTableView.getColumns().get(0);
        custIdCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        
        TableColumn<Customer, String> nameCol = (TableColumn<Customer, String>) customerTableView.getColumns().get(1);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        TableColumn<Customer, String> contactCol = (TableColumn<Customer, String>) customerTableView.getColumns().get(2);
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));

        if (customerTableView.getColumns().size() > 3) {
            TableColumn<Customer, String> roomCol = (TableColumn<Customer, String>) customerTableView.getColumns().get(3);
            roomCol.setCellValueFactory(new PropertyValueFactory<>("selectedRoomNumber"));
        }

        filteredCustomers = new FilteredList<>(masterCustomers, customer -> true);
        SortedList<Customer> sortedCustomers = new SortedList<>(filteredCustomers);
        sortedCustomers.comparatorProperty().bind(customerTableView.comparatorProperty());
        customerTableView.setItems(sortedCustomers);

        if (searchCustomerField != null) {
            searchCustomerField.textProperty().addListener((obs, oldValue, newValue) -> applyCustomerFilter(newValue));
        }
        
        addCustomerBtn.setOnAction(e -> handleAddCustomer());
        deleteCustomerBtn.setOnAction(e -> handleDeleteCustomer());
        refreshCustomerBtn.setOnAction(e -> refreshCustomerTable());

        if (selectedRoomCombo != null) {
            try {
                loadRoomNumbers();
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to load rooms: " + e.getMessage());
            }
        }
        refreshCustomerTable();
    }

    private void handleAddCustomer() {
        String name = customerNameField.getText().trim();
        String contact = contactNumberField.getText().trim();
        String selectedRoom = selectedRoomCombo != null ? selectedRoomCombo.getValue() : null;

        if (name.isEmpty() || contact.isEmpty()) {
            AlertUtils.showError("Validation Error", "Customer name and contact number are required!");
            return;
        }

        // Validate phone number: exactly 10 digits
        if (!contact.matches("\\d{10}")) {
            AlertUtils.showError("Validation Error", "Phone number must be exactly 10 digits!");
            return;
        }

        try {
            Customer customer = new Customer(name, contact, selectedRoom);
            customerDAO.create(customer);
            
            AlertUtils.showInfo("Success", "Customer added successfully!");
            clearCustomerForm();
            refreshCustomerTable();
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to add customer: " + e.getMessage());
        }
    }

    private void handleDeleteCustomer() {
        Customer selectedCustomer = customerTableView.getSelectionModel().getSelectedItem();
        if (selectedCustomer == null) {
            AlertUtils.showWarning("No Selection", "Please select a customer to delete!");
            return;
        }

        if (AlertUtils.showConfirmation("Confirm Deletion", "Are you sure you want to delete " + selectedCustomer.getName() + "?\nThis will also delete all associated bookings and bills.")) {
            try {
                customerDAO.delete(selectedCustomer.getCustomerId());

                ControllerRegistry.refreshBookingAndBilling();
                AlertUtils.showInfo("Success", "Customer and associated bookings deleted successfully!");
                refreshCustomerTable();
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to delete customer: " + e.getMessage());
            }
        }
    }

    private void refreshCustomerTable() {
        try {
            if (selectedRoomCombo != null) {
                loadRoomNumbers();
            }
            List<Customer> customers = customerDAO.getAll();
            masterCustomers.setAll(customers);
            applyCustomerFilter(searchCustomerField != null ? searchCustomerField.getText() : null);
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load customers: " + e.getMessage());
        }
    }

    private void clearCustomerForm() {
        customerNameField.clear();
        contactNumberField.clear();
        if (selectedRoomCombo != null) {
            selectedRoomCombo.setValue(null);
        }
    }

    public void refreshTable() {
        refreshCustomerTable();
    }

    private void applyCustomerFilter(String searchText) {
        if (filteredCustomers == null) {
            return;
        }

        String normalizedSearch = searchText == null ? "" : searchText.trim().toLowerCase();
        filteredCustomers.setPredicate(customer -> {
            if (normalizedSearch.isEmpty()) {
                return true;
            }
            String name = customer.getName();
            return name != null && name.toLowerCase().contains(normalizedSearch);
        });
    }

    private void loadRoomNumbers() throws SQLException {
        List<Room> rooms = roomDAO.getAll();
        if (selectedRoomCombo != null) {
            List<String> roomNumbers = rooms.stream().map(Room::getRoomNumber).toList();
            selectedRoomCombo.setItems(FXCollections.observableArrayList(roomNumbers));
            String currentSelection = selectedRoomCombo.getValue();
            if (currentSelection != null && !roomNumbers.contains(currentSelection)) {
                selectedRoomCombo.setValue(null);
            }
        }
    }
}
