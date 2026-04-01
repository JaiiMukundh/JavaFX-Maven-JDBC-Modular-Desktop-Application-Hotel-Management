# Hotel Management System

A modern, modular desktop application for managing hotel operations built with JavaFX, Maven, and JDBC.

![Java](https://img.shields.io/badge/Java-17+-orange)
![Maven](https://img.shields.io/badge/Maven-3.8+-blue)
![License](https://img.shields.io/badge/License-MIT-green)

## Overview

This Hotel Management System is a comprehensive desktop application designed to streamline hotel operations. It provides an intuitive interface for managing customers, rooms, bookings, and billing information. The application follows a modular architecture using Java 9+ modules and uses SQLite for persistent data storage.

## ✨ Features

- **Customer Management**: Add, edit, delete, and view customer information
- **Room Management**: Manage room inventory, availability, and pricing
- **Booking System**: Create and manage room bookings with flexible checkout options
- **Billing**: Generate and manage bills for bookings
- **Database-Driven**: Persistent storage using SQLite
- **Modern UI**: Clean and responsive JavaFX interface with CSS styling
- **Modular Architecture**: Built on Java Module System for better code organization

## 🛠️ Tech Stack

- **Language**: Java 17+
- **UI Framework**: JavaFX
- **Build Tool**: Maven
- **Database**: SQLite with JDBC
- **Architecture**: Modular Java (JPMS)

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.8 or higher
- **Git**: For version control

### Verify Installation

```bash
java -version
mvn -version
```

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/JaiiMukundh/JavaFX-Maven-JDBC-Modular-Desktop-Application-Hotel-Management.git
cd hotel-app
```

### 2. Build the Project

```bash
mvn clean install
```

This command will:
- Clean any previous builds
- Download dependencies
- Compile the source code
- Run tests
- Package the application

### 3. Run the Application

```bash
mvn javafx:run
```

Or compile and run directly:

```bash
mvn compile
java -m hotelmanagement/com.hotelmanagement.App
```

## 📁 Project Structure

```
hotel-app/
├── pom.xml                          # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java    # Java module descriptor
│   │   │   └── com/hotelmanagement/
│   │   │       ├── App.java         # Application entry point
│   │   │       ├── controller/      # JavaFX Controllers
│   │   │       │   ├── MainController.java
│   │   │       │   ├── CustomerController.java
│   │   │       │   ├── RoomController.java
│   │   │       │   ├── BookingController.java
│   │   │       │   ├── BillingController.java
│   │   │       │   └── ControllerRegistry.java
│   │   │       ├── dao/             # Data Access Objects
│   │   │       │   ├── CustomerDAO.java
│   │   │       │   ├── RoomDAO.java
│   │   │       │   ├── BookingDAO.java
│   │   │       │   └── BillDAO.java
│   │   │       ├── model/           # Entity Models
│   │   │       │   ├── Customer.java
│   │   │       │   ├── Room.java
│   │   │       │   ├── Booking.java
│   │   │       │   └── Bill.java
│   │   │       └── utils/           # Utility Classes
│   │   │           ├── DatabaseConnection.java
│   │   │           ├── DatabaseInitializer.java
│   │   │           └── AlertUtils.java
│   │   └── resources/
│   │       ├── com/hotelmanagement/
│   │       │   ├── fxml/           # JavaFX FXML UI files
│   │       │   │   ├── main.fxml
│   │       │   │   ├── customer-management.fxml
│   │       │   │   ├── room-management.fxml
│   │       │   │   ├── booking-checkout.fxml
│   │       │   │   └── billing.fxml
│   │       │   ├── css/            # Stylesheets
│   │       │   │   └── styles.css
│   │       │   └── sql/            # Database schema
│   │       │       └── schema.sql
│   └── test/
│       └── java/com/hotelmanagement/
│           └── dao/
│               └── DatabaseCascadeTest.java
└── target/                          # Build output (generated)
```

## 🎯 Usage

### Starting the Application

1. Run the application using Maven:
   ```bash
   mvn javafx:run
   ```

2. The main window will display with the following options:
   - **Customer Management**: Manage guest information
   - **Room Management**: Control room inventory and availability
   - **Bookings**: Create and view room reservations
   - **Billing**: Generate and manage bills

### Key Operations

- **Add New Customer**: Click "Add Customer" and fill in guest details
- **Book a Room**: Select a room, choose dates, and confirm booking
- **Generate Bill**: Create a bill for completed bookings
- **Manage Rooms**: Update room availability and pricing

## 🗄️ Database

The application uses SQLite for data persistence. The database schema is automatically initialized on first run and includes tables for:

- Customers
- Rooms
- Bookings
- Bills

The database file (`hotel_management.db`) is created in the application root directory.

## 🔧 Development

### Build Variants

**Development Build** (with debug info):
```bash
mvn clean compile
```

**Production Build**:
```bash
mvn clean package
```

**Run Tests**:
```bash
mvn test
```

### Code Architecture

- **MVC Pattern**: Controllers handle UI logic, Models represent data
- **DAO Pattern**: Data Access Objects abstract database operations
- **JPMS**: Modular architecture with explicit module dependencies
- **JavaFX FXML**: UI defined in XML for better separation of concerns

## 📝 Dependencies

Key dependencies managed by Maven:

- **JavaFX**: UI framework
- **SQLite JDBC**: Database driver
- **JUnit**: Testing framework

See `pom.xml` for complete dependency list and versions.

## 🐛 Troubleshooting

### Application won't start
- Ensure JDK 17+ is installed: `java -version`
- Verify Maven is installed: `mvn -version`
- Try clean build: `mvn clean install`

### Database errors
- Delete `hotel_management.db` to reset database
- Check database file permissions
- Ensure SQLite JDBC driver is in classpath

### JavaFX module issues
- Verify JavaFX modules are in module path
- Check `module-info.java` for correct module declarations
- Update Maven POM if using different JDK version

## 📚 Additional Resources

- [JavaFX Documentation](https://openjfx.io/)
- [Maven Documentation](https://maven.apache.org/)
- [Java Modules Guide](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Object.html)
- [SQLite JDBC Driver](https://github.com/xerial/sqlite-jdbc)

## 🤝 Contributing

Contributions are welcome! To contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Author

Created by **Jaii Mukundh**

---

**Last Updated**: April 2, 2026

For issues or questions, please open an issue on the [GitHub repository](https://github.com/JaiiMukundh/JavaFX-Maven-JDBC-Modular-Desktop-Application-Hotel-Management/issues).
