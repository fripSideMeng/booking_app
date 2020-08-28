import java.util.*;
import java.sql.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;


public class Search extends HttpServlet {

	private static final String SEARCH_DIRECT = "SELECT TOP(?) fid, day_of_month, carrier_id, flight_num, "
			+ "origin_city, dest_city, actual_time, capacity, price FROM Flights WHERE origin_city = ? "
			+ "AND dest_city = ? AND day_of_month = ? AND actual_time > 0 ORDER BY actual_time ASC, fid ASC";
	private static final String SEARCH_INDIRECT = "SELECT TOP(?) f1.fid AS f1_fid, f1.day_of_month AS f1_day_of_month, "
			+ "f1.carrier_id AS f1_carrier_id, f1.flight_num AS f1_flight_num, f1.origin_city AS f1_origin_city, "
			+ "f1.dest_city AS f1_dest_city, f1.actual_time AS f1_actual_time, f1.capacity AS f1_capacity, f1.price AS f1_price, "
			+ "f2.fid AS f2_fid, f2.day_of_month AS f2_day_of_month, f2.carrier_id AS f2_carrier_id, "
			+ "f2.flight_num AS f2_flight_num, f2.origin_city AS f2_origin_city, "
			+ "f2.dest_city AS f2_dest_city, f2.actual_time AS f2_actual_time, f2.capacity AS f2_capacity, f2.price AS f2_price "
			+ "FROM Flights AS f1, Flights AS f2 WHERE f1.day_of_month = f2.day_of_month "
			+ "AND f1.origin_city = ? AND f2.dest_city = ? AND f1.day_of_month = ? "
			+ "AND f1.dest_city = f2.origin_city  AND f1.actual_time > 0 AND f2.actual_time > 0 "
			+ "ORDER BY (f1.actual_time + f2.actual_time) ASC, f1_fid ASC";

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		ArrayList<Integer> id = new ArrayList<Integer>();
		Map<Integer, Reservation> recentSearch = new HashMap<>();
		Connection conn = null;
		PreparedStatement userDirectSearchStatement = null;
		PreparedStatement userIndirectSearchStatement = null;

		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		HttpSession session = req.getSession(false);
		if (session == null || session.getAttribute("conn") == null) {
			pw.println("Connection refused");
			req.getRequestDispatcher("search.html").include(req, res);
		} else {
			conn = (Connection) session.getAttribute("conn");
		}
		try {
			conn.setAutoCommit(true);
			userDirectSearchStatement = conn.prepareStatement(SEARCH_DIRECT);
			userIndirectSearchStatement = conn.prepareStatement(SEARCH_INDIRECT);
		} catch (Exception e) {
			pw.println("Search not ready");
			req.getRequestDispatcher("search.html").include(req, res);
		}
		
		req.getRequestDispatcher("basic.html").include(req, res);

		pw.println("<!DOCTYPE html>");
		pw.println("<html>");
		pw.print(
				"<head>\n<meta charset='ISO-8859-1'>\n<title>Search Page</title><style>\nh1 {text-align:center;}\np {text-align: center;}\n</style>\n</head>\n");
	
		String origin = req.getParameter("origin");
		String dest = req.getParameter("dest");
		int dayOfMonth = Integer.parseInt(req.getParameter("day"));
		int numberOfItineraries = Integer.parseInt(req.getParameter("itineraries"));
		boolean directFlight = req.getParameter("directOrNot").equalsIgnoreCase("yes");

		try {
			Reservation r = (Reservation) session.getAttribute("reservation");
		} catch (NullPointerException e) {
			req.getRequestDispatcher("search.html").forward(req, res);
		}

		String result = transaction_search(origin, dest, directFlight, dayOfMonth, numberOfItineraries, id, recentSearch, 
						session, userDirectSearchStatement, userIndirectSearchStatement);

		session.setAttribute("recentSearch", recentSearch);

		pw.print("<body>\n<h1>Search Results</h1>\n<p>" + result + "</p>\n<hr/>");
		if (session.getAttribute("uname") != null) {
			pw.print("<form action='book' method='post'>\n");
			pw.println("<label for='itineraryId'>Book an itinerary: </label>");
			pw.println("<select id='itineraryId' name='itineraryId'>");
			for (int i = 0; i < id.size(); i++) {
				pw.println("<option value='" + id.get(i) + "'>" + id.get(i) + "</option>");
			}
			pw.println("</select>");
			pw.println("<input type='submit' value='Book'/><br/>\n<a href='search.html'>Looking for other flights?</a></form>\n</body>\n</html>");
		} else {
			pw.print("<a href='index.html'>Please Log in to search and book</a>\n</body>\n</html>");
		}

		pw.close();
		
	}

	public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
			int numberOfItineraries, ArrayList<Integer> id, Map<Integer, Reservation> recentSearch, HttpSession session, 
			PreparedStatement userDirectSearchStatement, PreparedStatement userIndirectSearchStatement) {
		
		Reservation r = (Reservation) session.getAttribute("reservation");

		StringBuffer sb = new StringBuffer();
		Flight f1 = (Flight) session.getAttribute("flight_info");

		try {
			if (directFlight) {
				userDirectSearchStatement.setInt(1, numberOfItineraries);
				userDirectSearchStatement.setString(2, originCity);
				userDirectSearchStatement.setString(3, destinationCity);
				userDirectSearchStatement.setInt(4, dayOfMonth);
				
				ResultSet zeroHopResults = userDirectSearchStatement.executeQuery();
				// userDirectSearchStatement.clearParameters();

				int size = 0;

				while (zeroHopResults.next()) {
					Reservation r1 = r.getNewReservation();

					f1.fid = zeroHopResults.getInt("fid");
					f1.infoSearch(f1.fid);

					r1.fid1 = f1.fid;
					r1.day = f1.dayOfMonth;
					r1.price = f1.price;

					id.add(size);
					sb.append("Itinerary " + size + ": 1 flight(s), " + f1.time + " minutes<br/>");
					sb.append(f1.toString() + "<br/>");

					if(session.getAttribute("uname") != null) 
						recentSearch.put(size, r1);

					size++;
				}

				if (size == 0) {
					return "No flights match your selection\n";
				}

				zeroHopResults.close();

			} else {
				userDirectSearchStatement.setInt(1, numberOfItineraries);
				userDirectSearchStatement.setString(2, originCity);
				userDirectSearchStatement.setString(3, destinationCity);
				userDirectSearchStatement.setInt(4, dayOfMonth);
				ResultSet zeroHopResultsCopy = userDirectSearchStatement.executeQuery();

				int size = 0;
				while (zeroHopResultsCopy.next()) {
					size++;
				}
				zeroHopResultsCopy.close();
				ResultSet zeroHopResults = userDirectSearchStatement.executeQuery();
				// userDirectSearchStatement.clearParameters();
				if (size == 0) {
					userIndirectSearchStatement.setInt(1, numberOfItineraries);
					userIndirectSearchStatement.setString(2, originCity);
					userIndirectSearchStatement.setString(3, destinationCity);
					userIndirectSearchStatement.setInt(4, dayOfMonth);
					ResultSet oneHopResults = userIndirectSearchStatement.executeQuery();
					// userIndirectSearchStatement.clearParameters();
					if (!oneHopResults.next()) {
						oneHopResults.close();
						return "No flights match your selection\n";
					}
				}

				int itineraryId = 0;
				if (size < numberOfItineraries) {
					userIndirectSearchStatement.setInt(1, numberOfItineraries);
					userIndirectSearchStatement.setString(2, originCity);
					userIndirectSearchStatement.setString(3, destinationCity);
					userIndirectSearchStatement.setInt(4, dayOfMonth);
					ResultSet oneHopResults = userIndirectSearchStatement.executeQuery();
					// userIndirectSearchStatement.clearParameters();
					if (!oneHopResults.next()) {
						while (zeroHopResults.next()) {
							Reservation r1 = new Reservation();
							f1.fid = zeroHopResults.getInt("fid");
							f1.infoSearch(f1.fid);

							r1.fid1 = f1.fid;
							r1.day = f1.dayOfMonth;
							r1.price = f1.price;

							id.add(size);
							sb.append("Itinerary " + size + ": 1 flight(s), " + f1.time + " minutes<br/>");
							sb.append(f1.toString() + "<br/>");
							
							if(session.getAttribute("uname") != null) 
								recentSearch.put(size, r1);

						}
						return sb.toString();
					}

					zeroHopResults.next();

					int singleTime = zeroHopResults.getInt("actual_time");
					int doubleTime = oneHopResults.getInt("f1_actual_time") + oneHopResults.getInt("f2_actual_time");
					while (!zeroHopResults.isAfterLast() && !oneHopResults.isAfterLast()) {
						Reservation r1 = r.getNewReservation();
						Flight f2_1 = f1.getNewFlight();
						Flight f2_2 = f1.getNewFlight();

						if (singleTime > doubleTime) {
							f2_1.fid = oneHopResults.getInt("f1_fid");
							f2_1.infoSearch(f2_1.fid);

							r1.fid1 = f2_1.fid;
							r1.day = f2_1.dayOfMonth;

							f2_2.fid = oneHopResults.getInt("f2_fid");
							f2_2.infoSearch(f2_2.fid);

							r1.fid2 = f2_2.fid;
							r1.price = f2_1.price + f2_2.price;

							id.add(itineraryId);
							sb.append("Itinerary " + itineraryId + ": 2 flight(s), " + (f2_1.time + f2_2.time)
									+ " minutes<br/>");
							sb.append(f2_1.toString() + "<br/>" + f2_2.toString() + "<br/>");

							if(session.getAttribute("uname") != null) 
								recentSearch.put(itineraryId, r1);

							itineraryId++;
							oneHopResults.next();
							if (!oneHopResults.isAfterLast())
								doubleTime = oneHopResults.getInt("f1_actual_time")
										+ oneHopResults.getInt("f2_actual_time");
						} else if (singleTime == doubleTime) {
							int singleId = zeroHopResults.getInt("fid");
							int doubleId = oneHopResults.getInt("f1_fid");

							if (doubleId < singleId) {
								f2_1.fid = oneHopResults.getInt("f1_fid");
								f2_1.infoSearch(f2_1.fid);

								r1.fid1 = f2_1.fid;
								r1.day = f2_1.dayOfMonth;

								f2_2.fid = oneHopResults.getInt("f2_fid");
								f2_2.infoSearch(f2_2.fid);

								r1.fid2 = f2_2.fid;
								r1.price = f2_1.price + f2_2.price;

								id.add(itineraryId);
								sb.append("Itinerary " + itineraryId + ": 2 flight(s), " + (f2_1.time + f2_2.time)
										+ " minutes<br/>");
								sb.append(f2_1.toString() + "<br/>" + f2_2.toString() + "<br/>");

								if(session.getAttribute("uname") != null) 
									recentSearch.put(itineraryId, r1);

								itineraryId++;
								oneHopResults.next();
								if (!oneHopResults.isAfterLast())
									doubleTime = oneHopResults.getInt("f1_actual_time")
											+ oneHopResults.getInt("f2_actual_time");
							} else {
								f1.fid = zeroHopResults.getInt("fid");
								f1.infoSearch(f1.fid);

								r1.fid1 = f1.fid;
								r1.day = f1.dayOfMonth;
								r1.price = f1.price;

								id.add(itineraryId);
								sb.append("Itinerary " + itineraryId + ": 1 flight(s), " + f1.time + " minutes<br/>");
								sb.append(f1.toString() + "<br/>");


								if(session.getAttribute("uname") != null) 
									recentSearch.put(itineraryId, r1);

								itineraryId++;
								zeroHopResults.next();
								if (!zeroHopResults.isAfterLast())
									singleTime = zeroHopResults.getInt("actual_time");
							}

						} else {
							f1.fid = zeroHopResults.getInt("fid");
							f1.infoSearch(f1.fid);

							r1.fid1 = f1.fid;
							r1.day = f1.dayOfMonth;
							r1.price = f1.price;

							id.add(itineraryId);
							sb.append("Itinerary " + itineraryId + ": 1 flight(s), " + f1.time + " minutes<br/>");
							sb.append(f1.toString() + "<br/>");


							if(session.getAttribute("uname") != null) 
								recentSearch.put(itineraryId, r1);

							itineraryId++;
							zeroHopResults.next();
							if (!zeroHopResults.isAfterLast())
								singleTime = zeroHopResults.getInt("actual_time");
						}
					}

					if (!zeroHopResults.isAfterLast()) {
						Reservation r1 = r.getNewReservation();

						f1.fid = zeroHopResults.getInt("fid");
						f1.infoSearch(f1.fid);

						r1.fid1 = f1.fid;
						r1.day = f1.dayOfMonth;
						r1.price = f1.price;

						id.add(itineraryId);
						sb.append("Itinerary " + itineraryId + ": 1 flight(s), " + f1.time + " minutes<br/>");
						sb.append(f1.toString() + "<br/>");

						if(session.getAttribute("uname") != null) 
							recentSearch.put(itineraryId, r1);

						itineraryId++;

						while (zeroHopResults.next()) {
							r1 = r.getNewReservation();
							f1.fid = zeroHopResults.getInt("fid");
							f1.infoSearch(f1.fid);

							r1.fid1 = f1.fid;
							r1.day = f1.dayOfMonth;
							r1.price = f1.price;

							id.add(itineraryId);
							sb.append("Itinerary " + itineraryId + ": 1 flight(s), " + f1.time + " minutes<br/>");
							sb.append(f1.toString() + "<br/>");

							if(session.getAttribute("uname") != null) 
								recentSearch.put(itineraryId, r1);

							itineraryId++;
						}
					} else if (!oneHopResults.isAfterLast()) {
						Reservation r1 = r.getNewReservation();
						Flight f2_1 = f1.getNewFlight();
						Flight f2_2 = f1.getNewFlight();

						f2_1.fid = oneHopResults.getInt("f1_fid");
						f2_1.infoSearch(f2_1.fid);

						r1.fid1 = f2_1.fid;
						r1.day = f2_1.dayOfMonth;

						f2_2.fid = oneHopResults.getInt("f2_fid");
						f2_2.infoSearch(f2_2.fid);

						r1.fid2 = f2_2.fid;
						r1.price = f2_1.price + f2_2.price;

						id.add(itineraryId);
						sb.append("Itinerary " + itineraryId + ": 2 flight(s), " + (f2_1.time + f2_2.time)
								+ " minutes<br/>");
						sb.append(f2_1.toString() + "<br/>" + f2_2.toString() + "<br/>");

						if(session.getAttribute("uname") != null) 
							recentSearch.put(itineraryId, r1);

						itineraryId++;
						while (oneHopResults.next()) {
							r1 = r.getNewReservation();
							f2_1 = f1.getNewFlight();
							f2_2 = f1.getNewFlight();
							f2_1.fid = oneHopResults.getInt("f1_fid");
							f2_1.infoSearch(f2_1.fid);

							r1.fid1 = f2_1.fid;
							r1.day = f2_1.dayOfMonth;

							f2_2.fid = oneHopResults.getInt("f2_fid");
							f2_2.infoSearch(f2_2.fid);

							r1.fid2 = f2_2.fid;
							r1.price = f2_1.price + f2_2.price;

							id.add(itineraryId);
							sb.append("Itinerary " + itineraryId + ": 2 flight(s), " + (f2_1.time + f2_2.time)
									+ " minutes<br/>");
							sb.append(f2_1.toString() + "<br/>" + f2_2.toString() + "<br/>");

							if(session.getAttribute("uname") != null) 
								recentSearch.put(itineraryId, r1);

							itineraryId++;
						}
					}

					oneHopResults.close();

				} else {
					while (zeroHopResults.next()) {
						Reservation r1 = r.getNewReservation();
						f1.fid = zeroHopResults.getInt("fid");
						f1.infoSearch(f1.fid);

						r1.fid1 = f1.fid;
						r1.day = f1.dayOfMonth;
						r1.price = f1.price;

						id.add(itineraryId);
						sb.append("Itinerary " + itineraryId + ": 1 flight(s), " + f1.time + " minutes<br/>");
						sb.append(f1.toString() + "<br/>");

						if(session.getAttribute("uname") != null) 
							recentSearch.put(itineraryId, r1);

						itineraryId++;
					}
				}

				zeroHopResults.close();

			}

		} catch (SQLException e) {
			return "Failed to search " + e.toString() + "\n";
		}

		if (recentSearch.isEmpty()) {
			sb.append("No search is recorded<br/>");
		}
		return sb.toString();
	}
	
	
}
