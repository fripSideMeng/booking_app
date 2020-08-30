import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class Cancel extends HttpServlet {

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


	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		req.getRequestDispatcher("basic.html").include(req, res);
		pw.print(
				"<!DOCTYPE html>\n<html>\n<head>\n<meta charset='ISO-8859-1'>\n<title>Cancellation Info</title>\n<style>\nh1 {text-align:center;}\np {text-align: center;}\n</style></head>\n");

		HttpSession session = req.getSession(false);
		Connection conn = (Connection) session.getAttribute("conn");
		String currentName = (String) session.getAttribute("uname");
		int reservationId = Integer.parseInt((String)req.getParameter("rid1"));

		pw.print("<body>\n<h1>Cancellation Status</h1>\n<p>" + transaction_cancel(reservationId, pw, currentName, conn, 0)
				+ "<hr/><a href='search.html'>Looking for more flights? </a><br/>");
		pw.print("<button onclick='goBack()'>Return to view page</button><br/>\n"
				+ "<script>function goBack() {\n window.history.back();\n }\n </script><br/>\n");

		pw.print("</p>\n</body>\n</html>");

	}

	public String transaction_cancel(int reservationId, PrintWriter pw, String currentName, Connection conn, int transactionRetry) {
		try {
			conn.setAutoCommit(false);
			
			ResultSet reservationInfo = conn.createStatement()
											.executeQuery("SELECT r.Username AS Username, r.Day AS Day, r.PaidOrNot AS PaidOrNot, r.Price AS Price "
															+ "FROM Reservations r, ID i WHERE i.ReservationID = " + reservationId 
															+ " AND r.Username = i.Username");
			
			if (!reservationInfo.next() || !reservationInfo.getString("Username").equals(currentName)) {
				reservationInfo.close();
				conn.commit();
				pw.println("<p>No reservation found or username is inconsistent</p>");
				return "Failed to cancel reservation " + reservationId + "\n";
			}
			
			int paidOrNot = reservationInfo.getInt("PaidOrNot");
			int day = reservationInfo.getInt("Day");

			if (paidOrNot == 1) { // Refund for user
				int price = reservationInfo.getInt("Price");
				reservationInfo.close();
				
				ResultSet userBalance = conn.createStatement().executeQuery("SELECT Balance FROM Users "
						+ "WHERE Username = '" + currentName + "'");
				userBalance.next();
				
				int refund = price + userBalance.getInt("Balance");
				userBalance.close();
				
				conn.createStatement().executeUpdate("UPDATE Users SET Balance = " + refund 
						+ " WHERE Username = '" + currentName + "'");
			}
			
			conn.createStatement().executeUpdate("DELETE FROM Reservations "
					+ "WHERE Username = '" + currentName + "' AND Day = " + day);
			conn.commit();
			return "Canceled reservation " + reservationId + "\n";
		} catch (SQLException e) {
			if (transactionRetry < 3) {
				transactionRetry++;
				try {			
					if (!isDeadLock(e)) {
						conn.rollback();
					}
				} catch (SQLException e1) {
					
				}
				return transaction_cancel(reservationId, pw, currentName, conn, transactionRetry);
			} else {
				return "Failed to cancel reservation " + reservationId + "\n";
			}			
		} finally {
			checkDanglingTransaction(conn);
		}
	}
}

