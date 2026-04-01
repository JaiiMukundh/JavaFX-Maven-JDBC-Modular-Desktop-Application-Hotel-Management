package com.hotelmanagement.dao;

import com.hotelmanagement.model.Customer;
import com.hotelmanagement.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {
    private static final String SELECT_ALL = "SELECT * FROM customers";
    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE customer_id = ?";

    public void create(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (name, contact_number, selected_room_number) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getContactNumber());
            pstmt.setString(3, customer.getSelectedRoomNumber());
            pstmt.executeUpdate();
        }
    }

    public List<Customer> getAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }
        }
        return customers;
    }

    public Customer getById(int customerId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_BY_ID)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapCustomer(rs) : null;
            }
        }
    }

    public void update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET name = ?, contact_number = ?, selected_room_number = ? WHERE customer_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getContactNumber());
            pstmt.setString(3, customer.getSelectedRoomNumber());
            pstmt.setInt(4, customer.getCustomerId());
            pstmt.executeUpdate();
        }
    }

    public void delete(int customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.executeUpdate();
        }
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("name"),
                rs.getString("contact_number"),
                rs.getString("selected_room_number")
        );
    }
}
