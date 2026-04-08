package com.hotelmanagement.controller;

import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class RoomController {

    @FXML
    private TextField roomNumberField;
    @FXML
    private ComboBox<String> roomTypeCombo;
    @FXML
    private TextField priceField;
    @FXML
    private Button addRoomBtn;
    @FXML
    private Button filterAvailableBtn;
    @FXML
    private Button showAllRoomsBtn;
    @FXML
    private Button deleteRoomBtn;
    @FXML
    private TableView<Room> roomTableView;

    private RoomDAO roomDAO;

    @FXML
    @SuppressWarnings("unchecked")
    public void initialize() {
        ControllerRegistry.setRoomController(this);
        roomDAO = new RoomDAO();
        
        // Setup table columns with PropertyValueFactory
        TableColumn<Room, Integer> roomIdCol = (TableColumn<Room, Integer>) roomTableView.getColumns().get(0);
        roomIdCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        
        TableColumn<Room, String> roomNumCol = (TableColumn<Room, String>) roomTableView.getColumns().get(1);
        roomNumCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        
        TableColumn<Room, String> typeCol = (TableColumn<Room, String>) roomTableView.getColumns().get(2);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        
        TableColumn<Room, Double> priceCol = (TableColumn<Room, Double>) roomTableView.getColumns().get(3);
        priceCol.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        
        TableColumn<Room, Boolean> availCol = (TableColumn<Room, Boolean>) roomTableView.getColumns().get(4);
        availCol.setCellValueFactory(new PropertyValueFactory<>("available"));
        
        // Populate room type combo box
        roomTypeCombo.setItems(FXCollections.observableArrayList("Single", "Double", "Deluxe"));
        
        addRoomBtn.setOnAction(e -> handleAddRoom());
        filterAvailableBtn.setOnAction(e -> handleFilterAvailable());
        showAllRoomsBtn.setOnAction(e -> handleShowAll());
        deleteRoomBtn.setOnAction(e -> handleDeleteRoom());
        
        refreshRoomTable();
    }

    private void handleAddRoom() {
        String roomNumber = roomNumberField.getText().trim();
        String roomType = roomTypeCombo.getValue();
        String priceText = priceField.getText().trim();

        if (roomNumber.isEmpty() || roomType == null || priceText.isEmpty()) {
            AlertUtils.showError("Validation Error", "All fields are required!");
            return;
        }

        try {
            double price = Double.parseDouble(priceText);

            if (price < 0) {
                AlertUtils.showError("Validation Error", "Room price cannot be negative!");
                return;
            }
            
            // Check for duplicate room number
            if (roomDAO.getByRoomNumber(roomNumber) != null) {
                AlertUtils.showError("Duplicate Room", "Room number already exists!");
                return;
            }

            Room room = new Room(roomNumber, roomType, price, true);
            roomDAO.create(room);
            
            AlertUtils.showInfo("Success", "Room added successfully!");
            clearRoomForm();
            refreshRoomTable();
            ControllerRegistry.refreshCustomer();
            ControllerRegistry.refreshBookingAndBilling();
        } catch (NumberFormatException e) {
            AlertUtils.showError("Invalid Input", "Price must be a valid number!");
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to add room: " + e.getMessage());
        }
    }

    private void handleFilterAvailable() {
        try {
            List<Room> availableRooms = roomDAO.getAvailableRooms();
            roomTableView.setItems(FXCollections.observableArrayList(availableRooms));
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to fetch available rooms: " + e.getMessage());
        }
    }

    private void handleShowAll() {
        refreshRoomTable();
    }

    private void handleDeleteRoom() {
        Room selectedRoom = roomTableView.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            AlertUtils.showWarning("No Selection", "Please select a room to delete!");
            return;
        }

        if (AlertUtils.showConfirmation("Confirm Deletion", "Are you sure you want to delete room " + selectedRoom.getRoomNumber() + "?\nThis will also delete all associated bookings and bills.")) {
            try {
                roomDAO.delete(selectedRoom.getRoomId());

                ControllerRegistry.refreshCustomer();
                ControllerRegistry.refreshBookingAndBilling();
                AlertUtils.showInfo("Success", "Room and associated bookings deleted successfully!");
                refreshRoomTable();
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to delete room: " + e.getMessage());
            }
        }
    }

    private void refreshRoomTable() {
        try {
            List<Room> rooms = roomDAO.getAll();
            roomTableView.setItems(FXCollections.observableArrayList(rooms));
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load rooms: " + e.getMessage());
        }
    }

    private void clearRoomForm() {
        roomNumberField.clear();
        roomTypeCombo.setValue(null);
        priceField.clear();
    }

    public void refreshTable() {
        refreshRoomTable();
    }
}
