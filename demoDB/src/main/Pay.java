import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;

public class Pay extends HttpServlet {
	
	private static boolean isDeadLock(SQLException e) {
		return e.getErrorCode() == 1205;
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
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		req.getRequestDispatcher("basic.html").include(req, res);
		pw.print(
				"<!DOCTYPE html>\n<html>\n<head>\n<meta charset='ISO-8859-1'>\n<title>Payment Info</title>\n<style>\nh1 {text-align:center;}\np {text-align: center;}\n</style></head>\n");

		HttpSession session = req.getSession(false);
		if (session == null || session.getAttribute("uname") == null || session.getAttribute("conn") == null) {
			pw.println("Please log in first");
			req.getRequestDispatcher("index.html").include(req, res);
			return;
		} else if (req.getParameter("rid") == null) {
			pw.println("<button onclick='goBack()'>Please re-input</button>\n"
					+ "<script>function goBack() {\n window.history.back();\n }\n </script>\n");
			pw.print("</html>");
			return;
		}
		Connection conn = (Connection) session.getAttribute("conn");
		String currentName = (String) session.getAttribute("uname");
		int reservationId = Integer.parseInt((String)req.getParameter("rid"));
		pw.print("<body>\n<h1>Payment Status</h1>\n<p>" + transaction_pay(reservationId, pw, currentName, conn, 0)
				+ "<hr/><a href='search.html'>Looking for more flights? </a><br/>");
		pw.print("<button onclick='goBack()'>Pay for another flight</button><br/>\n"
				+ "<script>function goBack() {\n window.history.back();\n }\n </script><br/>\n");

		pw.print("</p>\n</body>\n</html>");

	}

	public String transaction_pay(int reservationId, PrintWriter pw, String currentName, Connection conn, int transactionRetry) {
		try {

			conn.setAutoCommit(false);

			ResultSet infoReservation = conn.createStatement().executeQuery(
					"SELECT r.Username AS Username, r.PaidOrNot AS PaidOrNot, r.Price AS Price, r.Day AS Day FROM " 
					+ "Reservations r, ID i WHERE i.ReservationID = " 
					+ reservationId + " AND i.Username = r.Username AND i.Day = r.Day");
			if (!infoReservation.next() || infoReservation.getInt("PaidOrNot") == 1) {
				conn.commit();
				infoReservation.close();
				return "Cannot find unpaid reservation " + reservationId + " under user: " + currentName + "\n";
			}

			ResultSet balanceCheck = conn.createStatement()
					.executeQuery("SELECT U.Balance AS Balance, R.Price AS Price "
							+ "FROM Users AS U, Reservations AS R WHERE U.Username = R.Username AND U.Username = '"
							+ currentName + "'");
			balanceCheck.next();
			int balance = balanceCheck.getInt("Balance");
			int price = balanceCheck.getInt("Price");

			if (balance - price < 0) {
				conn.commit();
				infoReservation.close();
				balanceCheck.close();
				return "User has only " + balance + " in account but itinerary costs " + price;
			} else {
				conn.createStatement().executeUpdate(
						"UPDATE Users SET BALANCE = " + (balance - price) + " WHERE Username = '" + currentName + "'");

				conn.createStatement().executeUpdate(
						"UPDATE Reservations SET PaidOrNot = 1 " + "WHERE Username = '" + currentName + "' AND Day = " + infoReservation.getInt("Day"));
				conn.commit();
				balanceCheck.close();
				infoReservation.close();
				return "Paid reservation: " + reservationId + " remaining balance: " + (balance - price);
			}

		} catch (SQLException e) {
			pw.println("<p>" + e.toString() + "</p>");
			if (transactionRetry < 3) {
				transactionRetry++;
				if (!isDeadLock(e)) {
					try {
						conn.rollback();
					} catch (SQLException e1) {
						
					}
				}
				return transaction_pay(reservationId, pw, currentName, conn, transactionRetry);
			} else {
				transactionRetry = 0;
				pw.println("<p>" + e.toString() + "</p>");
				return "Failed to pay for reservation " + reservationId;
			}
		} finally {
			try {
				checkDanglingTransaction(conn);
			} catch (Exception e) {
		
			}
		}
	}

}
