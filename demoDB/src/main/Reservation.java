import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;
public class Reservation implements Filter {
	public String userName;
	public int fid1;
	public int fid2 = -1;
	public int day;
	public int price;
	public int paidOrNot;
	private Context ctx;
	private DataSource ds;


	public void init(FilterConfig arg0) throws ServletException {
		try {
			ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/FlightDB");
		} catch (Exception e) {
		
		}
	}	
			

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = HttpServletRequest.class.cast(request);
		HttpServletResponse res = HttpServletResponse.class.cast(response);

		Connection conn = null;
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		try {
			conn = ds.getConnection();
			conn.setAutoCommit(true);
			conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
		} catch (SQLException e) {
			pw.println("<p>" + e.toString() + "</p>");
			req.getRequestDispatcher("search.html").include(req, res);
			return;
		}
		
		HttpSession session = req.getSession(false);
		if (session == null || session.getAttribute("conn") == null) {
			session = req.getSession();
			session.setAttribute("conn", conn);	
		} 
		Reservation r = new Reservation();
		session.setAttribute("reservation", r);	
		chain.doFilter(req, res);
	}
	
	public Reservation getNewReservation() {
		return new Reservation();
	}

	
}
