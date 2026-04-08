package com.hotelmanagement.model;

public class Bill {
    private int billId;
    private int bookingId;
    private double roomPrice;
    private int numberOfDays;
    private double discountAmount;
    private double totalAmount;
    private String customerName;
    private int roomNumber;

    public Bill() {
    }

    public Bill(int bookingId, double roomPrice, int numberOfDays) {
        this.bookingId = bookingId;
        this.roomPrice = roomPrice;
        this.numberOfDays = numberOfDays;
    }

    public Bill(int billId, int bookingId, double roomPrice, int numberOfDays, double discountAmount, double totalAmount) {
        this.billId = billId;
        this.bookingId = bookingId;
        this.roomPrice = roomPrice;
        this.numberOfDays = numberOfDays;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
    }

    public Bill(int billId, int bookingId, double roomPrice, int numberOfDays, double discountAmount, double totalAmount, String customerName, int roomNumber) {
        this.billId = billId;
        this.bookingId = bookingId;
        this.roomPrice = roomPrice;
        this.numberOfDays = numberOfDays;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.customerName = customerName;
        this.roomNumber = roomNumber;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public double getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(double roomPrice) {
        this.roomPrice = roomPrice;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "billId=" + billId +
                ", bookingId=" + bookingId +
                ", roomPrice=" + roomPrice +
                ", numberOfDays=" + numberOfDays +
                ", discountAmount=" + discountAmount +
                ", totalAmount=" + totalAmount +
                ", customerName='" + customerName + '\'' +
                ", roomNumber=" + roomNumber +
                '}';
    }
}
