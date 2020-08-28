import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class ViewCheck implements Filter {
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = HttpServletRequest.class.cast(request);
		HttpServletResponse res = HttpServletResponse.class.cast(response);

		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		HttpSession session = req.getSession();
		if (session.getAttribute("uname") == null) {
			pw.println("Please log in first");
			req.getRequestDispatcher("index.html").include(req, res);
		} else if ((Connection)session.getAttribute("conn") == null) {
			pw.println("Connection refused<br/>");
			req.getRequestDispatcher("welcome").include(req, res);
		} else {
			chain.doFilter(req, res);
		}
		pw.close();
	}
}
