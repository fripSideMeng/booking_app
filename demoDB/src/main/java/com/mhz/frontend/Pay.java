package com.mhz.frontend;

import com.mhz.booking.Query;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Pay extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        HttpSession currentSession = req.getSession(false);
        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        if (currentSession == null
                || currentSession.getAttribute("query") == null
                || ((Query)currentSession.getAttribute("query"))
                .getCurrentUserName().equals("Not logged in")) {
            pw.println("Not logged in");
            RequestDispatcher rd = req.getRequestDispatcher("index.html");
            rd.include(req, res);
        } else {
            int reservationId = Integer.parseInt(req.getParameter("rid"));
            Query currentQuery = (Query)currentSession.getAttribute("query");
            String payResult = currentQuery.pay(currentQuery.getCurrentUserName(), reservationId, 4);
            req.getRequestDispatcher("basic.html").include(req, res);
            pw.println("<!DOCTYPE html>");
            pw.println("<html>");
            pw.print("<head>\n<meta charset='UTF-8'>\n" +
                    "<title>Search Page</title><style>\nh1 {text-align:center;}\n" +
                    "p {text-align: center;}\n</style>\n</head>\n");
            pw.print("<body>\n<h1>Booking Status</h1>\n<p>" + payResult + "<hr/>");
            pw.print("<a href='search.html'>Looking for more flights? " +
                    "</a><a href='view.html'> View Booked Flights</a><br/>\n");
            pw.println("<button onclick='goBack()'>Pay for other flights</button>");
            pw.print("<script>function goBack() {\n window.history.back();\n" +
                    " }\n");
            pw.println("</script><br/>\n</p>\n</body>\n</html>");
        }
    }
}
