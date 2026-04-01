package com.hotelmanagement.model;

public class Customer {
    private int customerId;
    private String name;
    private String contactNumber;
    private String selectedRoomNumber;

    public Customer() {
    }

    public Customer(String name, String contactNumber, String selectedRoomNumber) {
        this.name = name;
        this.contactNumber = contactNumber;
        this.selectedRoomNumber = selectedRoomNumber;
    }

    public Customer(int customerId, String name, String contactNumber, String selectedRoomNumber) {
        this.customerId = customerId;
        this.name = name;
        this.contactNumber = contactNumber;
        this.selectedRoomNumber = selectedRoomNumber;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSelectedRoomNumber() {
        return selectedRoomNumber;
    }

    public void setSelectedRoomNumber(String selectedRoomNumber) {
        this.selectedRoomNumber = selectedRoomNumber;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", contactNumber='" + contactNumber + '\'' +
                ", selectedRoomNumber='" + selectedRoomNumber + '\'' +
                '}';
    }
}
