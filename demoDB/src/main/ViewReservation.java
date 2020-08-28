import java.io.PrintWriter;
import java.io.IOException;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;

public class ViewReservation extends HttpServlet {
	private static boolean isDeadLock(SQLException e) {
		return e.getErrorCode() == 1205;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		ArrayList<Integer> rid = new ArrayList<Integer>();

		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		HttpSession session = req.getSession(false);
		Connection conn = (Connection) session.getAttribute("conn");

		String currentName = (String) session.getAttribute("uname");

		req.getRequestDispatcher("basic.html").include(req, res);

		pw.println("<!DOCTYPE html>");
		pw.println("<html>");
		pw.print(
				"<head>\n<meta charset='ISO-8859-1'>\n<title>Booked Flights</title><style>\nh1 {text-align:center;}\np {text-align: center;}\n</style>\n</head>\n");

		pw.print("<body>\n<h1>Booked Flights</h1>\n<p>" + transaction_reservations(pw, rid, currentName, conn, 0) + "</p>\n<hr/>");
		try {
			conn.setAutoCommit(true);
		} catch (Exception e) {
			pw.println("<p>" + e.toString() + "</p>");
		}
		pw.print("<form action='pay' method='post'>");
		pw.println("<label for='rid'>Select one you want to pay: </label>");
		pw.println("<select id='rid' name='rid'>");
		for (int i = 0; i < rid.size(); i++) {
			pw.println("<option value='" + rid.get(i) + "'>" + rid.get(i) + "</option>");
		}
		pw.println("</select>");
		pw.println("<input type='submit' value='Pay'/>");
		pw.println("</form><br/>");
		pw.print("<form action='cancel' method='post'>");
		pw.println("<label for='rid1'>Select one you want to cancel: </label>");
		pw.println("<select id='rid1' name='rid1'>");
		for (int i = 0; i < rid.size(); i++) {
			pw.println("<option value='" + rid.get(i) + "'>" + rid.get(i) + "</option>");
		}
		pw.println("</select>");
		pw.println("<input type='submit' value='Cancel'/>");
		pw.println("</form>");
		rid.clear();
		pw.println("</body>\n</html>");
	}

	private void checkDanglingTransaction(Connection conn) {
		try {
			try (ResultSet rs = conn.prepareStatement("SELECT @@TRANCOUNT AS tran_count").executeQuery()) {
				rs.next();
				int count = rs.getInt("tran_count");
				if (count > 0) {
					throw new IllegalStateException(
							"Transaction not fully commit/rollback. Number of transaction in process: " + count);
				}
			} finally {
				conn.setAutoCommit(true);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Database error", e);
		}
	}

	public String infoSearch(int fid, Connection conn) {
		if (fid == -1) {
			return "";
		}
		try {
			ResultSet r = conn.createStatement().executeQuery("SELECT * FROM Flights WHERE fid = " + fid);
			r.next();

			int dayOfMonth = r.getInt("day_of_month");
			String carrierId = r.getString("carrier_id");
			String flightNum = r.getString("flight_num");
			String originCity = r.getString("origin_city");
			String destCity = r.getString("dest_city");
			int time = r.getInt("actual_time");
			int capacity = r.getInt("capacity");
			int price = r.getInt("price");

			r.close();
			return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: " + flightNum
					+ " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time + " Capacity: " + capacity
					+ " Price: " + price + "\n";

		} catch (SQLException e) {
			return "Search failed for flight id: " + fid;
		}

	}

	public String transaction_reservations(PrintWriter pw, ArrayList<Integer> rid, String currentName, Connection conn, int transactionRetry) {
		try {

			conn.setAutoCommit(false);
			ResultSet currentReservations = conn.createStatement()
					.executeQuery("SELECT r.Fid1 AS Fid1, r.Fid2 AS Fid2, r.PaidOrNot AS PaidOrNot, i.ReservationID AS ReservationID "
							+ "FROM Reservations r, ID i WHERE r.Username = '" + currentName + "' AND r.Username = i.Username AND r.Day = i.Day");
			if (!currentReservations.next()) {
				conn.commit();
				currentReservations.close();
				return "No reservations found\n";
			} else {
				StringBuffer sb = new StringBuffer();

				boolean paidOrNot = (currentReservations.getInt("PaidOrNot") == 1);
				rid.add(currentReservations.getInt("ReservationID"));
				sb.append("Reservation " + currentReservations.getInt("ReservationID") + " paid: " + paidOrNot
						+ "<br/>");
				sb.append(infoSearch(currentReservations.getInt("Fid1"), conn) + "<br/>"
						+ infoSearch(currentReservations.getInt("Fid2"), conn) + "<br/>");

				while (currentReservations.next()) {
					paidOrNot = (currentReservations.getInt("PaidOrNot") == 1);
					rid.add(currentReservations.getInt("ReservationID"));
					sb.append("Reservation " + currentReservations.getInt("ReservationID") + " paid: " + paidOrNot
							+ "<br/>");
					sb.append(infoSearch(currentReservations.getInt("Fid1"), conn) + "<br/>"
							+ infoSearch(currentReservations.getInt("Fid2"), conn) + "<br/>");
				}
				conn.commit();
				currentReservations.close();
				return sb.toString();
			}

		} catch (SQLException e) {
			if (transactionRetry < 3) {
				transactionRetry++;
				if (!isDeadLock(e)) {
					try {
						conn.rollback();
					} catch (SQLException e1) {
						pw.println("<p>" + e1.toString() + "</p>");
					}
				}
				return transaction_reservations(pw, rid, currentName, conn, transactionRetry);
			} else {
				return "Failed to retrieve reservations " + e.toString();
			}
		} finally {
			try {
				checkDanglingTransaction(conn);
			} catch (Exception e) {
				
			}
		}
	}

}
