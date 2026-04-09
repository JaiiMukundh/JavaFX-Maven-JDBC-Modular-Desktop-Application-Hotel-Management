package com.hotelmanagement.controller;

import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class BookingController {

    @FXML
    private ComboBox<String> customerCombo;
    @FXML
    private ComboBox<String> roomCombo;
    @FXML
    private DatePicker checkInDatePicker;
    @FXML
    private DatePicker checkOutDatePicker;
    @FXML
    private Button bookRoomBtn;
    @FXML
    private Button deleteBookingBtn;
    @FXML
    private TableView<Booking> bookingTableView;

    private BookingDAO bookingDAO;
    private CustomerDAO customerDAO;
    private RoomDAO roomDAO;

    private List<Customer> customers;
    private List<Room> rooms;

    @FXML
    @SuppressWarnings("unchecked")
    public void initialize() {
        // Register this controller in the registry
        ControllerRegistry.setBookingController(this);
        
        bookingDAO = new BookingDAO();
        customerDAO = new CustomerDAO();
        roomDAO = new RoomDAO();

        // Setup booking table columns
        TableColumn<Booking, Integer> bookingIdCol = (TableColumn<Booking, Integer>) bookingTableView.getColumns().get(0);
        bookingIdCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        
        TableColumn<Booking, Integer> custIdCol = (TableColumn<Booking, Integer>) bookingTableView.getColumns().get(1);
        custIdCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        
        TableColumn<Booking, String> roomNumCol = (TableColumn<Booking, String>) bookingTableView.getColumns().get(2);
        roomNumCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        
        TableColumn<Booking, LocalDate> checkInCol = (TableColumn<Booking, LocalDate>) bookingTableView.getColumns().get(3);
        checkInCol.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        
        TableColumn<Booking, LocalDate> checkOutCol = (TableColumn<Booking, LocalDate>) bookingTableView.getColumns().get(4);
        checkOutCol.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        
        TableColumn<Booking, String> statusCol = (TableColumn<Booking, String>) bookingTableView.getColumns().get(5);
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookRoomBtn.setOnAction(e -> handleBookRoom());
        deleteBookingBtn.setOnAction(e -> handleDeleteBooking());
        configureDatePickers();

        loadCustomersAndRooms();
        refreshBookingTable();
    }

    private void loadCustomersAndRooms() {
        try {
            customers = customerDAO.getAll();
            customerCombo.setItems(FXCollections.observableArrayList(
                    customers.stream()
                            .map(customer -> formatSelection(customer.getCustomerId(), customer.getName()))
                            .collect(Collectors.toList())
            ));

            updateAvailableRooms();
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load data: " + e.getMessage());
        }
    }

    private void updateAvailableRooms() {
        try {
            rooms = roomDAO.getAvailableRooms();
            roomCombo.setItems(FXCollections.observableArrayList(
                    rooms.stream()
                            .map(room -> formatSelection(
                                    room.getRoomId(),
                                    "Room " + room.getRoomNumber() + " (" + room.getRoomType() + ") - Rs. " + room.getPricePerDay()
                            ))
                            .collect(Collectors.toList())
            ));
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load rooms: " + e.getMessage());
        }
    }

    private void handleBookRoom() {
        String customerSelection = customerCombo.getValue();
        String roomSelection = roomCombo.getValue();
        LocalDate checkInDate = checkInDatePicker.getValue();
        LocalDate checkOutDate = checkOutDatePicker.getValue();

        if (customerSelection == null || roomSelection == null || checkInDate == null || checkOutDate == null) {
            AlertUtils.showError("Validation Error", "All fields are required!");
            return;
        }

        LocalDate today = LocalDate.now();
        if (checkInDate.isBefore(today) || checkOutDate.isBefore(today)) {
            AlertUtils.showError("Validation Error", "You cannot select a date before today.");
            return;
        }

        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            AlertUtils.showError("Validation Error", "Check-out date must be after check-in date!");
            return;
        }

        try {
            int customerId = extractId(customerSelection);
            int roomId = extractId(roomSelection);

            List<Booking> conflictingBookings = bookingDAO.getByRoomIdAndDateRange(roomId, checkInDate, checkOutDate);
            if (!conflictingBookings.isEmpty()) {
                AlertUtils.showError("Booking Conflict", "Room is already booked for the selected dates!");
                return;
            }

            Booking booking = new Booking(customerId, roomId, checkInDate, checkOutDate);
            bookingDAO.create(booking);

            // Update room availability
            Room room = roomDAO.getById(roomId);
            if (room != null) {
                room.setAvailable(false);
                roomDAO.update(room);
            }

            AlertUtils.showInfo("Success", "Room booked successfully!");
            clearBookingForm();
            updateAvailableRooms();
            refreshBookingTable();
            ControllerRegistry.refreshRoom();
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to book room: " + e.getMessage());
        }
    }

    private void refreshBookingTable() {
        try {
            List<Booking> bookings = bookingDAO.getAll();
            bookingTableView.setItems(FXCollections.observableArrayList(bookings));
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load bookings: " + e.getMessage());
        }
    }

    private void handleDeleteBooking() {
        Booking selectedBooking = bookingTableView.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            AlertUtils.showWarning("No Selection", "Please select a booking record to delete!");
            return;
        }

        if (!AlertUtils.showConfirmation(
                "Confirm Deletion",
                "Are you sure you want to delete booking #" + selectedBooking.getBookingId() + "?\n" +
                        "This will also remove its bill, if one exists, and mark the room as available."
        )) {
            return;
        }

        try {
            bookingDAO.delete(selectedBooking.getBookingId());

            Room room = roomDAO.getById(selectedBooking.getRoomId());
            if (room != null) {
                room.setAvailable(true);
                roomDAO.update(room);
            }

            refreshBookingTable();
            updateAvailableRooms();
            ControllerRegistry.refreshRoom();
            ControllerRegistry.refreshBilling();

            AlertUtils.showInfo("Success", "Booking record deleted successfully!");
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to delete booking: " + e.getMessage());
        }
    }

    private void clearBookingForm() {
        customerCombo.setValue(null);
        roomCombo.setValue(null);
        checkInDatePicker.setValue(null);
        checkOutDatePicker.setValue(null);
    }

    public void refreshTable() {
        loadCustomersAndRooms();
        refreshBookingTable();
    }

    private void configureDatePickers() {
        LocalDate today = LocalDate.now();
        checkInDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(today));
            }
        });

        updateCheckoutDateConstraints(today);
        checkInDatePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            updateCheckoutDateConstraints(newDate != null ? newDate : today);
            if (newDate != null && checkOutDatePicker.getValue() != null
                    && checkOutDatePicker.getValue().isBefore(newDate)) {
                checkOutDatePicker.setValue(null);
            }
        });
    }

    private void updateCheckoutDateConstraints(LocalDate minimumDate) {
        LocalDate today = LocalDate.now();
        LocalDate minCheckoutDate = minimumDate != null && minimumDate.isAfter(today) ? minimumDate : today;
        checkOutDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(minCheckoutDate));
            }
        });
    }

    private String formatSelection(int id, String label) {
        return id + " - " + label;
    }

    private int extractId(String selection) {
        return Integer.parseInt(selection.split(" - ", 2)[0]);
    }
}
