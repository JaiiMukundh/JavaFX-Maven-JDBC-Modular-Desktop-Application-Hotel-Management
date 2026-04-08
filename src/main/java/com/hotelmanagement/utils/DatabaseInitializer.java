package com.hotelmanagement.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initializeDatabase() {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            if (connection != null) {
                String schema = readSqlSchema();
                executeSqlScript(connection, schema);
                ensureCustomerColumns(connection);
                ensureBillColumns(connection);
                migrateLegacyForeignKeys(connection);
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
}
