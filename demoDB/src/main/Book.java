import java.util.*;
import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class Book extends HttpServlet {
	private static final String CHECK_FLIGHT_CAPACITY = "SELECT capacity FROM Flights WHERE fid = ?";
	private static final String SEARCH_DAY_FID = "SELECT day_of_month FROM Flights WHERE fid = ?";
	private PreparedStatement checkFlightCapacityStatement;

	private static boolean isDeadLock(SQLException ex) {
		return ex.getErrorCode() == 1205;
	}

	private void checkDanglingTransaction(Connection conn) {
		try (Statement statement = conn.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(
					"SHOW PROCESSLIST")) {
				int count = 0;
				while (resultSet.next()) {
					String state = resultSet.getString( "command" );
					if ("sleep".equalsIgnoreCase( state )) {
						count++;
					}
				}
				if (count > 0) {
					throw new IllegalStateException(
							"Transaction not fully commit/rollback. Number of transaction in process: " + count);
				}
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}


	private int checkFlightCapacity(int fid, Connection conn) throws SQLException {
		ResultSet results = conn.createStatement().executeQuery("SELECT capacity FROM Flights WHERE fid = " + fid);
		results.next();
		int capacity = results.getInt("capacity");
		results.close();
		return capacity;
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		HttpSession session = req.getSession(false);
		Connection conn = (Connection) session.getAttribute("conn");
		String currentName = (String) session.getAttribute("uname");
		Map<Integer, Reservation> recentSearch = castToMap(session);
		try {
			checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
		} catch (SQLException e) {
			pw.println("Check capacity failed");
		}

		req.getRequestDispatcher("basic.html").include(req, res);
		pw.print(
				"<!DOCTYPE html>\n<html>\n<head>\n<meta charset='ISO-8859-1'>\n<title>Booking Info</title>\n<style>\nh1 {text-align:center;}\np {text-align: center;}\n</style></head>\n");
		int itineraryId = 0;
		if (session.getAttribute("itineraryId") != null) {
			try {
				itineraryId = Integer.parseInt((String)session.getAttribute("itineraryId"));
			} catch (Exception e) {
				pw.println("<button onclick='goBack()'>Please re-input</button>\n"
						+ "<script>function goBack() {\n window.history.back();\n }\n </script>\n");
				pw.print("</html>");
			}
		} else {
			pw.println("No itinerary selected. Please search for flights first.");
			req.getRequestDispatcher("search.html").include(req, res);
			return;
		}

		Reservation r = recentSearch.get(itineraryId);		

		pw.print("<body>\n<h1>Booking Status</h1>\n<p>" + transaction_book(itineraryId, pw, r, currentName, conn, 0)
				+ "<hr/><a href='search.html'>Looking for more flights? </a>");
		pw.print("<a href='view'> View Booked Flights</a><br/>\n");
		pw.print("<button onclick='goBack()'>Continue Booking</button>\n"
				+ "<script>function goBack() {\n window.history.back();\n }\n </script><br/>\n");
		
		pw.print("</p>\n</body>\n</html>");

		pw.close();
	}

	@SuppressWarnings("unchecked")
	private HashMap<Integer, Reservation> castToMap(HttpSession session) {
		return (HashMap<Integer, Reservation>) session.getAttribute("recentSearch");
	}

	public String transaction_book(int itineraryId, PrintWriter pw, Reservation r, String currentName, Connection conn, int transactionRetry) {
		try {

			conn.setAutoCommit(false);
			if (r != null) {
				int f1Remained = 0;
				int f2Remained = 0;
				
				ResultSet temp = conn.createStatement().executeQuery("SELECT day_of_month FROM Flights WHERE fid = " + r.fid1);	
				conn.commit();
				temp.next();
				int day = temp.getInt("day_of_month");
				temp.close();		

				ResultSet total1 = conn.createStatement()
						.executeQuery("SELECT COUNT(*) AS fid1_total FROM Reservations " + "WHERE Fid1 = " + r.fid1);
				total1.next();
				f1Remained = checkFlightCapacity(r.fid1, conn) - total1.getInt("fid1_total");
				total1.close();

				if (r.fid2 != -1) {
					ResultSet total2 = conn.createStatement().executeQuery(
							"SELECT COUNT(*) AS fid2_total FROM Reservations " + "WHERE Fid2 = " + r.fid2);
					total2.next();
					f2Remained = checkFlightCapacity(r.fid2, conn) - total2.getInt("fid2_total");
					total2.close();
				}

				if (f1Remained <= 0) {
					conn.commit();
					return "Booking failed, first flight no seats available\n";
				} else if (r.fid2 != -1 && f2Remained <= 0) {
					conn.commit();
					return "Booking failed, second flight no seats available\n";
				}

				ResultSet rs = conn.createStatement().executeQuery("SELECT Fid1, Fid2 FROM Reservations WHERE Username = '" + currentName 
									+ "' AND Day = " + day);
				if (!rs.next()) {
					conn.createStatement()
							.executeUpdate("INSERT INTO Reservations "
									+ "VALUES ('" + currentName + "', " + r.fid1 + ", " + r.fid2 + ", " + r.day + ", "
									+ r.price + ", " + r.paidOrNot + ")");
					conn.commit();					
					rs.close();
				} else {
					int fid1_booked = rs.getInt("Fid1");
					int fid2_booked = rs.getInt("Fid2");
					if (fid1_booked != r.fid1) {
						conn.commit();
						return "You cannot book two flights in the same day\n";
					} else if (fid2_booked == -1) {
						if (r.fid2 == -1) {
							conn.commit();
							return "You cannot book two flights in the same day\n";
						}

						ResultSet result1 = conn.createStatement().executeQuery(
								"SELECT origin_city, dest_city FROM Flights " + "WHERE fid = " + fid1_booked);		
						conn.commit();			

						ResultSet result2 = conn.createStatement()
								.executeQuery("SELECT origin_city, dest_city FROM Flights " + "WHERE fid = " + r.fid2);										
						conn.commit();

						result1.next();	
						String dest = result1.getString("dest_city");
						
						result2.next();
						String origin = result2.getString("origin_city");

						if (dest.equals(origin)) {
							conn.createStatement()
									.execute("UPDATE Reservations SET Fid2 = " + r.fid2 + ", Price = " + r.price
											+ ", PaidOrNot = 1 " + "WHERE Username = '" + r.userName + "' AND Fid1 = "
											+ r.fid1);
							conn.commit();
							result1.close();
							result2.close();
							rs.close();
						} else {
							conn.commit();
							result1.close();
							result2.close();
							rs.close();
							return "Booking failed, two flights not consecutive\n";
						}
					} else {
						conn.commit();
						return "Booking failed, same flight booked twice\n";
					}

				}
			} else {
				return "No such itinerary " + itineraryId + "\n";
			}
			return "Flight(s) Booked";

		} catch (SQLException e) {
			if (transactionRetry < 3) {
				transactionRetry++;
				if (!isDeadLock(e)) {
					try {
						conn.rollback();
					} catch (SQLException e1) {
						
					}				
				} 
				return transaction_book(itineraryId, pw, r, currentName, conn, transactionRetry);
			} else {
				return "Booking failed";
			}

		} finally {
			try {
				checkDanglingTransaction(conn);
			} catch (Exception e) {
				
			}
		}
	}

}
