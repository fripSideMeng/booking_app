import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class ViewBalance extends HttpServlet {
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
		
		String currentName = null;
		Connection conn = null;
		HttpSession session = req.getSession(false);
		if(session == null || session.getAttribute("uname") == null || session.getAttribute("conn") == null) {
			pw.println("Please log in to view balance");
			req.getRequestDispatcher("index.html").include(req, res);
			return;
		} else {
			conn = (Connection) session.getAttribute("conn");
			currentName = (String) session.getAttribute("uname");
		}

		req.getRequestDispatcher("basic.html").include(req, res);
		pw.print(
				"<!DOCTYPE html>\n<html>\n<head>\n<meta charset='ISO-8859-1'>\n<title>Balance Info</title>\n<style>\nh1 {text-align:center;}\np {text-align: center;}\n</style></head>\n");

		pw.print("<body>\n<h1>Current Balance</h1>\n<p>" + transaction_viewBalance(pw, conn, 0, currentName) + "</p>\n");
		pw.print("<button onclick='goBack()'>Back to previous page</button><br/>\n"
				+ "<script>function goBack() {\n window.history.back();\n }\n </script><br/>\n");
		pw.println("</body>\n</html>");
	}

	public String transaction_viewBalance(PrintWriter pw, Connection conn, int transactionRetry, String currentName) {
		try {
			conn.setAutoCommit(false);
			
			ResultSet balanceRemain = conn.createStatement().executeQuery("SELECT Balance FROM Users WHERE Username = '" + currentName + "'");

			if (balanceRemain.next()) {
				int balance = balanceRemain.getInt("Balance");
				balanceRemain.close();
				conn.commit();
				return "User: " + currentName + " Balance: " + balance;
			} else {
				balanceRemain.close();
				conn.commit();
				return "No balance found under user: " + currentName;
			}
		} catch (SQLException e) {
			if (transactionRetry < 3) {
				transactionRetry++;
				pw.println("<p>" + e.toString() + "</p>");
				if (!isDeadLock(e)) {
					try {
						conn.rollback();
					} catch (SQLException e1) {
						pw.println("<p>" + e1.toString() + "</p>");
					}
				}
				return transaction_viewBalance(pw, conn, transactionRetry, currentName);
			} else {
				try {
					conn.commit();
				} catch (SQLException e2) {
				
				}
				
				return "Failed to view balance for user: " + currentName + e.toString();
			}
		} finally {
			checkDanglingTransaction(conn);
		}
	}
}		
		

