import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class BookCheck implements Filter {
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		HttpServletResponse res = HttpServletResponse.class.cast(response);
		HttpServletRequest req = HttpServletRequest.class.cast(request);
		
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();
		HttpSession session = req.getSession(false);
		if(session == null || session.getAttribute("uname") == null) {
			pw.println("Please login first.");
			RequestDispatcher rd = req.getRequestDispatcher("index.html");
			rd.include(req, res);
		} else if (session.getAttribute("recentSearch") == null || castToMap(session).isEmpty() || req.getParameter("itineraryId") == null) {
			if(session.getAttribute("recentSearch") == null) {
				pw.println("Has not done search yet. Please search for flights first.");
			} else if (castToMap(session).isEmpty()) {
				pw.println("No search record found. Please search for flights first.");
			} else {
				pw.println("No itinerary selected. Please search for flights first.");
			}
			req.getRequestDispatcher("search.html").include(req, res);
		} else {
			session.setAttribute("itineraryId", req.getParameter("itineraryId"));
			chain.doFilter(req, res);
		}
		pw.close();
	}

	@SuppressWarnings("unchecked")
	private HashMap<Integer, Reservation> castToMap(HttpSession session) {
		return (HashMap<Integer, Reservation>) session.getAttribute("recentSearch");
	}
}
	
