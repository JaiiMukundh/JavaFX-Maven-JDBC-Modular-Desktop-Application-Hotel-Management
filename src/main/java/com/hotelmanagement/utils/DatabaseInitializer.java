package com.hotelmanagement.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            if (connection != null) {
                String schema = readSqlSchema();
                executeSqlScript(connection, schema);
                ensureCustomerColumns(connection);
                ensureBillColumns(connection);
                migrateLegacyForeignKeys(connection);
                normalizeBookingDates(connection);
                if (isDefaultDatabase()) {
                    seedDemoData(connection);
                }
                System.out.println("Database initialized successfully.");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private static String readSqlSchema() throws IOException {
        StringBuilder sqlScript = new StringBuilder();
        InputStream inputStream = DatabaseInitializer.class
                .getResourceAsStream("/com/hotelmanagement/sql/schema.sql");

        if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().startsWith("--") && !line.trim().isEmpty()) {
                        sqlScript.append(line).append("\n");
                    }
                }
            }
        }
        return sqlScript.toString();
    }

    private static void executeSqlScript(Connection connection, String schema) throws SQLException {
        String[] statements = schema.split(";");
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    statement.execute(trimmedSql);
                }
            }
        }
    }

    private static void migrateLegacyForeignKeys(Connection connection) throws SQLException {
        boolean bookingsNeedMigration =
                !hasCascadeDelete(connection, "bookings", "rooms")
                        || !hasCascadeDelete(connection, "bookings", "customers");
        boolean billsNeedMigration = !hasCascadeDelete(connection, "bills", "bookings");

        if (!bookingsNeedMigration && !billsNeedMigration) {
            return;
        }

        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");

            if (tableExists(connection, "bills")) {
                statement.execute("ALTER TABLE bills RENAME TO bills_legacy");
            }
            if (tableExists(connection, "bookings")) {
                statement.execute("ALTER TABLE bookings RENAME TO bookings_legacy");
            }

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        booking_id INTEGER PRIMARY KEY,
                        customer_id INTEGER NOT NULL,
                        room_id INTEGER NOT NULL,
                        check_in_date DATE NOT NULL,
                        check_out_date DATE NOT NULL,
                        status VARCHAR(20) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
                        FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bills (
                        bill_id INTEGER PRIMARY KEY,
                        booking_id INTEGER NOT NULL,
                        room_price DECIMAL(10, 2) NOT NULL,
                        number_of_days INTEGER NOT NULL,
                        discount_amount DECIMAL(10, 2) DEFAULT 0,
                        total_amount DECIMAL(10, 2) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
                    )
                    """);

            if (tableExists(connection, "bookings_legacy")) {
                statement.execute("""
                        INSERT INTO bookings (booking_id, customer_id, room_id, check_in_date, check_out_date, status, created_at)
                        SELECT b.booking_id, b.customer_id, b.room_id, b.check_in_date, b.check_out_date,
                               COALESCE(b.status, 'ACTIVE'), COALESCE(b.created_at, CURRENT_TIMESTAMP)
                        FROM bookings_legacy b
                        INNER JOIN customers c ON c.customer_id = b.customer_id
                        INNER JOIN rooms r ON r.room_id = b.room_id
                        """);
            }

            if (tableExists(connection, "bills_legacy")) {
                statement.execute("""
                        INSERT INTO bills (bill_id, booking_id, room_price, number_of_days, discount_amount, total_amount, created_at)
                        SELECT bl.bill_id, bl.booking_id, bl.room_price, bl.number_of_days, COALESCE(bl.discount_amount, 0), bl.total_amount,
                                COALESCE(bl.created_at, CURRENT_TIMESTAMP)
                        FROM bills_legacy bl
                        INNER JOIN bookings b ON b.booking_id = bl.booking_id
                        """);
            }

            statement.execute("DROP TABLE IF EXISTS bills_legacy");
            statement.execute("DROP TABLE IF EXISTS bookings_legacy");

            statement.execute("CREATE INDEX IF NOT EXISTS idx_bookings_customer ON bookings(customer_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bookings_room ON bookings(room_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_bills_booking ON bills(booking_id)");
            statement.execute("PRAGMA foreign_keys = ON");

            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static void ensureCustomerColumns(Connection connection) throws SQLException {
        if (!columnExists(connection, "customers", "selected_room_number")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE customers ADD COLUMN selected_room_number VARCHAR(10) DEFAULT ''");
            }
        }
    }

    private static void ensureBillColumns(Connection connection) throws SQLException {
        if (!columnExists(connection, "bills", "discount_amount")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE bills ADD COLUMN discount_amount DECIMAL(10, 2) DEFAULT 0");
            }
        }
    }

    private static void normalizeBookingDates(Connection connection) throws SQLException {
        if (!tableExists(connection, "bookings")) {
            return;
        }

        String selectSql = "SELECT booking_id, check_in_date, check_out_date FROM bookings";
        String updateSql = "UPDATE bookings SET check_in_date = ?, check_out_date = ? WHERE booking_id = ?";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(selectSql);
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (rs.next()) {
                int bookingId = rs.getInt("booking_id");
                LocalDate checkInDate = parseBookingDateValue(rs.getObject("check_in_date"));
                LocalDate checkOutDate = parseBookingDateValue(rs.getObject("check_out_date"));

                if (checkInDate != null && checkOutDate != null) {
                    update.setDate(1, Date.valueOf(checkInDate));
                    update.setDate(2, Date.valueOf(checkOutDate));
                    update.setInt(3, bookingId);
                    update.addBatch();
                }
            }
            update.executeBatch();
        }
    }

    private static LocalDate parseBookingDateValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        if (value instanceof Number number) {
            return java.time.Instant.ofEpochMilli(number.longValue())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
            try {
                long epoch = Long.parseLong(text);
                return java.time.Instant.ofEpochMilli(epoch)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception ignoredAgain) {
                try {
                    return Date.valueOf(text).toLocalDate();
                } catch (Exception ignoredThird) {
                    return null;
                }
            }
        }
    }

    private static void seedDemoData(Connection connection) throws SQLException {
        seedRooms(connection);
        seedCustomers(connection);
        normalizeLegacySampleCustomers(connection);
        seedBookings(connection);
    }

    private static void seedRooms(Connection connection) throws SQLException {
        List<RoomSeed> rooms = List.of(
                new RoomSeed(1, "201", "Single", 1800.00, true),
                new RoomSeed(2, "202", "Double", 2400.00, true),
                new RoomSeed(3, "203", "Deluxe", 3200.00, true),
                new RoomSeed(4, "204", "Suite", 5200.00, true),
                new RoomSeed(5, "205", "Single", 1750.00, false),
                new RoomSeed(6, "206", "Double", 2550.00, false),
                new RoomSeed(7, "207", "Deluxe", 3400.00, false),
                new RoomSeed(8, "208", "Suite", 5500.00, false),
                new RoomSeed(9, "209", "Single", 1900.00, false),
                new RoomSeed(10, "210", "Double", 2600.00, true),
                new RoomSeed(11, "211", "Deluxe", 3600.00, true),
                new RoomSeed(12, "212", "Suite", 6000.00, true),
                new RoomSeed(13, "213", "Single", 1850.00, false),
                new RoomSeed(14, "214", "Double", 2750.00, false),
                new RoomSeed(15, "215", "Suite", 6400.00, false)
        );

        String sql = "INSERT OR IGNORE INTO rooms (room_id, room_number, room_type, price_per_day, is_available) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (RoomSeed room : rooms) {
                pstmt.setInt(1, room.id());
                pstmt.setString(2, room.number());
                pstmt.setString(3, room.type());
                pstmt.setDouble(4, room.price());
                pstmt.setBoolean(5, room.available());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static void seedCustomers(Connection connection) throws SQLException {
        List<CustomerSeed> customers = List.of(
                new CustomerSeed(1, "Aarav Sharma", "9876500001", "201"),
                new CustomerSeed(2, "Priya Iyer", "9876500002", ""),
                new CustomerSeed(3, "Rohan Mehta", "9876500003", "202"),
                new CustomerSeed(4, "Ananya Gupta", "9876500004", "203"),
                new CustomerSeed(5, "Vikram Singh", "9876500005", "204"),
                new CustomerSeed(6, "Neha Kapoor", "9876500006", "205"),
                new CustomerSeed(7, "Aditya Rao", "9876500007", "206"),
                new CustomerSeed(8, "Sneha Patil", "9876500008", "207"),
                new CustomerSeed(9, "Kabir Malhotra", "9876500009", "208"),
                new CustomerSeed(10, "Pooja Nair", "9876500010", "209"),
                new CustomerSeed(11, "Arjun Verma", "9876500011", "210"),
                new CustomerSeed(12, "Ishita Bansal", "9876500012", "211"),
                new CustomerSeed(13, "Karan Joshi", "9876500013", "212"),
                new CustomerSeed(14, "Meera Chatterjee", "9876500014", ""),
                new CustomerSeed(15, "Rahul Khanna", "9876500015", "")
        );

        String sql = "INSERT OR IGNORE INTO customers (customer_id, name, contact_number, selected_room_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (CustomerSeed customer : customers) {
                pstmt.setInt(1, customer.id());
                pstmt.setString(2, customer.name());
                pstmt.setString(3, customer.contactNumber());
                pstmt.setString(4, customer.selectedRoomNumber());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static void normalizeLegacySampleCustomers(Connection connection) throws SQLException {
        updateCustomerNameIfPresent(connection, 1, "jaii", "Aarav Sharma");
        updateCustomerNameIfPresent(connection, 2, "mahesh", "Priya Iyer");
    }

    private static void updateCustomerNameIfPresent(Connection connection, int customerId, String legacyName, String seedName) throws SQLException {
        String selectSql = "SELECT name FROM customers WHERE customer_id = ?";
        String updateSql = "UPDATE customers SET name = ? WHERE customer_id = ?";
        try (PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setInt(1, customerId);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next() && legacyName.equalsIgnoreCase(rs.getString("name"))) {
                    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                        update.setString(1, seedName);
                        update.setInt(2, customerId);
                        update.executeUpdate();
                    }
                }
            }
        }
    }

    private static void seedBookings(Connection connection) throws SQLException {
        LocalDate today = LocalDate.now();
        List<BookingSeed> bookings = List.of(
                new BookingSeed(1, 3, 1, today.minusDays(18), today.minusDays(15), "CHECKED_OUT"),
                new BookingSeed(2, 4, 4, today.minusDays(16), today.minusDays(13), "CHECKED_OUT"),
                new BookingSeed(3, 2, 3, today.minusDays(12), today.minusDays(10), "CHECKED_OUT"),
                new BookingSeed(4, 2, 3, today.minusDays(9), today.minusDays(7), "CHECKED_OUT"),
                new BookingSeed(5, 5, 5, today.plusDays(1), today.plusDays(4), "ACTIVE"),
                new BookingSeed(6, 1, 3, today.plusDays(2), today.plusDays(5), "ACTIVE"),
                new BookingSeed(7, 6, 6, today.plusDays(3), today.plusDays(6), "ACTIVE"),
                new BookingSeed(8, 7, 7, today.plusDays(4), today.plusDays(7), "ACTIVE"),
                new BookingSeed(9, 8, 8, today.plusDays(5), today.plusDays(8), "ACTIVE"),
                new BookingSeed(10, 9, 9, today.minusDays(8), today.minusDays(5), "CHECKED_OUT"),
                new BookingSeed(11, 10, 10, today.minusDays(7), today.minusDays(4), "CHECKED_OUT"),
                new BookingSeed(12, 11, 11, today.minusDays(6), today.minusDays(3), "CHECKED_OUT"),
                new BookingSeed(13, 12, 12, today.minusDays(5), today.minusDays(2), "CHECKED_OUT"),
                new BookingSeed(14, 13, 13, today.minusDays(4), today.minusDays(1), "CHECKED_OUT"),
                new BookingSeed(15, 14, 14, today.plusDays(6), today.plusDays(9), "ACTIVE")
        );

        String sql = "INSERT OR IGNORE INTO bookings (booking_id, customer_id, room_id, check_in_date, check_out_date, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (BookingSeed booking : bookings) {
                pstmt.setInt(1, booking.id());
                pstmt.setInt(2, booking.customerId());
                pstmt.setInt(3, booking.roomId());
                pstmt.setDate(4, Date.valueOf(booking.checkInDate()));
                pstmt.setDate(5, Date.valueOf(booking.checkOutDate()));
                pstmt.setString(6, booking.status());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static boolean hasCascadeDelete(Connection connection, String tableName, String referencedTable) throws SQLException {
        if (!tableExists(connection, tableName)) {
            return false;
        }

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA foreign_key_list(" + tableName + ")")) {
            while (rs.next()) {
                if (referencedTable.equalsIgnoreCase(rs.getString("table"))
                        && "CASCADE".equalsIgnoreCase(rs.getString("on_delete"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isDefaultDatabase() {
        String configuredUrl = System.getProperty("hotel.db.url");
        return configuredUrl == null || configuredUrl.isBlank();
    }

    private record RoomSeed(int id, String number, String type, double price, boolean available) {
    }

    private record CustomerSeed(int id, String name, String contactNumber, String selectedRoomNumber) {
    }

    private record BookingSeed(int id, int customerId, int roomId, LocalDate checkInDate, LocalDate checkOutDate, String status) {
    }
}
