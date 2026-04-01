package com.hotelmanagement.dao;

import com.hotelmanagement.model.Bill;
import com.hotelmanagement.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillDAO {
    private static final String SELECT_ALL = "SELECT * FROM bills";
    private static final String ACTIVE_BILLS_SQL =
            "SELECT b.bill_id, b.booking_id, b.room_price, b.number_of_days, b.total_amount, " +
            "c.name as customer_name, r.room_number " +
            "FROM bills b " +
            "INNER JOIN bookings bk ON b.booking_id = bk.booking_id " +
            "INNER JOIN customers c ON bk.customer_id = c.customer_id " +
            "INNER JOIN rooms r ON bk.room_id = r.room_id";

    public void create(Bill bill) throws SQLException {
        String sql = "INSERT INTO bills (booking_id, room_price, number_of_days, total_amount) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bill.getBookingId());
            pstmt.setDouble(2, bill.getRoomPrice());
            pstmt.setInt(3, bill.getNumberOfDays());
            pstmt.setDouble(4, bill.getTotalAmount());
            pstmt.executeUpdate();
        }
    }

    public List<Bill> getAll() throws SQLException {
        List<Bill> bills = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                bills.add(mapBill(rs));
            }
        }
        return bills;
    }

    public Bill getById(int billId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL + " WHERE bill_id = ?")) {
            pstmt.setInt(1, billId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapBill(rs) : null;
            }
        }
    }

    public Bill getByBookingId(int bookingId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL + " WHERE booking_id = ?")) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapBill(rs) : null;
            }
        }
    }

    public List<Bill> getByBookingIdList(int bookingId) throws SQLException {
        List<Bill> bills = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL + " WHERE booking_id = ?")) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bills.add(mapBill(rs));
                }
            }
        }
        return bills;
    }

    public List<Bill> getActiveBills() throws SQLException {
        List<Bill> bills = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(ACTIVE_BILLS_SQL)) {
            while (rs.next()) {
                bills.add(mapActiveBill(rs));
            }
        }
        return bills;
    }

    public void delete(int billId) throws SQLException {
        String sql = "DELETE FROM bills WHERE bill_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, billId);
            pstmt.executeUpdate();
        }
    }

    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM bills";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    private Bill mapBill(ResultSet rs) throws SQLException {
        return new Bill(
                rs.getInt("bill_id"),
                rs.getInt("booking_id"),
                rs.getDouble("room_price"),
                rs.getInt("number_of_days"),
                rs.getDouble("total_amount")
        );
    }

    private Bill mapActiveBill(ResultSet rs) throws SQLException {
        return new Bill(
                rs.getInt("bill_id"),
                rs.getInt("booking_id"),
                rs.getDouble("room_price"),
                rs.getInt("number_of_days"),
                rs.getDouble("total_amount"),
                rs.getString("customer_name"),
                rs.getInt("room_number")
        );
    }
}
