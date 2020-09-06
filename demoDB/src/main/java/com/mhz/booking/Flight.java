package com.mhz.booking;

public class Flight {
    private int fid;
    private int dayOfMonth;
    private String carrierId;
    private String flightNum;
    private String originCity;
    private String destCity;
    private int time;
    private int capacity;
    private int price;

    public Flight(int fid, int dayOfMonth, String carrierId, String flightNum,
                  String originCity, String destCity, int time, int capacity, int price) {
        this.fid = fid;
        this.dayOfMonth = dayOfMonth;
        this.carrierId = carrierId;
        this.flightNum = flightNum;
        this.originCity = originCity;
        this.destCity = destCity;
        this.time = time;
        this.capacity = capacity;
        this.price = price;
    }

    public Flight() {}

    public int getFid() {
        return fid;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public int getPrice() {
        return price;
    }

    public int getTime() {
        return time;
    }

    public String getCarrierId() {
        return carrierId;
    }

    public String getDestCity() {
        return destCity;
    }

    public String getOriginCity() {
        return originCity;
    }

    public String getFlightNum() {
        return flightNum;
    }
}
