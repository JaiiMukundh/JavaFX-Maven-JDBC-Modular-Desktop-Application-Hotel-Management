package com.hotelmanagement.controller;

import com.hotelmanagement.dao.BillDAO;
import com.hotelmanagement.dao.BookingDAO;
import com.hotelmanagement.dao.CustomerDAO;
import com.hotelmanagement.dao.RoomDAO;
import com.hotelmanagement.model.Bill;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.AlertUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class InvoiceController {
    private static final int LONG_STAY_DISCOUNT_THRESHOLD_DAYS = 7;
    private static final double LONG_STAY_DISCOUNT_RATE = 0.10;

    @FXML
    private Label invoiceNumberLabel;
    @FXML
    private Label generatedAtLabel;
    @FXML
    private Label bookingIdLabel;
    @FXML
    private Label customerNameLabel;
    @FXML
    private Label contactNumberLabel;
    @FXML
    private Label roomNumberLabel;
    @FXML
    private Label roomTypeLabel;
    @FXML
    private Label checkInLabel;
    @FXML
    private Label checkOutLabel;
    @FXML
    private Label nightsLabel;
    @FXML
    private Label roomRateLabel;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button confirmCheckoutBtn;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BillDAO billDAO = new BillDAO();

    private Stage stage;
    private int bookingId;
    private Booking booking;
    private Customer customer;
    private Room room;
    private long nights;
    private double subtotalAmount;
    private double discountAmount;
    private double totalAmount;

    @FXML
    public void initialize() {
        confirmCheckoutBtn.setOnAction(e -> handleConfirmCheckout());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void loadBooking(int bookingId) {
        this.bookingId = bookingId;
        try {
            booking = bookingDAO.getById(bookingId);
            if (booking == null) {
                statusLabel.setText("Booking not found.");
                confirmCheckoutBtn.setDisable(true);
                return;
            }

            customer = customerDAO.getById(booking.getCustomerId());
            room = roomDAO.getById(booking.getRoomId());
            if (customer == null || room == null) {
                statusLabel.setText("Missing customer or room details.");
                confirmCheckoutBtn.setDisable(true);
                return;
            }

            nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            if (nights <= 0) {
                statusLabel.setText("Invalid booking dates for billing.");
                confirmCheckoutBtn.setDisable(true);
                return;
            }

            subtotalAmount = room.getPricePerDay() * nights;
            discountAmount = calculateDiscount(subtotalAmount, nights);
            totalAmount = subtotalAmount - discountAmount;
            renderInvoice();
        } catch (SQLException e) {
            statusLabel.setText("Failed to load invoice data.");
            confirmCheckoutBtn.setDisable(true);
            AlertUtils.showError("Database Error", "Failed to load invoice: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void renderInvoice() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String invoiceNo = "INV-" + String.format("%05d", bookingId);

        invoiceNumberLabel.setText(invoiceNo);
        generatedAtLabel.setText(LocalDate.now().format(formatter));
        bookingIdLabel.setText(String.valueOf(bookingId));
        customerNameLabel.setText(customer.getName());
        contactNumberLabel.setText(customer.getContactNumber());
        roomNumberLabel.setText(room.getRoomNumber());
        roomTypeLabel.setText(room.getRoomType());
        checkInLabel.setText(booking.getCheckInDate().format(formatter));
        checkOutLabel.setText(booking.getCheckOutDate().format(formatter));
        nightsLabel.setText(String.valueOf(nights));
        roomRateLabel.setText(String.format("Rs. %.2f", room.getPricePerDay()));
        subtotalLabel.setText(String.format("Rs. %.2f", subtotalAmount));
        discountLabel.setText(String.format("Rs. %.2f", discountAmount));
        totalLabel.setText(String.format("Rs. %.2f", totalAmount));
        statusLabel.setText(discountAmount > 0
                ? String.format("Long-stay discount applied at %.0f%%.", LONG_STAY_DISCOUNT_RATE * 100)
                : "Ready to confirm checkout.");
        confirmCheckoutBtn.setDisable(false);
    }

    private void handleConfirmCheckout() {
        if (booking == null || room == null) {
            AlertUtils.showError("Error", "Invoice data is not ready.");
            return;
        }

        if (!AlertUtils.showConfirmation("Confirm Checkout", "Generate the bill and complete checkout now?")) {
            return;
        }

        try {
            Bill existingBill = billDAO.getByBookingId(booking.getBookingId());
            if (existingBill != null) {
                AlertUtils.showWarning("Already Billed", "A bill already exists for this booking.");
                if (stage != null) {
                    stage.close();
                }
                return;
            }

            Bill bill = new Bill(booking.getBookingId(), room.getPricePerDay(), (int) nights);
            bill.setDiscountAmount(discountAmount);
            bill.setTotalAmount(totalAmount);
            billDAO.create(bill);

            bookingDAO.updateStatus(booking.getBookingId(), "CHECKED_OUT");
            roomDAO.updateAvailability(room.getRoomId(), true);

            ControllerRegistry.refreshRoom();
            ControllerRegistry.refreshBilling();
            ControllerRegistry.refreshBooking();

            AlertUtils.showInfo(
                    "Checkout Successful",
                    String.format("Checkout completed for %s.\nBill: %s\nDiscount: Rs. %.2f\nTotal: Rs. %.2f",
                            customer.getName(), invoiceNumberLabel.getText(), discountAmount, totalAmount)
            );

            if (stage != null) {
                stage.close();
            }
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to complete checkout: " + e.getMessage());
        }
    }

    private double calculateDiscount(double subtotal, long nightsStayed) {
        return nightsStayed > LONG_STAY_DISCOUNT_THRESHOLD_DAYS ? subtotal * LONG_STAY_DISCOUNT_RATE : 0.0;
    }
}
