import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
public class Logout extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();
		req.getRequestDispatcher("link_out.html").include(req, res);

		HttpSession session = req.getSession(false);
		if (session.getAttribute("conn") == null) {
			pw.println("Logout with internal error");
			return;
		} else {
			Connection conn = (Connection) session.getAttribute("conn");
			try {			
				conn.close();
			} catch (SQLException e) {
				pw.println("Logout with internal error");
				return;
			}

		}
		session.invalidate();

		pw.println("Logout successfully...");
		pw.close();
	}
	
}
