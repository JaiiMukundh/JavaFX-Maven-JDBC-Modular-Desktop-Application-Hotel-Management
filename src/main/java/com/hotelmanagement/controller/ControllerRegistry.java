package com.hotelmanagement.controller;

/**
 * A registry to hold references to controllers so they can be accessed and refreshed globally.
 */
public class ControllerRegistry {

    private static BookingController bookingController;
    private static BillingController billingController;

    public static void setBookingController(BookingController controller) {
        bookingController = controller;
    }

    public static BookingController getBookingController() {
        return bookingController;
    }

    public static void setBillingController(BillingController controller) {
        billingController = controller;
    }

    public static BillingController getBillingController() {
        return billingController;
    }

    public static void refreshBooking() {
        if (bookingController != null) {
            bookingController.refreshTable();
        }
    }

    public static void refreshBilling() {
        if (billingController != null) {
            billingController.refreshTable();
        }
    }

    public static void refreshBookingAndBilling() {
        refreshBooking();
        refreshBilling();
    }
}
