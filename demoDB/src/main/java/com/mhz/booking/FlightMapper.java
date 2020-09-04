package com.mhz.booking;

import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.ArrayList;
public interface FlightMapper {
    ArrayList<Flight> transaction_search(@Param("originCity") String originCity,
                                   @Param("destCity") String destCity,
                                   @Param("dayOfMonth") int dayOfMonth,
                                   @Param("numOfFlights") int num);
    ArrayList<TwoFlights> transaction_search_indirect(@Param("originCity") String originCity,
                                                 @Param("destCity") String destCity,
                                                 @Param("dayOfMonth") int dayOfMonth,
                                                 @Param("numOfFlights") int num);
    User transaction_login(@Param("userName") String userName);
    String checkIfUserExists(@Param("userName") String userName);
    void transaction_createUser(@Param("userName") String userName,
                                @Param("password") byte[] password,
                                @Param("salt") byte[] salt,
                                @Param("balance") int balance);
    Flight checkCapacity(@Param("fid") int fid);
    int checkBookedSeats1(@Param("Fid1") int fid);
    int checkBookedSeats2(@Param("Fid2") int fid);
    Integer checkCurrentReservation(@Param("userName") String userName,
                                 @Param("day") int day);
    void insert_reservation(@Param("userName") String userName,
                           @Param("paidOrNot") int paidOrNot,
                           @Param("price") int price,
                           @Param("day") int day,
                           @Param("fid1") int fid1,
                           @Param("fid2") int fid2);
    void update_reservation(@Param("fid2") int fid2,
                            @Param("userName") String userName,
                            @Param("fid1") int fid1);
    ArrayList<Reservation> list_reservations(@Param("userName") String userName);
    Reservation retrieve_price(@Param("reservationId") int reservationId);
    int balance_check(@Param("userName") String userName);
    void update_balance(@Param("userName") String userName, @Param("newBalance") int newBalance);
    void update_payment_status(@Param("userName") String userName,
                               @Param("day") int day);
    void cancel_reservation(@Param("userName") String userName,
                            @Param("day") int day);
    void user_refund(@Param("userName") String userName,
                     @Param("refund") int refund);
}
