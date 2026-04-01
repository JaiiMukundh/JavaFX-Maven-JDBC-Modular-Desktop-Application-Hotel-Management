package com.hotelmanagement.dao;

import com.hotelmanagement.model.Bill;
import com.hotelmanagement.model.Booking;
import com.hotelmanagement.model.Customer;
import com.hotelmanagement.model.Room;
import com.hotelmanagement.utils.DatabaseConnection;
import com.hotelmanagement.utils.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseCascadeTest {
    private Path tempDb;

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final BillDAO billDAO = new BillDAO();

    @BeforeEach
    void setUp() throws Exception {
        tempDb = Files.createTempFile("hotel-app-test", ".db");
        System.setProperty("hotel.db.url", "jdbc:sqlite:" + tempDb);
        DatabaseInitializer.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("hotel.db.url");
        Files.deleteIfExists(tempDb);
    }

    @Test
    void deletingRoomCascadesToBookingsAndBills() throws Exception {
        int customerId = createCustomer("Ada", "9876543210");
        int roomId = createRoom("401", "Deluxe", 3200.0);
        int bookingId = createBooking(customerId, roomId);
        billDAO.create(new Bill(bookingId, 3200.0, 2));

        roomDAO.delete(roomId);

        assertNull(roomDAO.getById(roomId));
        assertNull(bookingDAO.getById(bookingId));
        assertNull(billDAO.getByBookingId(bookingId));
    }

    @Test
    void deletingCustomerCascadesToBookingsAndBills() throws Exception {
        int customerId = createCustomer("Mira", "9123456789");
        int roomId = createRoom("305", "Single", 1800.0);
        int bookingId = createBooking(customerId, roomId);
        billDAO.create(new Bill(bookingId, 1800.0, 3));

        customerDAO.delete(customerId);

        assertNull(customerDAO.getById(customerId));
        assertNull(bookingDAO.getById(bookingId));
        assertNull(billDAO.getByBookingId(bookingId));
        assertNotNull(roomDAO.getById(roomId));
    }

    @Test
    void sqliteConnectionsAlwaysEnableForeignKeys() throws Exception {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, rs.getInt(1));
        }
    }

    private int createCustomer(String name, String contactNumber) throws Exception {
        customerDAO.create(new Customer(name, contactNumber, "101"));
        return customerDAO.getAll().getFirst().getCustomerId();
    }

    private int createRoom(String roomNumber, String roomType, double pricePerDay) throws Exception {
        roomDAO.create(new Room(roomNumber, roomType, pricePerDay, true));
        return roomDAO.getByRoomNumber(roomNumber).getRoomId();
    }

    private int createBooking(int customerId, int roomId) throws Exception {
        bookingDAO.create(new Booking(customerId, roomId, LocalDate.now(), LocalDate.now().plusDays(2)));
        return bookingDAO.getAll().getFirst().getBookingId();
    }
}
