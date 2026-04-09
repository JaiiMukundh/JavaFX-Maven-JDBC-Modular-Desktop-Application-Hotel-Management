package com.hotelmanagement.dao;

import com.hotelmanagement.model.Booking;
import com.hotelmanagement.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {
    private static final String SELECT_ALL =
            "SELECT b.booking_id, b.customer_id, b.room_id, b.check_in_date, b.check_out_date, b.status, " +
            "r.room_number " +
            "FROM bookings b INNER JOIN rooms r ON b.room_id = r.room_id";
    private static final String SELECT_ACTIVE = SELECT_ALL + " WHERE b.status = 'ACTIVE'";

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
             PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL + " WHERE b.booking_id = ?")) {
            pstmt.setInt(1, bookingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapBooking(rs) : null;
            }
        }
    }

    public List<Booking> getByCustomerId(int customerId) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String sql = SELECT_ALL + " WHERE b.customer_id = ? AND b.status = 'ACTIVE'";
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
        Booking booking = new Booking(
                rs.getInt("booking_id"),
                rs.getInt("customer_id"),
                rs.getInt("room_id"),
                readBookingDate(rs, "check_in_date"),
                readBookingDate(rs, "check_out_date"),
                rs.getString("status")
        );
        try {
            booking.setRoomNumber(rs.getString("room_number"));
        } catch (SQLException ignored) {
            booking.setRoomNumber(String.valueOf(booking.getRoomId()));
        }
        return booking;
    }

    private LocalDate readBookingDate(ResultSet rs, String columnLabel) throws SQLException {
        Object value = rs.getObject(columnLabel);
        if (value == null) {
            throw new SQLException("Missing booking date for column: " + columnLabel);
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            throw new SQLException("Empty booking date for column: " + columnLabel);
        }

        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                long epoch = Long.parseLong(text);
                return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate();
            } catch (NumberFormatException numberFormatException) {
                try {
                    return Date.valueOf(text).toLocalDate();
                } catch (IllegalArgumentException dateException) {
                    throw new SQLException("Unsupported booking date value in " + columnLabel + ": " + text,
                            dateException);
                }
            }
        }
    }
}
