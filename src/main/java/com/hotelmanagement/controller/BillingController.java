package com.hotelmanagement.controller;

import com.hotelmanagement.dao.BillDAO;
import com.hotelmanagement.model.Bill;
import com.hotelmanagement.utils.AlertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class BillingController {

    @FXML
    private ComboBox<String> bookingCombo;
    @FXML
    private Label roomPriceLabel;
    @FXML
    private Label daysLabel;
    @FXML
    private Label totalAmountLabel;
    @FXML
    private TableView<Bill> billTableView;
    @FXML
    private Button deleteBillBtn;
    @FXML
    private Button deleteAllBillsBtn;

    private BillDAO billDAO;
    private List<Bill> allBills;

    @FXML
    @SuppressWarnings("unchecked")
    public void initialize() {
        // Register this controller in the registry
        ControllerRegistry.setBillingController(this);
        
        billDAO = new BillDAO();

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
        
        TableColumn<Bill, Double> totalCol = (TableColumn<Bill, Double>) billTableView.getColumns().get(5);
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        bookingCombo.setOnAction(e -> handleBookingSelection());
        deleteBillBtn.setOnAction(e -> handleDeleteBill());
        deleteAllBillsBtn.setOnAction(e -> handleDeleteAllBills());

        clearBillDisplay();
        refreshBillTable();
        loadBookings();
    }

    private void loadBookings() {
        try {
            List<Bill> bills = billDAO.getActiveBills();
            ObservableList<String> bookingItems = FXCollections.observableArrayList();

            for (Bill b : bills) {
                bookingItems.add("Booking #" + b.getBookingId() + " (Customer: " + b.getCustomerName() + ", Room: " + b.getRoomNumber() + ")");
            }

            bookingCombo.setItems(bookingItems);
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load bookings: " + e.getMessage());
        }
    }

    private void handleBookingSelection() {
        String selectedText = bookingCombo.getValue();
        if (selectedText == null || selectedText.isEmpty()) {
            clearBillDisplay();
            return;
        }

        try {
            int bookingId = Integer.parseInt(selectedText.split("#")[1].split(" ")[0]);

            Bill bill = billDAO.getByBookingId(bookingId);

            if (bill != null) {
                roomPriceLabel.setText("Rs. " + String.format("%.2f", bill.getRoomPrice()));
                daysLabel.setText(String.valueOf(bill.getNumberOfDays()));
                totalAmountLabel.setText("Rs. " + String.format("%.2f", bill.getTotalAmount()));
            } else {
                clearBillDisplay();
                AlertUtils.showWarning("No Bill", "No bill found for this booking yet. Bill is generated on checkout.");
            }
        } catch (SQLException e) {
            AlertUtils.showError("Database Error", "Failed to load bill: " + e.getMessage());
            clearBillDisplay();
        }
    }

    private void clearBillDisplay() {
        roomPriceLabel.setText("Rs. 0.00");
        daysLabel.setText("0");
        totalAmountLabel.setText("Rs. 0.00");
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
        loadBookings();
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
}
