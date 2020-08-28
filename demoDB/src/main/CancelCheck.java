import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
public class CancelCheck implements Filter {
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = HttpServletRequest.class.cast(request);
		HttpServletResponse res = HttpServletResponse.class.cast(response);
		
		res.setContentType("text/html");
		PrintWriter pw = res.getWriter();

		HttpSession session = req.getSession();
		if (session.getAttribute("conn") == null || session.getAttribute("uname") == null) {
			if (session.getAttribute("conn") == null) {
				pw.println("Connection error. Please re-login.");
				req.getRequestDispatcher("index.html").include(req, res);
			} else {
				pw.println("Username error. Please re-login.");
				req.getRequestDispatcher("index.html").include(req, res);
			}
		} else if (req.getParameter("rid1") == null) {
			pw.println("<button onclick='goBack()'>Please re-input</button>\n"
					+ "<script>function goBack() {\n window.history.back();\n }\n </script>\n");
			pw.print("</html>");
		} else {
			chain.doFilter(req, res);
		}
		pw.close();
	}
}
