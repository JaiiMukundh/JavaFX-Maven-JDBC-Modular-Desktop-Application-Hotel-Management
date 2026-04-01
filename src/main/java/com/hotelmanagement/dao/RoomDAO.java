package com.hotelmanagement.dao;

import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {
    private static final String SELECT_ALL = "SELECT * FROM rooms";
    private static final String SELECT_BY_ID = SELECT_ALL + " WHERE room_id = ?";
    private static final String SELECT_BY_ROOM_NUMBER = SELECT_ALL + " WHERE room_number = ?";
    private static final String SELECT_AVAILABLE = SELECT_ALL + " WHERE is_available = 1";

    public void create(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, room_type, price_per_day, is_available) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getRoomNumber());
            pstmt.setString(2, room.getRoomType());
            pstmt.setDouble(3, room.getPricePerDay());
            pstmt.setBoolean(4, room.isAvailable());
            pstmt.executeUpdate();
        }
    }

    public List<Room> getAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL)) {
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        }
        return rooms;
    }

    public Room getById(int roomId) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_BY_ID)) {
            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRoom(rs) : null;
            }
        }
    }

    public Room getByRoomNumber(String roomNumber) throws SQLException {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(SELECT_BY_ROOM_NUMBER)) {
            pstmt.setString(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? mapRoom(rs) : null;
            }
        }
    }

    public List<Room> getAvailableRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_AVAILABLE)) {
            while (rs.next()) {
                rooms.add(mapRoom(rs));
            }
        }
        return rooms;
    }

    public void update(Room room) throws SQLException {
        String sql = "UPDATE rooms SET room_type = ?, price_per_day = ?, is_available = ? WHERE room_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, room.getRoomType());
            pstmt.setDouble(2, room.getPricePerDay());
            pstmt.setBoolean(3, room.isAvailable());
            pstmt.setInt(4, room.getRoomId());
            pstmt.executeUpdate();
        }
    }

    public void updateAvailability(int roomId, boolean isAvailable) throws SQLException {
        String sql = "UPDATE rooms SET is_available = ? WHERE room_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, isAvailable);
            pstmt.setInt(2, roomId);
            pstmt.executeUpdate();
        }
    }

    public void delete(int roomId) throws SQLException {
        String sql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.executeUpdate();
        }
    }

    private Room mapRoom(ResultSet rs) throws SQLException {
        return new Room(
                rs.getInt("room_id"),
                rs.getString("room_number"),
                rs.getString("room_type"),
                rs.getDouble("price_per_day"),
                rs.getBoolean("is_available")
        );
    }
}
