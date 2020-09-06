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

@Alias("Query")
public class Query {
    private static final int HASH_STRENGTH = 65536;
    private static final int KEY_LENGTH = 128;
    private User currentUser;
    private Map<Integer, Reservation> recentSearch;
    private final SqlSession session;

    private static boolean isNotDeadLock(SQLException e) {
        return e.getErrorCode() != 1205;
    }

    public String getCurrentUserName() {
        return currentUser != null ? currentUser.getUserName() : "Not logged in";
    }

    public Query(SqlSession session) {
        this.session = session;
        this.recentSearch = new HashMap<>();
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
            return transactionRetry > 0 ?
                    login(userName, password, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    login(userName, password, --transactionRetry) : "Internal error, try again later.<br/>";
        }
    }
    public String search(String originCity, String destCity,
                         int dayOfMonth, int numOfFlights, boolean indirect) {
        StringBuilder sb = new StringBuilder();
        FlightMapper flightMapper = session.getMapper(FlightMapper.class);
        List<Flight> flights = flightMapper.transaction_search(originCity, destCity, dayOfMonth, numOfFlights);
        session.commit();
        int itineraryId = 1;
        sb.append("<Flights>\n");
        if (flights == null || flights.size() == 0) {
            sb.append("  <Flight>\n");
            sb.append("    <day>").append("</day>\n");
            sb.append("    <carrier>").append("</carrier>\n");
            sb.append("    <number>").append("</number>\n");
            sb.append("    <origin>").append("</origin>\n");
            sb.append("    <dest>").append("</dest>\n");
            sb.append("    <capacity>").append("</capacity>\n");
            sb.append("    <time>").append("</time>\n");
            sb.append("    <price>").append("</price>\n");
            sb.append("    <book>").append("</book>\n");
            sb.append("  </Flight>\n");
            if (!indirect) { // Turn to indirect since no direct ones found
                indirect = true;
            }
        }
        if (!indirect || flights.size() >= numOfFlights) {
            for (Flight f : flights) {
                sb.append("  <Flight>\n");
                sb.append("    <day>").append(f.getDayOfMonth()).append("</day>\n");
                sb.append("    <carrier>").append(f.getCarrierId()).append("</carrier>\n");
                sb.append("    <number>").append(f.getFlightNum()).append("</number>\n");
                sb.append("    <origin>").append(f.getOriginCity()).append("</origin>\n");
                sb.append("    <dest>").append(f.getDestCity()).append("</dest>\n");
                sb.append("    <capacity>").append(f.getCapacity()).append("</capacity>\n");
                sb.append("    <time>").append(f.getTime()).append("</time>\n");
                sb.append("    <price>").append(f.getPrice()).append("</price>\n");
                if (currentUser != null) {
                    sb.append("    <book>").append("<a href='book?iid=")
                            .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                            "Book this flight</div></a></book>\n");
                    recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                            f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                    itineraryId++;
                } else {
                    sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                            "Log in to book this flight</div></a></book>\n");
                }
                sb.append("  </Flight>\n");
            }
        } else {
            List<TwoFlights> indirectFlights =
                    flightMapper.transaction_search_indirect(originCity, destCity, dayOfMonth,
                            numOfFlights - flights.size());
            session.commit();
            if (indirectFlights == null || indirectFlights.size() == 0) { // Search is finished
                sb.append("  <Flight>\n");
                sb.append("    <day>").append("</day>\n");
                sb.append("    <carrier>").append("</carrier>\n");
                sb.append("    <number>").append("</number>\n");
                sb.append("    <origin>").append("</origin>\n");
                sb.append("    <dest>").append("</dest>\n");
                sb.append("    <capacity>").append("</capacity>\n");
                sb.append("    <time>").append("</time>\n");
                sb.append("    <price>").append("</price>\n");
                sb.append("    <book>").append("</book>\n");
                sb.append("  </Flight>\n");
                return sb.append("</Flights>\n").toString();
            }
            int index1 = 0;
            int index2 = 0;
            while (index1 < flights.size() && index2 < indirectFlights.size()) {
                Flight f = flights.get(index1);
                int singleTime = f.getTime();
                TwoFlights ff = indirectFlights.get(index2);
                int doubleTime = ff.totalTime();
                if (singleTime < doubleTime) {
                    sb.append("  <Flight>\n");
                    sb.append("    <day>").append(f.getDayOfMonth()).append("</day>\n");
                    sb.append("    <carrier>").append(f.getCarrierId()).append("</carrier>\n");
                    sb.append("    <number>").append(f.getFlightNum()).append("</number>\n");
                    sb.append("    <origin>").append(f.getOriginCity()).append("</origin>\n");
                    sb.append("    <dest>").append(f.getDestCity()).append("</dest>\n");
                    sb.append("    <capacity>").append(f.getCapacity()).append("</capacity>\n");
                    sb.append("    <time>").append(f.getTime()).append("</time>\n");
                    sb.append("    <price>").append(f.getPrice()).append("</price>\n");
                    if (currentUser != null) {
                        sb.append("    <book>").append("<a href='book?iid=")
                                .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                                "Book this flight</div></a></book>\n");
                        recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                        itineraryId++;
                    } else {
                        sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                                "Log in to book this flight</div></a></book>\n");
                    }
                    sb.append("  </Flight>\n");
                    index1++;
                } else if (singleTime == doubleTime) { // break tie on fid
                    if (flights.get(index1).getFid() < indirectFlights.get(index2).getF1_fid()) {
                        sb.append("  <Flight>\n");
                        sb.append("    <day>").append(f.getDayOfMonth()).append("</day>\n");
                        sb.append("    <carrier>").append(f.getCarrierId()).append("</carrier>\n");
                        sb.append("    <number>").append(f.getFlightNum()).append("</number>\n");
                        sb.append("    <origin>").append(f.getOriginCity()).append("</origin>\n");
                        sb.append("    <dest>").append(f.getDestCity()).append("</dest>\n");
                        sb.append("    <capacity>").append(f.getCapacity()).append("</capacity>\n");
                        sb.append("    <time>").append(f.getTime()).append("</time>\n");
                        sb.append("    <price>").append(f.getPrice()).append("</price>\n");
                        if (currentUser != null) {
                            sb.append("    <book>").append("<a href='book?iid=")
                                    .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                                    "Book this flight</div></a></book>\n");
                            recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                    f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                            itineraryId++;
                        } else {
                            sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                                    "Log in to book this flight</div></a></book>\n");
                        }
                        sb.append("  </Flight>\n");
                        index1++;
                    } else {
                        Flight f1 = ff.getItinerary1();
                        Flight f2 = ff.getItinerary2();
                        sb.append("  <Flight>\n");
                        sb.append("    <day>").append(f1.getDayOfMonth()).append("</day>\n");
                        sb.append("    <carrier>").append(f1.getCarrierId()).append("|")
                                .append(f2.getCarrierId()).append("</carrier>\n");
                        sb.append("    <number>").append(f1.getFlightNum()).append("|")
                                .append(f2.getFlightNum()).append("</number>\n");
                        sb.append("    <origin>").append(f1.getOriginCity()).append("|")
                                .append(f2.getOriginCity()).append("</origin>\n");
                        sb.append("    <dest>").append(f1.getDestCity()).append("|")
                                .append(f2.getDestCity()).append("</dest>\n");
                        sb.append("    <capacity>").append(f1.getCapacity()).append("|")
                                .append(f2.getCapacity()).append("</capacity>\n");
                        sb.append("    <time>").append(f1.getTime()).append("|")
                                .append(f2.getTime()).append("</time>\n");
                        sb.append("    <price>").append(ff.getPrice()).append("</price>\n");
                        if (currentUser != null) {
                            sb.append("    <book>").append("<a href='book?iid=")
                                    .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                                    "Book this flight</div></a></book>\n");
                            recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                    ff.getF1_fid(), ff.getF2_fid(), ff.getDayOfMonth(), ff.getPrice(), 0));
                            itineraryId++;
                        } else {
                            sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                                    "Log in to book this flight</div></a></book>\n");
                        }
                        sb.append("  </Flight>\n");
                        index2++;
                    }
                } else {
                    Flight f1 = ff.getItinerary1();
                    Flight f2 = ff.getItinerary2();
                    sb.append("  <Flight>\n");
                    sb.append("    <day>").append(f1.getDayOfMonth()).append("</day>\n");
                    sb.append("    <carrier>").append(f1.getCarrierId()).append("|")
                            .append(f2.getCarrierId()).append("</carrier>\n");
                    sb.append("    <number>").append(f1.getFlightNum()).append("|")
                            .append(f2.getFlightNum()).append("</number>\n");
                    sb.append("    <origin>").append(f1.getOriginCity()).append("|")
                            .append(f2.getOriginCity()).append("</origin>\n");
                    sb.append("    <dest>").append(f1.getDestCity()).append("|")
                            .append(f2.getDestCity()).append("</dest>\n");
                    sb.append("    <capacity>").append(f1.getCapacity()).append("|")
                            .append(f2.getCapacity()).append("</capacity>\n");
                    sb.append("    <time>").append(f1.getTime()).append("|")
                            .append(f2.getTime()).append("</time>\n");
                    sb.append("    <price>").append(ff.getPrice()).append("</price>\n");
                    if (currentUser != null) {
                        sb.append("    <book>").append("<a href='book?iid=")
                                .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                                "Book this flight</div></a></book>\n");
                        recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                                ff.getF1_fid(), ff.getF2_fid(), ff.getDayOfMonth(), ff.getPrice(), 0));
                        itineraryId++;
                    } else {
                        sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                                "Log in to book this flight</div></a></book>\n");
                    }
                    sb.append("  </Flight>\n");
                    index2++;
                }
            }
            while (index1 < flights.size()) {
                Flight f = flights.get(index1);
                sb.append("  <Flight>\n");
                sb.append("    <day>").append(f.getDayOfMonth()).append("</day>\n");
                sb.append("    <carrier>").append(f.getCarrierId()).append("</carrier>\n");
                sb.append("    <number>").append(f.getFlightNum()).append("</number>\n");
                sb.append("    <origin>").append(f.getOriginCity()).append("</origin>\n");
                sb.append("    <dest>").append(f.getDestCity()).append("</dest>\n");
                sb.append("    <capacity>").append(f.getCapacity()).append("</capacity>\n");
                sb.append("    <time>").append(f.getTime()).append("</time>\n");
                sb.append("    <price>").append(f.getPrice()).append("</price>\n");
                if (currentUser != null) {
                    sb.append("    <book>").append("<a href='book?iid=")
                            .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                            "Book this flight</div></a></book>\n");
                    recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                            f.getFid(), -1, f.getDayOfMonth(), f.getPrice(), 0));
                    itineraryId++;
                } else {
                    sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                            "Log in to book this flight</div></a></book>\n");
                }
                sb.append("  </Flight>\n");
                index1++;
            }
            while (index2 < indirectFlights.size()) {
                TwoFlights ff = indirectFlights.get(index2);
                Flight f1 = ff.getItinerary1();
                Flight f2 = ff.getItinerary2();
                sb.append("  <Flight>\n");
                sb.append("    <day>").append(f1.getDayOfMonth()).append("</day>\n");
                sb.append("    <carrier>").append(f1.getCarrierId()).append("|")
                        .append(f2.getCarrierId()).append("</carrier>\n");
                sb.append("    <number>").append(f1.getFlightNum()).append("|")
                        .append(f2.getFlightNum()).append("</number>\n");
                sb.append("    <origin>").append(f1.getOriginCity()).append("|")
                        .append(f2.getOriginCity()).append("</origin>\n");
                sb.append("    <dest>").append(f1.getDestCity()).append("|")
                        .append(f2.getDestCity()).append("</dest>\n");
                sb.append("    <capacity>").append(f1.getCapacity()).append("|")
                        .append(f2.getCapacity()).append("</capacity>\n");
                sb.append("    <time>").append(f1.getTime()).append("|")
                        .append(f2.getTime()).append("</time>\n");
                sb.append("    <price>").append(ff.getPrice()).append("</price>\n");
                if (currentUser != null) {
                    sb.append("    <book>").append("<a href='book?iid=")
                            .append(itineraryId).append("'><div style='height:100%;width:100%'>" +
                            "Book this flight</div></a></book>\n");
                    recentSearch.put(itineraryId, new Reservation(-1, currentUser.getUserName(),
                            ff.getF1_fid(), ff.getF2_fid(), ff.getDayOfMonth(), ff.getPrice(), 0));
                    itineraryId++;
                } else {
                    sb.append("    <book><a href='index.html'><div style='height:100%;width:100%'>" +
                            "Log in to book this flight</div></a></book>\n");
                }
                sb.append("  </Flight>\n");
                index2++;
            }
        }
        sb.append("</Flights>\n");
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
            return transactionRetry > 0 ?
                    createUser(userName, password, balance, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    createUser(userName, password, balance, --transactionRetry) : "Internal error, try again later.<br/>";
        }

    }
    public String book(int itineraryId, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class); // Connection not created yet
            Reservation r = recentSearch.get(itineraryId);
            if (r == null) {
                return "No such itinerary";
            }
            Flight f1 = flightMapper.checkCapacity(r.getFid1());
            session.commit(); // Get capacity for the flight
            int f1Remained = f1.getCapacity() - flightMapper.checkBookedSeats1(r.getFid1());
            if (f1Remained <= 0) {
                session.commit();
                return "Booking failed, first itinerary no seats available";
            }
            int f2Remained;
            if (r.getFid2() != -1) {
                f2Remained = flightMapper.checkCapacity(r.getFid2()).getCapacity()
                        - flightMapper.checkBookedSeats2(r.getFid2());
                if (f2Remained <= 0) {
                    session.commit();
                    return "Booking failed, second itinerary no seats available";
                }
            }
            Integer currentFid1 = flightMapper.checkCurrentReservation(r.getUserName(), r.getDay());
            if (currentFid1 == null) { // No records found
                flightMapper.insert_reservation(r.getUserName(), r.getPaidOrNot(), r.getPrice(),
                        r.getDay(), r.getFid1(), r.getFid2());
                session.commit();
                return "Booked seat";
            } else {
                if (r.getFid2() == -1) { // No second itinerary needs to be added
                    session.commit();
                    return "Booking failed, you cannot book two flights in the same day";
                } else if (r.getFid1() == currentFid1) {
                    session.commit();
                    return "Booking failed, you cannot book two flights in the same day";
                } else { // Update the second itinerary
                    flightMapper.update_reservation(r.getFid2(), r.getUserName(), r.getFid1());
                    session.commit();
                    return "Booked seat on second itinerary";
                }
            }
        } catch (PersistenceException e) {
            return transactionRetry > 0 ?
                    book(itineraryId, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    book(itineraryId, --transactionRetry) : "Internal error, try again later.<br/>";
        }
    }
    public String view_reservations(String userName, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            List<Reservation> currentReservations = flightMapper.list_reservations(userName);
            session.commit();
            StringBuilder sb = new StringBuilder();
            if (currentReservations == null || currentReservations.size() == 0) {
                sb.append("  <Reservation>\n");
                sb.append("    <rid>").append("</rid>\n");
                sb.append("    <paidOrNot>").append("</paidOrNot>\n");
                sb.append("    <day>").append("</day>\n");
                sb.append("    <price>").append("</price>\n");
                sb.append("    <origin>").append("</origin>\n");
                sb.append("    <dest>").append("</dest>\n");
                sb.append("    <pay>").append("</pay>\n");
                sb.append("    <cancel>").append("</cancel>\n");
                sb.append("  </Reservation>\n");
                return sb.toString();
            }
            for (Reservation r : currentReservations) {
                Flight f1 = flightMapper.get_origin_dest(r.getFid1());
                Flight f2 = flightMapper.get_origin_dest(r.getFid2());
                String originCity = f1.getOriginCity();
                String destCity = f2 == null ? f1.getDestCity() : f2.getDestCity();
                sb.append("  <Reservation>\n");
                sb.append("    <rid>").append(r.getReservationId()).append("</rid>\n");
                sb.append("    <paidOrNot>").append(r.getPaidOrNot() == 1 ? "Paid" : "Not paid")
                        .append("</paidOrNot>\n");
                sb.append("    <day>").append(r.getDay()).append("</day>\n");
                sb.append("    <price>").append(r.getPrice()).append("</price>\n");
                sb.append("    <origin>").append(originCity).append("</origin>\n");
                sb.append("    <dest>").append(destCity).append("</dest>\n");
                sb.append("    <pay>").append("<a href='pay?rid=")
                        .append(r.getReservationId()).append("'><div style='height:100%;width:100%'>" +
                        "Pay for this reservation</div></a></pay>\n");
                sb.append("    <cancel>").append("<a href='cancel?rid=")
                        .append(r.getReservationId()).append("'><div style='height:100%;width:100%'>" +
                        "Cancel this flight</div></a></cancel>\n");
                sb.append("  </Reservation>\n");
            }
            return sb.toString();
        } catch (PersistenceException e) {
            return transactionRetry > 0 ?
                    view_reservations(userName, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    view_reservations(userName, --transactionRetry) : "Internal error, try again later.<br/>";
        }
    }
    public String pay(String userName, int reservationId, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            Reservation r = flightMapper.retrieve_price(reservationId);
            session.commit();
            if (r == null) {
                return "Failed to pay, no such reservation.<br/>";
            }
            int balance = flightMapper.balance_check(userName);
            session.commit();
            int price = r.getPrice();
            int day = r.getDay();
            if (r.getPaidOrNot() == 1) {
                return "Already paid for this flight.<br/>";
            }
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
            return transactionRetry > 0 ?
                    pay(userName, reservationId, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    pay(userName, reservationId, --transactionRetry) : "Internal error, try again later.<br/>";
        }
    }
    public String view_balance(String userName, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            int balance = flightMapper.balance_check(userName);
            return "Current balance: " + balance + "<br/>";
        } catch (PersistenceException e) {
            return transactionRetry > 0 ?
                    view_balance(userName, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    view_balance(userName, --transactionRetry) : "Internal error, try again later.<br/>";
        }
    }
    public String cancel(String userName, int reservationId, int transactionRetry) {
        try {
            FlightMapper flightMapper = session.getMapper(FlightMapper.class);
            Reservation r = flightMapper.retrieve_price(reservationId);
            session.commit();
            if (r == null) {
                return "Failed to cancel, no such reservation<br/>";
            }
            int price = r.getPrice();
            int day = r.getDay();
            flightMapper.cancel_reservation(userName, day);
            session.commit();
            flightMapper.user_refund(userName, price);
            session.commit();
            return "Canceled reservation: " + reservationId + "<br/>";
        } catch (PersistenceException e) {
            return transactionRetry > 0 ?
                    cancel(userName, reservationId, --transactionRetry) : "Internal error, try again later.<br/>";
        } catch (SQLException e1) {
            if (isNotDeadLock(e1)) {
                session.rollback();
            }
            return transactionRetry > 0 ?
                    cancel(userName, reservationId, --transactionRetry) : "Internal error, try again later.<br/>";
        }
    }
}
