import java.io.*;
import java.sql.*;
import javax.naming.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class Flight implements Filter {
	public int fid;
	public int dayOfMonth;
	public String carrierId;
	public String flightNum;
	public String originCity;
	public String destCity;
	public int time;
	public int capacity;
	public int price;
	public Connection conn;
	public static final String INFO_SEARCH = "SELECT * FROM Flights WHERE fid = ?";
	public PreparedStatement infoFind;

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		HttpServletRequest req = HttpServletRequest.class.cast(request);
		HttpServletResponse res = HttpServletResponse.class.cast(response);
		
		HttpSession session = req.getSession();
		PrintWriter pw = res.getWriter();
		
		if (session.getAttribute("conn") == null) {
			pw.println("Connection error, retry now");
			req.getRequestDispatcher("index.html").include(req, res);
		}
		Flight f = new Flight();
		f.conn = (Connection) session.getAttribute("conn");
		session.setAttribute("flight_info", f); 
		chain.doFilter(req, res);		
	}

	public Flight getNewFlight() {
		Flight f = new Flight();
		f.conn = this.conn;
		return f;
	}
	
	public String infoSearch(int fid) {
		if (fid == -1) {
			return "";
		}
		try {
			infoFind = conn.prepareStatement(INFO_SEARCH);
			infoFind.setInt(1, fid);
			ResultSet r = infoFind.executeQuery();
			r.next();

			this.fid = fid;
			dayOfMonth = r.getInt("day_of_month");
			carrierId = r.getString("carrier_id");
			flightNum = r.getString("flight_num");
			originCity = r.getString("origin_city");
			destCity = r.getString("dest_city");
			time = r.getInt("actual_time");
			capacity = r.getInt("capacity");
			price = r.getInt("price");

			r.close();
			return this.toString();

		} catch (SQLException e) {
			return "Search failed\n";
		}

	}

	@Override
	public String toString() {
		return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: " + flightNum
				+ " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time + " Capacity: " + capacity
				+ " Price: " + price + "\n";		
	}
}
