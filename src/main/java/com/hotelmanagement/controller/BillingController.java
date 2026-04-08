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
import com.hotelmanagement.App;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BillingController {
    private static final int LONG_STAY_DISCOUNT_THRESHOLD_DAYS = 7;
    private static final double LONG_STAY_DISCOUNT_RATE = 0.10;

    @FXML
    private ComboBox<String> bookingCombo;
    @FXML
    private Label roomPriceLabel;
    @FXML
    private Label daysLabel;
    @FXML
    private Label discountAmountLabel;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private TableView<Bill> billTableView;
    @FXML
    private Button deleteBillBtn;
    @FXML
    private Button deleteAllBillsBtn;
    @FXML
    private Button checkoutBtn;
    @FXML
    private VBox billingVBox;

    private BillDAO billDAO;
    private BookingDAO bookingDAO;
    private CustomerDAO customerDAO;
    private RoomDAO roomDAO;
    private List<Bill> allBills;
    private Integer selectedBookingId;

    @FXML
    @SuppressWarnings("unchecked")
    public void initialize() {
        // Register this controller in the registry
        ControllerRegistry.setBillingController(this);
        
        billDAO = new BillDAO();
        bookingDAO = new BookingDAO();
        customerDAO = new CustomerDAO();
        roomDAO = new RoomDAO();

        TableColumn<Bill, Integer> billIdCol = (TableColumn<Bill, Integer>) billTableView.getColumns().get(0);
        billIdCol.setCellValueFactory(new PropertyValueFactory<>("billId"));
        
        TableColumn<Bill, String> customerNameCol = (TableColumn<Bill, String>) billTableView.getColumns().get(1);
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        
        TableColumn<Bill, Integer> roomNumberCol = (TableColumn<Bill, Integer>) billTableView.getColumns().get(2);
        roomNumberCol.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        
        TableColumn<Bill, Double> roomPriceCol = (TableColumn<Bill, Double>) billTableView.getColumns().get(3);
        roomPriceCol.setCellValueFactory(new PropertyValueFactory<>("roomPrice"));
        
        TableColumn<Bill, Integer> daysCol = (TableColumn<Bill, Integer>) billTableView.getColumns().get(4);
        daysCol.setCellValueFactory(new PropertyValueFactory<>("numberOfDays"));
        
        TableColumn<Bill, Double> discountCol = (TableColumn<Bill, Double>) billTableView.getColumns().get(5);
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discountAmount"));

        TableColumn<Bill, Double> totalCol = (TableColumn<Bill, Double>) billTableView.getColumns().get(6);
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        bookingCombo.setOnAction(e -> handleBookingSelection());
        deleteBillBtn.setOnAction(e -> handleDeleteBill());
        deleteAllBillsBtn.setOnAction(e -> handleDeleteAllBills());
        checkoutBtn.setOnAction(e -> handleOpenInvoice());

        clearBillDisplay();
        refreshBillTable();
        loadActiveBookings();
    }

    private void loadActiveBookings() {
        try {
            List<Booking> bookings = bookingDAO.getActiveBookings();
            bookingCombo.setItems(FXCollections.observableArrayList(
                    bookings.stream().map(this::formatBookingSelection).toList()
            ));

            if (bookings.isEmpty()) {
                bookingCombo.setPromptText("No active bookings available");
            } else {
                bookingCombo.setPromptText("Select an active booking");
            }
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load active bookings: " + e.getMessage());
        }
    }

    private void handleBookingSelection() {
        String selectedText = bookingCombo.getValue();
        if (selectedText == null || selectedText.isEmpty()) {
            selectedBookingId = null;
            clearBillDisplay();
            return;
        }

        try {
            int bookingId = Integer.parseInt(selectedText.split("#")[1].split(" ")[0]);
            selectedBookingId = bookingId;
            previewBill(bookingId);
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load booking details: " + e.getMessage());
            clearBillDisplay();
        } catch (RuntimeException e) {
            AlertUtils.showError("Error", "Failed to read booking selection.");
            clearBillDisplay();
        }
    }

    private void clearBillDisplay() {
        roomPriceLabel.setText("Rs. 0.00");
        daysLabel.setText("0");
        discountAmountLabel.setText("Rs. 0.00");
        totalAmountLabel.setText("Rs. 0.00");
    }

    private void previewBill(int bookingId) throws SQLException {
        Booking booking = bookingDAO.getById(bookingId);
        if (booking == null) {
            clearBillDisplay();
            AlertUtils.showWarning("No Booking", "The selected booking could not be found.");
            return;
        }

        Customer customer = customerDAO.getById(booking.getCustomerId());
        Room room = roomDAO.getById(booking.getRoomId());
        if (customer == null || room == null) {
            clearBillDisplay();
            AlertUtils.showError("Data Error", "Missing customer or room details for this booking.");
            return;
        }

        long daysStayed = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (daysStayed <= 0) {
            clearBillDisplay();
            AlertUtils.showError("Validation Error", "Booking dates are not valid for billing.");
            return;
        }

        double subtotal = room.getPricePerDay() * daysStayed;
        double discountAmount = calculateDiscount(subtotal, daysStayed);
        double totalAmount = subtotal - discountAmount;
        roomPriceLabel.setText("Rs. " + String.format("%.2f", room.getPricePerDay()));
        daysLabel.setText(String.valueOf(daysStayed));
        discountAmountLabel.setText("Rs. " + String.format("%.2f", discountAmount));
        totalAmountLabel.setText("Rs. " + String.format("%.2f", totalAmount));
    }

    private void refreshBillTable() {
        try {
            allBills = billDAO.getActiveBills();
            billTableView.setItems(FXCollections.observableArrayList(allBills));
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load bills: " + e.getMessage());
        }
    }

    public void refreshTable() {
        clearBillDisplay();
        bookingCombo.setValue(null);
        selectedBookingId = null;
        loadActiveBookings();
        refreshBillTable();
    }

    private void handleDeleteBill() {
        Bill selectedBill = billTableView.getSelectionModel().getSelectedItem();
        if (selectedBill == null) {
            AlertUtils.showWarning("No Selection", "Please select a bill to delete!");
            return;
        }

        if (AlertUtils.showConfirmation("Confirm Deletion", "Are you sure you want to delete Bill #" + selectedBill.getBillId() + "?")) {
            try {
                billDAO.delete(selectedBill.getBillId());
                AlertUtils.showInfo("Success", "Bill deleted successfully!");
                refreshBillTable();
                clearBillDisplay();
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to delete bill: " + e.getMessage());
            }
        }
    }

    private void handleDeleteAllBills() {
        if (allBills == null || allBills.isEmpty()) {
            AlertUtils.showWarning("No Bills", "There are no bills to delete!");
            return;
        }

        if (AlertUtils.showConfirmation("Confirm Deletion", "Are you sure you want to delete ALL billing records?\nThis action cannot be undone!")) {
            try {
                billDAO.deleteAll();
                AlertUtils.showInfo("Success", "All billing records deleted successfully!");
                refreshBillTable();
                clearBillDisplay();
            } catch (SQLException e) {
                AlertUtils.showError("Database Error", "Failed to delete bills: " + e.getMessage());
            }
        }
    }

    private void handleCheckout() {
        handleOpenInvoice();
    }

    private void handleOpenInvoice() {
        if (selectedBookingId == null) {
            AlertUtils.showWarning("No Selection", "Please select a booking to preview the invoice!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/invoice.fxml"));
            Parent root = loader.load();

            InvoiceController controller = loader.getController();
            controller.loadBooking(selectedBookingId);

            Scene scene = new Scene(root, 980, 720);
            String css = App.class.getResource("css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            String themeClass = "light-theme";
            if (billingVBox != null && billingVBox.getScene() != null && billingVBox.getScene().getRoot() != null) {
                if (billingVBox.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                    themeClass = "dark-theme";
                }
            }
            root.getStyleClass().add(themeClass);

            Stage stage = new Stage();
            stage.setTitle("Invoice");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(940);
            stage.setMinHeight(680);
            stage.setScene(scene);
            controller.setStage(stage);
            stage.showAndWait();

            refreshTable();
        } catch (IOException e) {
            AlertUtils.showError("UI Error", "Failed to open invoice window: " + e.getMessage());
        }
    }

    private String formatBookingSelection(Booking booking) {
        try {
            Customer customer = customerDAO.getById(booking.getCustomerId());
            Room room = roomDAO.getById(booking.getRoomId());
            String customerName = customer != null ? customer.getName() : "Unknown";
            String roomNumber = room != null ? room.getRoomNumber() : "Unknown";
            return "Booking #" + booking.getBookingId() + " (Customer: " + customerName + ", Room: " + roomNumber + ")";
        } catch (SQLException e) {
            return "Booking #" + booking.getBookingId();
        }
    }

    private double calculateDiscount(double subtotal, long daysStayed) {
        return daysStayed > LONG_STAY_DISCOUNT_THRESHOLD_DAYS ? subtotal * LONG_STAY_DISCOUNT_RATE : 0.0;
    }
}
