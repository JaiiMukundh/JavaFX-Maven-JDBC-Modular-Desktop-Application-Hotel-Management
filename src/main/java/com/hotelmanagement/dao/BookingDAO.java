package com.hotelmanagement.dao;

import com.hotelmanagement.model.Booking;
import com.hotelmanagement.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {
    private static final String SELECT_ALL = "SELECT * FROM bookings";
    private static final String SELECT_ACTIVE = SELECT_ALL + " WHERE status = 'ACTIVE'";

    public void create(Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (customer_id, room_id, check_in_date, check_out_date, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, booking.getCustomerId());
            pstmt.setInt(2, booking.getRoomId());
            pstmt.setDate(3, Date.valueOf(booking.getCheckInDate()));
            pstmt.setDate(4, Date.valueOf(booking.getCheckOutDate()));
            pstmt.setString(5, booking.getStatus());
            pstmt.executeUpdate();
        }
    }

    public List<Booking> getAll() throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                bookings.add(mapBooking(rs));
            }
        }
        return bookings;
    }

    public List<Booking> getActiveBookings() throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ACTIVE)) {
            while (rs.next()) {
                bookings.add(mapBooking(rs));
            }
        }
        return bookings;
    }

    public Booking getById(int bookingId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL + " WHERE booking_id = ?")) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapBooking(rs) : null;
            }
        }
    }

    public List<Booking> getByCustomerId(int customerId) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE customer_id = ? AND status = 'ACTIVE'";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
            PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        }
        return bookings;
    }

    public List<Booking> getByRoomIdAndDateRange(int roomId, LocalDate checkInDate, LocalDate checkOutDate) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE room_id = ? AND status = 'ACTIVE' " +
                     "AND NOT (check_out_date <= ? OR check_in_date >= ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setDate(2, Date.valueOf(checkInDate));
            pstmt.setDate(3, Date.valueOf(checkOutDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        }
        return bookings;
    }

    public void updateStatus(int bookingId, String status) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, bookingId);
            pstmt.executeUpdate();
        }
    }

    public void delete(int bookingId) throws SQLException {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookingId);
            pstmt.executeUpdate();
        }
    }

    public void deleteByRoomId(int roomId) throws SQLException {
        String sql = "DELETE FROM bookings WHERE room_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate();
        }
    }

    public void deleteByCustomerId(int customerId) throws SQLException {
        String sql = "DELETE FROM bookings WHERE customer_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.executeUpdate();
        }
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        return new Booking(
                rs.getInt("booking_id"),
                rs.getInt("customer_id"),
                rs.getInt("room_id"),
                rs.getDate("check_in_date").toLocalDate(),
                rs.getDate("check_out_date").toLocalDate(),
                rs.getString("status")
        );
    }
}
