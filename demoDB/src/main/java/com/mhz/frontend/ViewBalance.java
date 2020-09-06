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
public class ViewBalance extends HttpServlet {
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        HttpSession currentSession = req.getSession(false);
        if (currentSession == null
                || currentSession.getAttribute("query") == null
                || ((Query)currentSession.getAttribute("query"))
                .getCurrentUserName().equals("Not logged in")) {
            RequestDispatcher rd = req.getRequestDispatcher("index.html");
            rd.include(req, res);
        } else {
            Query currentQuery = (Query)currentSession.getAttribute("query");
            res.setContentType("text/html");
            PrintWriter pw = res.getWriter();
            String currentBalance = currentQuery.view_balance(currentQuery.getCurrentUserName(), 4);
            req.getRequestDispatcher("basic.html").include(req, res);
            pw.println("<!DOCTYPE html>");
            pw.println("<html>");
            pw.print("<head>\n<meta charset='UTF-8'>\n" +
                    "<title>Search Page</title><style>\nh1 {text-align:center;}\n" +
                    "p {text-align: center;}\n</style>\n</head>\n");
            pw.print("<body>\n<h1>Booking Status</h1>\n<p>" + currentBalance + "<hr/>\n");
            pw.println("</p>\n</body>\n</html>");
        }
    }
}
