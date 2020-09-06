package com.mhz.frontend;

import com.mhz.booking.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class Book extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        int itineraryId = Integer.parseInt(req.getParameter("iid"));
        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        HttpSession currentSession = req.getSession(false);
        if (currentSession == null || currentSession.getAttribute("query") == null
            || ((Query) currentSession.getAttribute("query"))
                .getCurrentUserName().equals("Not logged in")) {
            req.getRequestDispatcher("index.html").include(req, res);
        } else {
            Query currentQuery = (Query) currentSession.getAttribute("query");
            String bookResult = currentQuery.book(itineraryId, 4);
            req.getRequestDispatcher("basic.html").include(req, res);
            pw.println("<!DOCTYPE html>");
            pw.println("<html>");
            pw.print("<head>\n<meta charset='UTF-8'>\n" +
                            "<title>Search Page</title><style>\nh1 {text-align:center;}\n" +
                            "p {text-align: center;}\n</style>\n</head>\n");
            pw.print("<body>\n<h1>Booking Status</h1>\n<p>" + bookResult + "<hr/>");
            pw.print("<a href='search.html'>Looking for more flights? " +
                    "</a><a href='view.html'> View Booked Flights</a><br/>\n");
            pw.println("<button onclick='goBack()'>Continue Booking</button>");
            pw.print("<script>function goBack() {\n window.history.back();\n" +
                    " }\n");
            pw.println("</script><br/>\n</p>\n</body>\n</html>");
        }
    }
}
