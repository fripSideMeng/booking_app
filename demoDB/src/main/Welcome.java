import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class Welcome extends HttpServlet {
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();
		req.getRequestDispatcher("link.html").include(req, res);

		HttpSession session = req.getSession(false);
		String userName = (String)session.getAttribute("uname");

		pw.println("Welcome: " + userName);
		
		pw.close();
	}
	

}
