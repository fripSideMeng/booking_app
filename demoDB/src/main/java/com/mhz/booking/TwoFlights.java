package com.mhz.booking;

public class TwoFlights {
    private int f1_fid;
    private int f1_dayOfMonth;
    private String f1_carrierId;
    private String f1_flightNum;
    private String f1_originCity;
    private String f1_destCity;
    private int f1_time;
    private int f1_capacity;
    private int f1_price;
    private int f2_fid;
    private int f2_dayOfMonth;
    private String f2_carrierId;
    private String f2_flightNum;
    private String f2_originCity;
    private String f2_destCity;
    private int f2_time;
    private int f2_capacity;
    private int f2_price;
    public TwoFlights(int f1_fid, int f2_fid, int f1_dayOfMonth, int f2_dayOfMonth,
                      String f1_carrierId, String f2_carrierId, String f1_flightNum, String f2_flightNum,
                      String f1_originCity, String f2_originCity, String f1_destCity, String f2_destCity,
                      int f1_time, int f2_time, int f1_capacity, int f2_capacity,
                      int f1_price, int f2_price) {
        this.f1_fid = f1_fid;
        this.f2_fid = f2_fid;
        this.f1_dayOfMonth = f1_dayOfMonth;
        this.f2_dayOfMonth = f2_dayOfMonth;
        this.f1_carrierId = f1_carrierId;
        this.f2_carrierId = f2_carrierId;
        this.f1_flightNum = f1_flightNum;
        this.f2_flightNum = f2_flightNum;
        this.f1_originCity = f1_originCity;
        this.f2_originCity = f2_originCity;
        this.f1_destCity = f1_destCity;
        this.f2_destCity = f2_destCity;
        this.f1_time = f1_time;
        this.f2_time = f2_time;
        this.f1_capacity = f1_capacity;
        this.f2_capacity = f2_capacity;
        this.f1_price = f1_price;
        this.f2_price = f2_price;
    }
    public TwoFlights() {}
    protected Flight getItinerary1() {
        return new Flight(f1_fid, f1_dayOfMonth, f1_carrierId, f1_flightNum, f1_originCity,
                f1_destCity, f1_time, f1_capacity, f1_price);
    }
    protected Flight getItinerary2() {
        return new Flight(f2_fid, f2_dayOfMonth, f2_carrierId, f2_flightNum, f2_originCity,
                f2_destCity, f2_time, f2_capacity, f2_price);
    }
    public int totalTime() {
        return f1_time + f2_time;
    }
    protected int getF1_fid() {
        return f1_fid;
    }
    protected int getF2_fid() {
        return f2_fid;
    }
    protected int getPrice() {
        return f1_price + f2_price;
    }
    protected int getDayOfMonth() {
        return f1_dayOfMonth;
    }
    @Override
    public String toString() {
        return getItinerary1().toString() + getItinerary2().toString();
    }
}
