package com.mhz.booking;

import org.apache.ibatis.type.Alias;

@Alias("Reservation")
public class Reservation {
    private int reservationId;
    private String userName;
    private int fid1;
    private int fid2;
    private int day;
    private int price;
    private int paidOrNot;
    public Reservation(int reservationId, String userName, int fid1, int fid2, int day, int price, int paidOrNot) {
        this.reservationId = reservationId;
        this.userName = userName;
        this.fid1 = fid1;
        this.fid2 = fid2;
        this.day = day;
        this.price = price;
        this.paidOrNot = paidOrNot;
    }
    public Reservation() {}
    public int getReservationId() { return reservationId; }
    public String getUserName() {
        return userName;
    }
    public int getFid1() {
        return fid1;
    }
    public int getFid2() {
        return fid2;
    }
    public int getDay() {
        return day;
    }
    public int getPrice() {
        return price;
    }
    public int getPaidOrNot() {
        return paidOrNot;
    }
    @Override
    public String toString() {
        return "User: " + userName + " Reservation: " + reservationId + " Day: " + day
                + " Paid or not: " + (paidOrNot == 1 ? "Yes<br/>" : "No<br/>");
    }
}
