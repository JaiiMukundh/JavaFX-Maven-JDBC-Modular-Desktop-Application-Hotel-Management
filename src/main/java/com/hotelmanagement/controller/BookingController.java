package com.hotelmanagement.controller;

import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.BillDAO;
import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.model.Bill;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private TableView<Booking> bookingTableView;
    @FXML
    private Button checkoutBtn;

    private BookingDAO bookingDAO;
    private CustomerDAO customerDAO;
    private RoomDAO roomDAO;
    private BillDAO billDAO;

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
        billDAO = new BillDAO();

        // Setup booking table columns
        TableColumn<Booking, Integer> bookingIdCol = (TableColumn<Booking, Integer>) bookingTableView.getColumns().get(0);
        bookingIdCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        
        TableColumn<Booking, Integer> custIdCol = (TableColumn<Booking, Integer>) bookingTableView.getColumns().get(1);
        custIdCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        
        TableColumn<Booking, Integer> roomIdCol = (TableColumn<Booking, Integer>) bookingTableView.getColumns().get(2);
        roomIdCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        
        TableColumn<Booking, LocalDate> checkInCol = (TableColumn<Booking, LocalDate>) bookingTableView.getColumns().get(3);
        checkInCol.setCellValueFactory(new PropertyValueFactory<>("checkInDate"));
        
        TableColumn<Booking, LocalDate> checkOutCol = (TableColumn<Booking, LocalDate>) bookingTableView.getColumns().get(4);
        checkOutCol.setCellValueFactory(new PropertyValueFactory<>("checkOutDate"));
        
        TableColumn<Booking, String> statusCol = (TableColumn<Booking, String>) bookingTableView.getColumns().get(5);
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookRoomBtn.setOnAction(e -> handleBookRoom());
        checkoutBtn.setOnAction(e -> handleCheckout());

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
            ControllerRegistry.refreshBilling();
            updateAvailableRooms();
            refreshBookingTable();
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to book room: " + e.getMessage());
        }
    }

    private void handleCheckout() {
        Booking selectedBooking = bookingTableView.getSelectionModel().getSelectedItem();
        if (selectedBooking == null) {
            AlertUtils.showWarning("No Selection", "Please select a booking to checkout!");
            return;
        }

        if (AlertUtils.showConfirmation("Confirm Checkout", "Are you sure you want to checkout this booking?")) {
            try {
                LocalDate checkInDate = selectedBooking.getCheckInDate();
                LocalDate checkOutDate = selectedBooking.getCheckOutDate();
                long daysStayed = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

                Room room = roomDAO.getById(selectedBooking.getRoomId());
                if (room == null) {
                    AlertUtils.showError("Error", "Room not found!");
                    return;
                }

                double totalAmount = room.getPricePerDay() * daysStayed;

                Bill bill = new Bill(selectedBooking.getBookingId(), room.getPricePerDay(), (int) daysStayed);
                bill.setTotalAmount(totalAmount);
                billDAO.create(bill);

                bookingDAO.updateStatus(selectedBooking.getBookingId(), "CHECKED_OUT");

                roomDAO.updateAvailability(room.getRoomId(), true);

                String billMessage = String.format("Checkout Successful!\n\n" +
                        "Room Price: Rs. %.2f\n" +
                        "Days Stayed: %d\n" +
                        "Total Amount: Rs. %.2f",
                        room.getPricePerDay(), daysStayed, totalAmount);

                AlertUtils.showInfo("Checkout Successful", billMessage);
                ControllerRegistry.refreshBilling();
                updateAvailableRooms();
                refreshBookingTable();
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to checkout: " + e.getMessage());
            }
        }
    }

    private void refreshBookingTable() {
        try {
            List<Booking> bookings = bookingDAO.getActiveBookings();
            bookingTableView.setItems(FXCollections.observableArrayList(bookings));
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load bookings: " + e.getMessage());
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

    private String formatSelection(int id, String label) {
        return id + " - " + label;
    }

    private int extractId(String selection) {
        return Integer.parseInt(selection.split(" - ", 2)[0]);
    }
}
