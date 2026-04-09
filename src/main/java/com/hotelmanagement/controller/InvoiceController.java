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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class InvoiceController {
    private static final int LONG_STAY_DISCOUNT_THRESHOLD_DAYS = 7;
    private static final double LONG_STAY_DISCOUNT_RATE = 0.10;
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

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
        String invoiceNo = "INV-" + String.format("%05d", bookingId);

        invoiceNumberLabel.setText(invoiceNo);
        generatedAtLabel.setText(LocalDate.now().format(DISPLAY_DATE_FORMATTER));
        bookingIdLabel.setText(String.valueOf(bookingId));
        customerNameLabel.setText(customer.getName());
        contactNumberLabel.setText(customer.getContactNumber());
        roomNumberLabel.setText(room.getRoomNumber());
        roomTypeLabel.setText(room.getRoomType());
        checkInLabel.setText(booking.getCheckInDate().format(DISPLAY_DATE_FORMATTER));
        checkOutLabel.setText(booking.getCheckOutDate().format(DISPLAY_DATE_FORMATTER));
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
            String invoiceNo = invoiceNumberLabel.getText();
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

            try {
                saveInvoiceToFolder(invoiceNo, bill);
            } catch (IOException fileException) {
                AlertUtils.showError("File Error", "Checkout completed, but the invoice file could not be saved: "
                        + fileException.getMessage());
            }

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

    private void saveInvoiceToFolder(String invoiceNumber, Bill bill) throws IOException {
        Path rootDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        if (rootDir.getFileName() != null && "hotel-app".equalsIgnoreCase(rootDir.getFileName().toString())
                && rootDir.getParent() != null) {
            rootDir = rootDir.getParent();
        }

        Path invoicesDir = rootDir.resolve("invoices");
        Files.createDirectories(invoicesDir);

        Path invoiceFile = invoicesDir.resolve(invoiceNumber + ".txt");
        String content = buildInvoiceFileContent(invoiceNumber, bill);
        Files.writeString(invoiceFile, content, StandardCharsets.UTF_8);
    }

    private String buildInvoiceFileContent(String invoiceNumber, Bill bill) {
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append("OSDL Hotel Management").append(lineSeparator);
        builder.append("Invoice Number: ").append(invoiceNumber).append(lineSeparator);
        builder.append("Generated On: ").append(generatedAtLabel.getText()).append(lineSeparator);
        builder.append("Booking ID: ").append(bill.getBookingId()).append(lineSeparator);
        builder.append("Customer Name: ").append(customer.getName()).append(lineSeparator);
        builder.append("Contact Number: ").append(customer.getContactNumber()).append(lineSeparator);
        builder.append("Room Number: ").append(room.getRoomNumber()).append(lineSeparator);
        builder.append("Room Type: ").append(room.getRoomType()).append(lineSeparator);
        builder.append("Check In: ").append(checkInLabel.getText()).append(lineSeparator);
        builder.append("Check Out: ").append(checkOutLabel.getText()).append(lineSeparator);
        builder.append("Nights: ").append(bill.getNumberOfDays()).append(lineSeparator);
        builder.append("Rate Per Day: Rs. ").append(String.format("%.2f", bill.getRoomPrice())).append(lineSeparator);
        builder.append("Subtotal: Rs. ").append(String.format("%.2f", subtotalAmount)).append(lineSeparator);
        builder.append("Discount: Rs. ").append(String.format("%.2f", bill.getDiscountAmount())).append(lineSeparator);
        builder.append("Total Amount: Rs. ").append(String.format("%.2f", bill.getTotalAmount())).append(lineSeparator);
        return builder.toString();
    }
}
