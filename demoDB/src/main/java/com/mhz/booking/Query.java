package com.mhz.booking;

import java.security.spec.KeySpec;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.type.Alias;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Alias("Query")
public class Query {
    private static final int HASH_STRENGTH = 65536;
    private static final int KEY_LENGTH = 128;
    private static User currentUser;
    private static Map<Integer, Reservation> recentSearch;
    public static final int transactionRetry = 4;
    private List<Integer> itineraryIds;
    private SqlSession session;

    private static boolean isNotDeadLock(SQLException e) {
        return e.getErrorCode() != 1205;
    }

    public String getCurrentUserName() {
        return currentUser != null ? currentUser.getUserName() : "Not logged in";
    }

    public Query(SqlSession session) {
        this.session = session;
    }

    public Map<Integer, Reservation> getRecentSearch() {
        return recentSearch;
    }

    // Return connection back to the pool
    public void closeConnection() {
        this.session.close();
    }

    public String login(String userName, String password, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class); // Connection not created yet
            currentUser = flightMapper.transaction_login(userName); // Connection created
            session.commit();
            if (currentUser == null) {
                return "Logged in failed, username error!<br/>";
            }
            byte[] salt = currentUser.getSalt();
            // Specify the hash parameters
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);
            // Generate the hash
            SecretKeyFactory factory;
            byte[] hash;
            try {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                hash = factory.generateSecret(spec).getEncoded();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                throw new IllegalStateException();
            }
            if (Arrays.equals(hash, currentUser.getPasswd())) {
                return "Logged in as " + userName + "<br/>";
            } else {
                return "Logged in failed, password error!<br/>";
            }
        } catch (PersistenceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException && transactionRetry > 0) {
                SQLException e1 = new SQLException(cause);
                if (isNotDeadLock(e1)) {
                    session.rollback();
                }
                return login(userName, password, --transactionRetry);
            } else {
                return "Internal error, try again later.<br/>";
            }
        }
    }
    public String search(String originCity, String destCity,
                         int dayOfMonth, int numOfFlights, boolean indirect) {
        recentSearch = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        FlightMapper flightMapper = session.getMapper(FlightMapper.class);
        List<Flight> flights = flightMapper.transaction_search(originCity, destCity, dayOfMonth, numOfFlights);
        session.commit();
        int itineraryId = 1;
        if (!indirect || flights.size() >= numOfFlights) {
            sb.append("<h1>Search Results</h1>\n<p>");
            for (Flight f : flights) {
                sb.append(f.toString()).append("<br/>");
                if (currentUser != null) {
                    recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                            f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                    itineraryId++;
                }
            }
        } else {
            List<TwoFlights> indirectFlights =
                    flightMapper.transaction_search_indirect(originCity, destCity, dayOfMonth,
                            numOfFlights - flights.size());
            session.commit();
            int index1 = 0;
            int index2 = 0;
            while (index1 < flights.size() && index2 < indirectFlights.size()) {
                Flight f = flights.get(index1);
                int singleTime = f.getTime();
                TwoFlights ff = indirectFlights.get(index2);
                int doubleTime = ff.totalTime();
                if (singleTime < doubleTime) {
                    sb.append(f.toString());
                    if (currentUser != null) {
                        recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                        itineraryId++;
                    }
                    index1++;
                } else if (singleTime == doubleTime) { // break tie on fid
                    if (flights.get(index1).getFid() < indirectFlights.get(index2).getF1_fid()) {
                        sb.append(f.toString());
                        if (currentUser != null) {
                            recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                    f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                            itineraryId++;
                        }
                        index1++;
                    } else {
                        sb.append(ff.toString());
                        if (currentUser != null) {
                            recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                    ff.getF1_fid(), ff.getF2_fid(), ff.getDayOfMonth(), ff.getPrice(), 0));
                            itineraryId++;
                        }
                        index2++;
                    }
                } else {
                    sb.append(ff.toString());
                    if (currentUser != null) {
                        recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                ff.getF1_fid(), ff.getF2_fid(), ff.getDayOfMonth(), ff.getPrice(), 0));
                        itineraryId++;
                    }
                    index2++;
                }
            }
            while (index1 < flights.size()) {
                Flight f = flights.get(index1);
                sb.append(f.toString());
                if (currentUser != null) {
                    recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                            f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                    itineraryId++;
                }
                index1++;
            }
            while (index2 < indirectFlights.size()) {
                TwoFlights ff = indirectFlights.get(index2);
                sb.append(ff.toString());
                if (currentUser != null) {
                    recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                            ff.getF1_fid(), ff.getF2_fid(), ff.getDayOfMonth(), ff.getPrice(), 0));
                    itineraryId++;
                }
                index2++;
            }
        }
        sb.append("</p>\n<hr/>");
        return sb.toString();
    }
    public String createUser(String userName, String password,
                           int balance, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class); // Connection not created yet
            String checkCurrentUsers = flightMapper.checkIfUserExists(userName);
            session.commit();
            if (userName.equals(checkCurrentUsers)) {
                return "User already exists! Please log in.<br/>";
            }
            // Generate a random cryptographic salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            // Specify the hash parameters
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_STRENGTH, KEY_LENGTH);
            // Generate the hash
            SecretKeyFactory factory;
            byte[] hash;
            try {
                factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                hash = factory.generateSecret(spec).getEncoded();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
                throw new IllegalStateException();
            }
            // Connection created
            flightMapper.transaction_createUser(userName, hash, salt, balance);
            session.commit();
            return "User: " + userName + " created!<br/>";
        } catch (PersistenceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException && transactionRetry > 0) {
                SQLException e1 = new SQLException(cause);
                if (isNotDeadLock(e1)) {
                    session.rollback();
                }
                return createUser(userName, password, balance, --transactionRetry);
            } else {
                return "Internal error, try again later.<br/>";
            }
        }

    }
    public String book(int itineraryId, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class); // Connection not created yet
            Reservation r = recentSearch.get(itineraryId);
            Flight f1 = flightMapper.checkCapacity(r.getFid1());
            session.commit(); // Get capacity for the flight
            int f1Remained = f1.getCapacity() - flightMapper.checkBookedSeats1(r.getFid1());
            if (f1Remained <= 0) {
                session.commit();
                return "Booking failed, first itinerary no seats available<br/>";
            }
            int f2Remained;
            if (r.getFid2() != -1) {
                f2Remained = flightMapper.checkCapacity(r.getFid2()).getCapacity()
                        - flightMapper.checkBookedSeats2(r.getFid2());
                if (f2Remained <= 0) {
                    session.commit();
                    return "Booking failed, second itinerary no seats available<br/>";
                }
            }
            Integer currentFid1 = flightMapper.checkCurrentReservation(r.getUserName(), r.getDay());
            if (currentFid1 == null) { // No records found
                flightMapper.insert_reservation(r.getUserName(), r.getPaidOrNot(), r.getPrice(),
                        r.getDay(), r.getFid1(), r.getFid2());
                session.commit();
                return "Booked seat<br/>";
            } else {
                if (r.getFid2() == -1) { // No second itinerary needs to be added
                    session.commit();
                    return "Booking failed, you cannot book two flights in the same day<br/>";
                } else if (r.getFid1() == currentFid1) {
                    session.commit();
                    return "Booking failed, you cannot book two flights in the same day<br/>";
                } else { // Update the second itinerary
                    flightMapper.update_reservation(r.getFid2(), r.getUserName(), r.getFid1());
                    session.commit();
                    return "Booked seat on second itinerary<br/>";
                }
            }
        } catch (PersistenceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException && transactionRetry > 0) {
                SQLException e1 = new SQLException(cause);
                if (isNotDeadLock(e1)) {
                    session.rollback();
                }
                return book(itineraryId, --transactionRetry);
            } else {
                return e.toString();
            }
        }
    }
    public List<Reservation> view_reservations(String userName, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            List<Reservation> currentReservations = flightMapper.list_reservations(userName);
            session.commit();
            return currentReservations;
        } catch (PersistenceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException && transactionRetry > 0) {
                SQLException e1 = new SQLException(cause);
                if (isNotDeadLock(e1)) {
                    session.rollback();
                }
                return view_reservations(userName, --transactionRetry);
            } else {
                return null;
            }
        }
    }
    public String pay(String userName, int reservationId, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            Reservation r = flightMapper.retrieve_price(reservationId);
            session.commit();
            int balance = flightMapper.balance_check(userName);
            session.commit();
            int price = r.getPrice();
            int day = r.getDay();
            if (balance - price >= 0) {
                flightMapper.update_balance(userName, balance - price);
                session.commit();
                flightMapper.update_payment_status(userName, day);
                session.commit();
            } else {
                return "Failed to pay, not enough balance<br/>";
            }
            return "Paid for reservation: " + reservationId + "<br/>";
        } catch (PersistenceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException && transactionRetry > 0) {
                SQLException e1 = new SQLException(cause);
                if (isNotDeadLock(e1)) {
                    session.rollback();
                }
                return pay(userName, reservationId, --transactionRetry);
            } else {
                return "Failed to pay for reservation: " + reservationId + ", internal error<br/>";
            }
        }
    }
    public String cancel(String userName, int reservationId, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            Reservation r = flightMapper.retrieve_price(reservationId);
            session.commit();
            int price = r.getPrice();
            int day = r.getDay();
            flightMapper.cancel_reservation(userName, day);
            session.commit();
            flightMapper.user_refund(userName, price);
            session.commit();
            return "Canceled reservation: " + reservationId + "<br/>";
        } catch (PersistenceException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException && transactionRetry > 0) {
                SQLException e1 = new SQLException(cause);
                if (isNotDeadLock(e1)) {
                    session.rollback();
                }
                return cancel(userName, reservationId, --transactionRetry);
            } else {
                return "Failed to cancel reservation: " + reservationId + ", internal error<br/>";
            }
        }
    }
}
